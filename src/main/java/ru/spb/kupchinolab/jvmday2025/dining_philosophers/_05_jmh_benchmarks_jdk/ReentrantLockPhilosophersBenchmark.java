package ru.spb.kupchinolab.jvmday2025.dining_philosophers._05_jmh_benchmarks_jdk;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._02_reentrant_pivot.ReentrantPhilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ReentrantLockPhilosophersBenchmark {

    private static Consumer<Integer> constructNoopEating(Blackhole blackhole) {
        return blackhole::consume;
    }

    @Benchmark
    public void test_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_noop_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), constructNoopEating(blackhole));
    }

    private void test_philosophers_internal(ThreadFactory factory, Consumer<Integer> eating) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
            reentrantPhilosophers.add(new ReentrantPhilosopher(i, leftChopstick, rightChopstick, barrier, eating));
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            reentrantPhilosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ReentrantLockPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .jvmArgs("--enable-preview")
                .build();
        new Runner(opt).run();
    }
}
