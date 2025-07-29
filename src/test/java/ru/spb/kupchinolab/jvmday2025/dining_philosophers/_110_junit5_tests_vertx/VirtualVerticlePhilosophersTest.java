package ru.spb.kupchinolab.jvmday2025.dining_philosophers._110_junit5_tests_vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot.VirtualVerticlePhilosopher;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@ExtendWith(VertxExtension.class)
public class VirtualVerticlePhilosophersTest {

    @RepeatedTest(25) // тут меньше итераций так как сильно дольше работает
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void test_virtual_verticle_philosophers(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        test_virtual_philosophers_internal(vertx, testContext);
    }

    private void test_virtual_philosophers_internal(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        Checkpoint undeploy = testContext.checkpoint(PHILOSOPHERS_COUNT_BASE);
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.deploymentIDs().forEach(deploymentId -> vertx.undeploy(deploymentId).onComplete(_ -> undeploy.flag()));
        });
        DeploymentOptions deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(new VirtualVerticlePhilosopher(i, _ -> {/*NO_OP*/}), deploymentOptions)
                    .onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start  eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
    }

}
