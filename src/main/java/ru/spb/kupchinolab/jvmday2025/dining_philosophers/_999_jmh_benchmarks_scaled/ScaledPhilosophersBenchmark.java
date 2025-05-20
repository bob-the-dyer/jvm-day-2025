package ru.spb.kupchinolab.jvmday2025.dining_philosophers._999_jmh_benchmarks_scaled;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._002_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._003_synchronized_pivot.SynchronizedPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_vertx_pivot.VerticlePhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_vertx_pivot.VirtualVerticlePhilosopher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.vertx.core.ThreadingModel.VIRTUAL_THREAD;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ScaledPhilosophersBenchmark {

    private static Consumer<Integer> constructNoopEating(Blackhole blackhole) {
        return blackhole::consume;
    }

    @Benchmark
    public void _010_test_0010K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 10, 1);
    }

    @Benchmark
    public void _020_test_0010K_0010K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 10, 1);
    }

    @Benchmark
    public void _030_test_0010K_0010K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 1);
    }

    @Benchmark
    public void _040_test_0010K_0010K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 1);
    }

    @Benchmark
    public void _050_test_0100K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 100, 1);
    }

    @Benchmark
    public void _060_test_0100K_0010K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 100, 1);
    }

    @Benchmark
    public void _070_test_0100K_0010K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 1);
    }

    @Benchmark
    public void _080_test_0100K_0010K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 1);
    }

    @Benchmark
    public void _090_test_0010K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 10, 10);
    }

    @Benchmark
    public void _100_test_0010K_0100K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 10, 10);
    }

    @Benchmark
    public void _110_test_0010K_0100K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 10);
    }

    @Benchmark
    public void _120_test_0010K_0100K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 10);
    }


    @Benchmark
    public void _130_test_0100K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 100, 10);
    }

    @Benchmark
    public void _140_test_0100K_0100K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 100, 10);
    }

    @Benchmark
    public void _150_test_0100K_0100K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 10);
    }

    @Benchmark
    public void _160_test_0100K_0100K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 10);
    }

    @Benchmark
    public void _170_test_1000K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 1000, 1);
    }

    @Benchmark
    public void _180_test_1000K_0010K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 1000, 1);
    }

    @Benchmark
    public void _190_test_1000K_0010K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 1);
    }

    @Benchmark
    public void _200_test_1000K_0010K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 1);
    }

    @Benchmark
    public void _210_test_0010K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 10, 100);
    }

    @Benchmark
    public void _220_test_0010K_1000K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 10, 100);
    }

    @Benchmark
    public void _230_test_0010K_1000K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 100);
    }

    @Benchmark
    public void _240_test_0010K_1000K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 100);
    }

    @Benchmark
    public void _250_test_1000K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructNoopEating(blackhole), 1000, 100);
    }

    @Benchmark
    public void _260_test_1000K_1000K_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructNoopEating(blackhole), 1000, 100);
    }

    @Benchmark
    public void _270_test_1000K_1000K_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 100);
    }

    @Benchmark
    public void _280_test_1000K_1000K_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 100);
    }

    private void test_classical_philosophers_internal(BiFunction<List<Object>, Integer, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating, int philosophersMultiplier, int eatingMultiplier) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<Callable<Integer>> philosophers = new ArrayList<>();
        int philosophersCount = PHILOSOPHERS_COUNT_BASE * philosophersMultiplier;
        CyclicBarrier barrier = new CyclicBarrier(1 + philosophersCount);
        for (int i = 0; i < philosophersCount; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < philosophersCount; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : philosophersCount - 1);
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating), eatingMultiplier));
        }
        try (StructuredTaskScope.ShutdownOnSuccess<Integer> scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    private void test_verticle_philosophers_internal(BiFunction<List<Object>, Integer, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions, int philosophersMultiplier, int eatingMultiplier) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        int philosophersCount = PHILOSOPHERS_COUNT_BASE * philosophersMultiplier;
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        for (int i = 0; i < philosophersCount; i++) {
            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating), eatingMultiplier), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.close().onComplete(_ -> finishEatingLatch.countDown()
            );
        });
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ScaledPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("--enable-preview")
                .build();
        new Runner(opt).run();
    }

}
