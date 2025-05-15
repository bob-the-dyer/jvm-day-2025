package ru.spb.kupchinolab.jvmday2025.dining_philosophers._08_jmh_benchmarks_reading;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._02_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._03_synchronized_pivot.SynchronizedPhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ReadingPhilosophersBenchmark {

    private static final int TEST_PHILOSOPHERS_COUNT = 1_000;

    static List<Chopstick> chopsticks = new ArrayList<>();
    static List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
    static List<SynchronizedPhilosopher> synchronizedPhilosophers = new ArrayList<>();
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
                try (InputStream in = Path.of("64KB_file.txt").toFile().toURI().toURL().openStream()) {//read sequentially from SSD with speed of 1MB in 1M nanosec
                    byte[] bytes = in.readAllBytes();
                    blackhole.consume(bytes.length);
                    blackhole.consume(stats);
                } catch (IOException e) {
                    Thread.currentThread().interrupt();
                }
            };
            SynchronizedPhilosopher.eating = ReentrantPhilosopher.eating;
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ReadingPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .jvmArgs("--enable-preview")
                .build();

        new Runner(opt).run();
    }
}
