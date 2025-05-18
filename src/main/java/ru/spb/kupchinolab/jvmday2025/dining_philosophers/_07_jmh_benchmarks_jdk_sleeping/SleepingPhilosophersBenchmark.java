package ru.spb.kupchinolab.jvmday2025.dining_philosophers._07_jmh_benchmarks_jdk_sleeping;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._02_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._03_synchronized_pivot.SynchronizedPhilosopher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SleepingPhilosophersBenchmark {

    @Benchmark
    public void test_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, blackhole);
    }

    @Benchmark
    public void test_reentrant_lock_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, blackhole);
    }

    @Benchmark
    public void test_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, blackhole);
    }

    @Benchmark
    public void test_synchronized_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, blackhole);
    }

    private void test_philosophers_internal(ThreadFactory factory, Function<List<Object>, ? extends Callable<Integer>> philosopherSupplier, Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
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
            philosophers.add(
                    philosopherSupplier.apply(
                            List.of(i, leftChopstick, rightChopstick, barrier, (Consumer<Integer>) (stats) -> {
                                try {
                                    Thread.sleep(Duration.ofNanos(64_000)); //read sequentially from SSD with speed of 1MB in 1M nanosec
                                    blackhole.consume(stats);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            })
                    )
            );
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SleepingPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(10)
                .jvmArgs("--enable-preview")
                .build();

        new Runner(opt).run();
    }
}
