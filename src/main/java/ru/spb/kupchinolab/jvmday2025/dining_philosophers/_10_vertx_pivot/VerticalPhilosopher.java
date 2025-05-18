package ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

public class VerticalPhilosopher extends VerticleBase {
    private final int firstChopstick;
    private final int secondChopstick;
    private final int order;
    private int stats;
    private MessageConsumer<Object> loopMyselfConsumer;

    public VerticalPhilosopher(int order) {
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
        stats++;
        if (stats >= MAX_EAT_ATTEMPTS) {
            vertx.eventBus().send(
                    "max_eat_attempts_has_reached",
                    format("%s #%s has reached %s attempts to eat!", VerticalPhilosopher.class.getSimpleName(), order, stats)
            );
        } else {
            loopMyselfOnce();
        }
    }

    private void loopMyselfOnce() {
        vertx.eventBus().send("loop_myself_" + order, "loop!");
    }
}
