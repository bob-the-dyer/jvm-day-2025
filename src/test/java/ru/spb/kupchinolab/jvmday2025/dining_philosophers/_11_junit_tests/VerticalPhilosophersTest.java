package ru.spb.kupchinolab.jvmday2025.dining_philosophers._11_junit_tests;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot.VerticalPhilosopher;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@ExtendWith(VertxExtension.class)
public class VerticalPhilosophersTest {

    @RepeatedTest(100)
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void test_vertical_philosophers_as_is(Vertx vertx, VertxTestContext testContext) {
        test_philosophers_internal(vertx, testContext);
    }

    private void test_philosophers_internal(Vertx vertx, VertxTestContext testContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(PHILOSOPHERS_COUNT);
        vertx.deployVerticle(VerticalPhilosopher.class, deploymentOptions).onComplete(_ -> {
            System.out.println("all ph deployed at " + Instant.now());
            System.out.println("start  eating at " + Instant.now());
            vertx.eventBus().publish("start_barrier", "Go-go-go!");
            vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
                System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
                testContext.completeNow();
            });
        });
    }

}
