package ru.spb.kupchinolab.jvmday2025.dining_philosophers._011_junit5_tests_vertx;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_vertx_pivot.VerticlePhilosopher;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@ExtendWith(VertxExtension.class)
public class VerticlePhilosophersTest {

    @RepeatedTest(25) // тут меньше итераций так как сильно дольше работает
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void test_verticle_philosophers_as_is(Vertx vertx, VertxTestContext testContext) {
        test_philosophers_internal(vertx, testContext);
    }

    private void test_philosophers_internal(Vertx vertx, VertxTestContext testContext) {
        Checkpoint verticlesDeployed = testContext.checkpoint(PHILOSOPHERS_COUNT_BASE);
        Checkpoint maxEatAttemptsHasReached = testContext.laxCheckpoint(1);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(new VerticlePhilosopher(i, _ -> {/*NO_OP*/}))
                    .onComplete(_ -> verticlesDeployed.flag());
        }
        System.out.println("all verticles deployed at " + Instant.now());
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.close().onComplete(_ -> maxEatAttemptsHasReached.flag());
        });
        System.out.println("start  eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
    }

}
