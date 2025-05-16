package ru.spb.kupchinolab.jvmday2025.dining_philosophers._09_jmh_benchmarks_jdk_looping;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._02_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._03_synchronized_pivot.SynchronizedPhilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LoopingPhilosophersBenchmark {

    static List<Chopstick> chopsticks = new ArrayList<>();
    static List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
    static List<SynchronizedPhilosopher> synchronizedPhilosophers = new ArrayList<>();
    static CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT);

    static {
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
            reentrantPhilosophers.add(new ReentrantPhilosopher(i, leftChopstick, rightChopstick, barrier));
            synchronizedPhilosophers.add(new SynchronizedPhilosopher(i, leftChopstick, rightChopstick, barrier));
        }
    }

    @TearDown(Level.Invocation)
    public void resetBarrierAndPhilosophers() {
        barrier.reset();
        reentrantPhilosophers.forEach(ReentrantPhilosopher::resetStats);
        synchronizedPhilosophers.forEach(SynchronizedPhilosopher::resetStats);
    }

    @Benchmark
    public void test_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), reentrantPhilosophers, blackhole);
    }

    @Benchmark
    public void test_reentrant_lock_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), reentrantPhilosophers, blackhole);
    }

    @Benchmark
    public void test_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), synchronizedPhilosophers, blackhole);
    }

    @Benchmark
    public void test_synchronized_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), synchronizedPhilosophers, blackhole);
    }

    private void test_philosophers_internal(ThreadFactory factory, List<? extends Callable<Integer>> philosophers, Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            ReentrantPhilosopher.eating = (stats) -> {
                long startTimeInNanos = System.nanoTime();
                long currentTimeInNanos;
                do {
                    currentTimeInNanos = System.nanoTime();
                } while (currentTimeInNanos < startTimeInNanos + 524_288 / 2); //read sequentially from SSD with speed of 1MB in 1M nanosec, 256KB
                blackhole.consume(currentTimeInNanos);
                blackhole.consume(stats);
            };
            SynchronizedPhilosopher.eating = ReentrantPhilosopher.eating;
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LoopingPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .jvmArgs("--enable-preview")
                .build();

        new Runner(opt).run();
    }
}
