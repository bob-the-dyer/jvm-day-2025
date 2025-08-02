package ru.spb.kupchinolab.jvmday2025.dining_philosophers._140_jmh_benchmarks_vertx_active_waiting;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot.VirtualVerticlePhilosopher;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.vertx.core.ThreadingModel.VIRTUAL_THREAD;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ActiveWaitingVirtualVerticlePhilosophersBenchmark {

    private static Consumer<Integer> constructActiveWaitingEating(Blackhole blackhole) {
        return stats -> {
            long startTimeInNanos = System.nanoTime();
            long currentTimeInNanos;
            do {
                currentTimeInNanos = System.nanoTime();
            } while (currentTimeInNanos < startTimeInNanos + 16_000); //emulating - read sequentially from SSD with speed of 1MB in 1M nanosec
            blackhole.consume(currentTimeInNanos);
            blackhole.consume(stats);
        };
    }

    @Benchmark
    public void test_virtual_active_waiting_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructActiveWaitingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
    }

    private void test_verticle_philosophers_internal(Function<List<Object>, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch allVerticlesUnDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating)), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.deploymentIDs().forEach(deploymentId -> vertx.undeploy(deploymentId).onComplete(_ -> allVerticlesUnDeployedLatch.countDown()));
            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
        });
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
        allVerticlesUnDeployedLatch.await();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ActiveWaitingVirtualVerticlePhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(7)
                .jvmArgs("--enable-preview", "-Xmx4096m")
                .build();
        new Runner(opt).run();
    }
}
