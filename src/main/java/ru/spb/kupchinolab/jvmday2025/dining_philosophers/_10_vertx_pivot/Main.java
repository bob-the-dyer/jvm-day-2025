package ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.time.Instant;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.eventBus().publish("stop_barrier", "No-no-no!");
            vertx.close();
        });
        DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(PHILOSOPHERS_COUNT);
        vertx.deployVerticle(VerticalPhilosopher.class, deploymentOptions, _ -> {
            vertx.eventBus().publish("start_barrier", "Go-go-go!");
            System.out.println("start  eating at " + Instant.now());
        });
    }

}
