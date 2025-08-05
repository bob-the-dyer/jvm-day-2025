package ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.SharedData;

import java.util.List;
import java.util.function.Consumer;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS_BASE;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class FatVirtualVerticlePhilosopher extends VerticleBase {
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> loopMyselfConsumer;
    private final Consumer<Integer> eating;
    private final int eatingMultiplier;
    private boolean interrupted = false;

    public FatVirtualVerticlePhilosopher(int order, Consumer<Integer> eating) {
        this(order, eating, 1);
    }

    public FatVirtualVerticlePhilosopher(int order, Consumer<Integer> eating, int eatingMultiplier) {
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
        vertx.eventBus().consumer("start_barrier", _ -> eat());
        vertx.eventBus().consumer("max_eat_attempts_has_reached", _ -> interrupted = true);
        return succeededFuture();
    }

    public Future<?> stop() {
        return succeededFuture();
    }

    private void eat() {
        while (interrupted != true) {
            SharedData sharedData = vertx.sharedData();
            sharedData.withLocalLock("chopstick_" + firstChopstick, () -> {
//                System.out.println("chopstick_" + firstChopstick + " locked");
                return sharedData.withLocalLock("chopstick_" + secondChopstick, () -> {
//                    System.out.println("chopstick_" + firstChopstick + " locked_" + "_chopstick_" + secondChopstick + " locked");
                    updateStats();
                    return succeededFuture();
                });
            });
        }
    }

    private void updateStats() {
        eating.accept(stats++);
        if (stats >= MAX_EAT_ATTEMPTS_BASE * eatingMultiplier) {
            interrupted = true;
            vertx.eventBus().send(
                    "max_eat_attempts_has_reached",
                    format("%s #%s has reached %s attempts to eat!", FatVirtualVerticlePhilosopher.class.getSimpleName(), order, stats)
            );
        }
    }

    public static FatVirtualVerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer) {
        return new FatVirtualVerticlePhilosopher(order, statsConsumer);
    }

    public static FatVirtualVerticlePhilosopher from(Integer order, Consumer<Integer> statsConsumer, int eatingMultiplier) {
        return new FatVirtualVerticlePhilosopher(order, statsConsumer, eatingMultiplier);
    }

    public static FatVirtualVerticlePhilosopher from(List<Object> from) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1)
        );
    }

    public static FatVirtualVerticlePhilosopher from(List<Object> from, int eatingMultiplier) {
        return from(
                (Integer) from.get(0),
                (Consumer<Integer>) from.get(1),
                eatingMultiplier
        );
    }

}
