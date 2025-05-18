package ru.spb.kupchinolab.jvmday2025.dining_philosophers._12_jmh_benchmarks_vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot.VirtualVerticalPhilosopher;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class VirtualVerticalPhilosophersBenchmark {

    @Benchmark
    public void test_virtual_vertical_philosophers(Blackhole blackhole) throws InterruptedException {
        test_virtual_vertical_philosophers_internal(blackhole);
    }

    private void test_virtual_vertical_philosophers_internal(Blackhole blackhole) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticalsDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            vertx.deployVerticle(new VirtualVerticalPhilosopher(i, blackhole::consume), deploymentOptions)
                    .onComplete(_ -> allVerticalsDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            finishEatingLatch.countDown();
            vertx.close();
        });
        allVerticalsDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VirtualVerticalPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(10)
                .build();
        new Runner(opt).run();
    }
}
