package ru.spb.kupchinolab.jvmday2025.dining_philosophers._11_junit5_tests_vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot.VirtualVerticalPhilosopher;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@ExtendWith(VertxExtension.class)
public class VirtualVerticalPhilosophersTest {

    @RepeatedTest(100)
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void test_vertical_philosophers_as_is(Vertx vertx, VertxTestContext testContext) {
        test_philosophers_internal(vertx, testContext);
    }

    private void test_philosophers_internal(Vertx vertx, VertxTestContext testContext) {
        Checkpoint verticlesDeployed = testContext.checkpoint(PHILOSOPHERS_COUNT);
        Checkpoint maxEatAttemptsHasReached = testContext.laxCheckpoint(1);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            vertx.deployVerticle(new VirtualVerticalPhilosopher(i), deploymentOptions).onComplete(_ -> verticlesDeployed.flag());
        }
        System.out.println("all verticles deployed at " + Instant.now());
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            maxEatAttemptsHasReached.flag();
        });
        System.out.println("start  eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
    }

}
