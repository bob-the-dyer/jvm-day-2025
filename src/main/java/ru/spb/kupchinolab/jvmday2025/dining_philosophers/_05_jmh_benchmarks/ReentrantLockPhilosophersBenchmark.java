package ru.spb.kupchinolab.jvmday2025.dining_philosophers._05_jmh_benchmarks;

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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ReentrantLockPhilosophersBenchmark {

    private static final int TEST_PHILOSOPHERS_COUNT = 1_000;

    static List<Chopstick> chopsticks = new ArrayList<>();
    static List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
    static CyclicBarrier barrier = new CyclicBarrier(1 + TEST_PHILOSOPHERS_COUNT);

    static {
        for (int i = 0; i < TEST_PHILOSOPHERS_COUNT; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < TEST_PHILOSOPHERS_COUNT; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : TEST_PHILOSOPHERS_COUNT - 1);
            reentrantPhilosophers.add(new ReentrantPhilosopher(i, leftChopstick, rightChopstick, barrier));
        }
    }

    @TearDown(Level.Invocation)
    public void resetBarrierAndPhilosophers() {
        barrier.reset();
        reentrantPhilosophers.forEach(ReentrantPhilosopher::resetStats);
    }

    @Benchmark
    public void test_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_reentrant_lock_philosophers_internal(Thread.ofVirtual().factory(), blackhole);
    }

    @Benchmark
    public void test_reentrant_lock_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_reentrant_lock_philosophers_internal(Thread.ofPlatform().factory(), blackhole);
    }

    private void test_reentrant_lock_philosophers_internal(ThreadFactory factory, Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            reentrantPhilosophers.forEach(scope::fork);
            ReentrantPhilosopher.eating = (stats) -> {/*NO_OP*/ blackhole.consume(stats);};
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
