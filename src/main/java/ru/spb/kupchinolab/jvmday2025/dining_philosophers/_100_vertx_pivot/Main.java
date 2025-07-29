package ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot;

import io.vertx.core.Vertx;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
        });
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(new VerticlePhilosopher(i, _ -> {/*NO_OP*/}))
                    .onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        allVerticlesDeployedLatch.await();
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        System.out.println("start  eating at " + Instant.now());
        finishEatingLatch.await();
    }

}
