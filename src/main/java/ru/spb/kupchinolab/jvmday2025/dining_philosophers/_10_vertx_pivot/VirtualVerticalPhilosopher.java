package ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

public class VirtualVerticalPhilosopher extends VerticleBase {
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> eatingConsumer;

    public VirtualVerticalPhilosopher(int order) {
        this.order = order;
        this.stats = 0;
        if (order == 0) {
            firstChopstick = order;
            secondChopstick = (PHILOSOPHERS_COUNT - 1);
        } else {
            firstChopstick = (order - 1);
            secondChopstick = order;
        }
    }

    @Override
    public Future<?> start() {
        eatingConsumer = vertx.eventBus().consumer("loop_myself_" + order, (_) -> eat());
        vertx.eventBus().consumer("start_barrier", _ -> {
            stats = 0;
            eatingConsumer.resume();
            vertx.eventBus().send("loop_myself_" + order, "loop!");
        });
        vertx.eventBus().consumer("reset_" + order, msg -> {
            stats = 0;
            eatingConsumer.pause();
            msg.reply("yes, sir!");
        });
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
            e.printStackTrace(); // в тестах не ловится, оставлю для формализма
        } finally {
            if (secondLock != null) secondLock.release();
            if (firstLock != null) firstLock.release();
        }
    }

    private void updateStats() {
        stats++;
        if (stats >= MAX_EAT_ATTEMPTS) {
            vertx.eventBus().send(
                    "max_eat_attempts_has_reached",
                    format("%s #%s has reached %s attempts to eat!", VirtualVerticalPhilosopher.class.getSimpleName(), order, stats)
            );
            eatingConsumer.pause();
        } else {
            vertx.eventBus().send("loop_myself_" + order, "loop!");
        }
    }
}
