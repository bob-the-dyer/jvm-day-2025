package ru.spb.kupchinolab.jvmday2025.dining_philosophers._99_jmh_benchmarks_united;

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
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._02_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._03_synchronized_pivot.SynchronizedPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot.VerticlePhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._10_vertx_pivot.VirtualVerticlePhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.vertx.core.ThreadingModel.VIRTUAL_THREAD;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class UnitedPhilosophersBenchmark {

    private static Consumer<Integer> constructNoopEating(Blackhole blackhole) {
        return blackhole::consume;
    }

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

    private static Consumer<Integer> constructBlockingReadingEating(Blackhole blackhole) {
        return stats -> {
            try (InputStream in = Path.of("16KB_file.txt").toFile().toURI().toURL().openStream()) { //read sequentially from SSD with speed of 1MB in 1M nanosec
                byte[] bytes = in.readAllBytes();
                blackhole.consume(bytes.length);
                blackhole.consume(stats);
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

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
    public void test_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_noop_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_noop_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_sleeping_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_sleeping_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_sleeping_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_sleeping_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructBlockingReadingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_blocking_reading_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructBlockingReadingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_blocking_reading_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructBlockingReadingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_blocking_reading_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructBlockingReadingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_active_waiting_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_active_waiting_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_active_waiting_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_active_waiting_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_verticle_noop_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions());
    }

    @Benchmark
    public void test_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
    }

    @Benchmark
    public void test_virtual_sleeping_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructSleepingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
    }

    @Benchmark
    public void test_virtual_blocking_reading_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructBlockingReadingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
    }

    @Benchmark
    public void test_virtual_active_waiting_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructActiveWaitingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
    }

    private void test_classical_philosophers_internal(ThreadFactory factory, Function<List<Object>, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<Callable<Integer>> philosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating)));
        }
        try (StructuredTaskScope.ShutdownOnSuccess<Integer> scope = new StructuredTaskScope.ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    private void test_verticle_philosophers_internal(Function<List<Object>, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating)), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            finishEatingLatch.countDown();
            vertx.close();
        });
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UnitedPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .jvmArgs("--enable-preview")
                .build();
        new Runner(opt).run();
    }

}
