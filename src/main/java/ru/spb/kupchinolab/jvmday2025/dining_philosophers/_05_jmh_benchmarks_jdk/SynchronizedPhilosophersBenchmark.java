package ru.spb.kupchinolab.jvmday2025.dining_philosophers._05_jmh_benchmarks_jdk;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._03_synchronized_pivot.SynchronizedPhilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SynchronizedPhilosophersBenchmark {

    @Benchmark
    public void test_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_synchronized_philosophers_internal(Thread.ofVirtual().factory(), blackhole);
    }

    @Benchmark
    public void test_synchronized_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_synchronized_philosophers_internal(Thread.ofPlatform().factory(), blackhole);
    }

    private void test_synchronized_philosophers_internal(ThreadFactory factory, Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<SynchronizedPhilosopher> synchronizedPhilosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT);
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
            synchronizedPhilosophers.add(new SynchronizedPhilosopher(i, leftChopstick, rightChopstick, barrier, blackhole::consume));
        }

        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            synchronizedPhilosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SynchronizedPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(10)
                .jvmArgs("--enable-preview")
                .build();

        new Runner(opt).run();
    }
}
