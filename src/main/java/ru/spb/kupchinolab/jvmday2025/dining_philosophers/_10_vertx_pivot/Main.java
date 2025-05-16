package ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot;

import io.vertx.core.Vertx;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.eventBus().publish("stop_barrier", "No-no-no!");
            vertx.close();
        });
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            vertx.deployVerticle(new VerticalPhilosopher(i));
        }
        Thread.sleep(Duration.of(1, ChronoUnit.SECONDS));
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        System.out.println("start  eating at " + Instant.now());
    }

}
