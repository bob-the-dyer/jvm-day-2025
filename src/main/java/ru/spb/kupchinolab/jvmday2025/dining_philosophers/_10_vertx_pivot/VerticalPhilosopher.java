package ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

public class VerticalPhilosopher extends AbstractVerticle {
    private static int globalOrder = 0;
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> eatingConsumer;

    public VerticalPhilosopher() {
        this.order = globalOrder;
        this.stats = 0;
        globalOrder++;
        if (order == 0) {
            firstChopstick = order;
            secondChopstick = (PHILOSOPHERS_COUNT - 1);
        } else {
            firstChopstick = (order - 1);
            secondChopstick = order;
        }
    }

    @Override
    public void start() {
        eatingConsumer = vertx.eventBus().consumer("loop_myself_" + order, (_) -> eat());
        vertx.eventBus().consumer("start_barrier", _ -> {
            eatingConsumer.resume();
            vertx.eventBus().send("loop_myself_" + order, "loop!");
        });
    }

    private void eat() {
        SharedData sharedData = vertx.sharedData();
        sharedData.getLock("chopstick_" + firstChopstick, firstLock -> {
            Lock chopstick_1 = firstLock.result();
            sharedData.getLock("chopstick_" + secondChopstick, secondLock -> {
                Lock chopstick_2 = secondLock.result();
                updateStats();
                chopstick_2.release();
                chopstick_1.release();
                vertx.eventBus().send("loop_myself_" + order, "loop!");
            });
        });
    }

    private void updateStats() {
        stats++;
        if (stats >= MAX_EAT_ATTEMPTS) {
            vertx.eventBus().send("max_eat_attempts_has_reached", format("%s #%s has reached %s attempts to eat!", VerticalPhilosopher.class.getSimpleName(), order, stats));
            globalOrder = 0;
            eatingConsumer.pause();
        }
    }
}
