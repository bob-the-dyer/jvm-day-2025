package ru.spb.kupchinolab.jvmday2025.dining_philosophers._015_jmh_benchmarks_vertx_sleeping;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_vertx_pivot.VerticlePhilosopher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SleepingVerticlePhilosophersBenchmark {

    private static Consumer<Integer> constructSleepingEating(Blackhole blackhole) {
        return stats -> {
            try {
                Thread.sleep(Duration.ofNanos(16_000)); //emulating - read sequentially from SSD with speed of 1MB in 1M nanosec
                blackhole.consume(stats);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Benchmark
    public void test_verticle_sleeping_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructSleepingEating(blackhole), new DeploymentOptions());
    }

    private void test_verticle_philosophers_internal(Function<List<Object>, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating)), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
        });
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SleepingVerticlePhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(7)
                .build();
        new Runner(opt).run();
    }
}
