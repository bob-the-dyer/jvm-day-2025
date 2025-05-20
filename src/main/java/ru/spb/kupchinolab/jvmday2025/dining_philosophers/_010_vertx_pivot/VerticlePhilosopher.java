package ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_vertx_pivot;

import io.vertx.core.AsyncResult;
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

public class VerticlePhilosopher extends VerticleBase {
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> loopMyselfConsumer;
    private final Consumer<Integer> eating;
    private final int eatingMultiplier;

    public VerticlePhilosopher(int order, Consumer<Integer> eating) {
        this(order, eating, 1);
    }

    public VerticlePhilosopher(int order, Consumer<Integer> eating, int eatingMultiplier) {
        this.order = order;
        this.eating = eating;
        this.eatingMultiplier = eatingMultiplier;
        this.stats = 0;
        if (order == 0) {
            firstChopstick = order;
            secondChopstick = (PHILOSOPHERS_COUNT_BASE - 1);
        } else {
            firstChopstick = (order - 1);
            secondChopstick = order;
        }
    }

    @Override
    public Future<?> start() {
        loopMyselfConsumer = vertx.eventBus().consumer("loop_myself_" + order, (_) -> eat());
        vertx.eventBus().consumer("start_barrier", _ -> loopMyselfOnce());
        return succeededFuture();
    }

    public Future<?> stop() {
        loopMyselfConsumer.unregister();
        return succeededFuture();
    }

    private void eat() {
        SharedData sharedData = vertx.sharedData();
        Future<Lock> firstLock = sharedData.getLock("chopstick_" + firstChopstick);
        firstLock.onComplete((AsyncResult<Lock> ar1) -> {
            if (ar1.succeeded()) {
                Lock chopstick_1 = ar1.result();
                Future<Lock> secondLock = sharedData.getLock("chopstick_" + secondChopstick);
                secondLock.onComplete((AsyncResult<Lock> ar2) -> {
                    if (ar2.succeeded()) {
                        Lock chopstick_2 = secondLock.result();
                        updateStats();
                        chopstick_2.release();
                    } else {
//                        ТУТ может быть неудача, если взять лок не получилось, например в кластерном окружении из-за сети или на остановке вертекса
//                        System.out.println(format("chopstick_2 lock for #%s failed: %s", order, ar2.cause().getLocalizedMessage()));
//                        loopMyselfOnce();
                    }
                    chopstick_1.release();
                });
            } else {
//                ТУТ может быть неудача, если взять лок не получилось, например в кластерном окружении из-за сети или на остановке вертекса
//                System.out.println(format("chopstick_1 lock for #%s failed: %s", order, ar1.cause().getLocalizedMessage()));
//                loopMyselfOnce();
            }
        });
    }

    private void updateStats() {
        eating.accept(stats++);
        if (stats >= MAX_EAT_ATTEMPTS_BASE * eatingMultiplier) {
            vertx.eventBus().send(
                    "max_eat_attempts_has_reached",
                    format("%s #%s has reached %s attempts to eat!", VerticlePhilosopher.class.getSimpleName(), order, stats)
            );
        } else {
            loopMyselfOnce();
        }
    }

    private void loopMyselfOnce() {
        vertx.eventBus().send("loop_myself_" + order, "loop!");
    }

    public static VerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer) {
        return new VerticlePhilosopher(order, statsConsumer);
    }

    public static VerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer, int eatingMultiplier) {
        return new VerticlePhilosopher(order, statsConsumer, eatingMultiplier);
    }

    public static VerticlePhilosopher from(List<Object> from) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1)
        );
    }

    public static VerticlePhilosopher from(List<Object> from, int eatingMultiplier) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1),
                eatingMultiplier
        );
    }

}
