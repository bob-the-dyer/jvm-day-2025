package ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import java.util.List;
import java.util.function.Consumer;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS_BASE;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class VirtualVerticlePhilosopher extends VerticleBase {
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> loopMyselfConsumer;
    private final Consumer<Integer> eating;
    private final int eatingMultiplier;

    public VirtualVerticlePhilosopher(int order, Consumer<Integer> eating) {
        this(order, eating, 1);
    }

    public VirtualVerticlePhilosopher(int order, Consumer<Integer> eating, int eatingMultiplier) {
        this.order = order;
        this.eating = eating;
        this.eatingMultiplier = eatingMultiplier;
        this.stats = 0;
        if (order == 0) {
            firstChopstick = (PHILOSOPHERS_COUNT_BASE - 1);
            secondChopstick = order;
        } else {
            firstChopstick = order;
            secondChopstick = (order - 1);
        }
    }

    @Override
    public Future<?> start() {
        loopMyselfConsumer = vertx.eventBus().consumer("loop_myself_" + order, (_) -> eat());
        vertx.eventBus().consumer("start_barrier", _ -> loopMyselfOnce());
        return succeededFuture();
    }

    public Future<?> stop() {
        loopMyselfConsumer.unregister().await();
        return succeededFuture();
    }

    private void eat() {
        SharedData sharedData = vertx.sharedData();
        Lock firstLock = null;
        Lock secondLock = null;
        try {
            firstLock = sharedData.getLock("chopstick_" + firstChopstick).await();
            secondLock = sharedData.getLock("chopstick_" + secondChopstick).await();
            updateStats();
        } catch (Exception e) {
//            ТУТ может лететь исключение, если взять лок не получилось, например в кластерном окружении из-за сети или на остановке vert.x
            e.printStackTrace();
            System.err.println(format("chopstick lock for #%s failed: %s", order, e.getLocalizedMessage()));
            loopMyselfOnce();
        } finally {
            if (secondLock != null) secondLock.release();
            if (firstLock != null) firstLock.release();
        }
    }

    private void updateStats() {
        eating.accept(stats++);
        if (stats >= MAX_EAT_ATTEMPTS_BASE * eatingMultiplier) {
            vertx.eventBus().send(
                    "max_eat_attempts_has_reached",
                    format("%s #%s has reached %s attempts to eat!", VirtualVerticlePhilosopher.class.getSimpleName(), order, stats)
            );
        } else {
            loopMyselfOnce();
        }
    }

    private void loopMyselfOnce() {
        vertx.eventBus().send("loop_myself_" + order, "loop!");
    }

    public static VirtualVerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer) {
        return new VirtualVerticlePhilosopher(order, statsConsumer);
    }

    public static VirtualVerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer, int eatingMultiplier) {
        return new VirtualVerticlePhilosopher(order, statsConsumer, eatingMultiplier);
    }

    public static VirtualVerticlePhilosopher from(List<Object> from) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1)
        );
    }

    public static VirtualVerticlePhilosopher from(List<Object> from, int eatingMultiplier) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1),
                eatingMultiplier
        );
    }

}
