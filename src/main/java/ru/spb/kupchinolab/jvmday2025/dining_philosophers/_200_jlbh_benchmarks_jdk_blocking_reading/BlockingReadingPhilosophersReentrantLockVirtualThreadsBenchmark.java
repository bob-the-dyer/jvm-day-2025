package ru.spb.kupchinolab.jvmday2025.dining_philosophers._200_jlbh_benchmarks_jdk_blocking_reading;

import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._020_reentrant_pivot.ReentrantPhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class BlockingReadingPhilosophersReentrantLockVirtualThreadsBenchmark implements JLBHTask {

    private JLBH jlbh;

    private NanoSampler eatingSampler;

    private static Consumer<Integer> constructBlockingReadingEating() {
        return (_) -> {
            try (InputStream in = Path.of("16KB_file.txt").toFile().toURI().toURL().openStream()) { //read sequentially from SSD with speed of 1MB in 1M nanosec
                byte[] bytes = in.readAllBytes(); //TODO ?? possible compiler optimization here?
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads(NanoSampler eatingSampler) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructBlockingReadingEating(), eatingSampler);
    }

    private void test_philosophers_internal(ThreadFactory factory, Function<List<Object>, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating, NanoSampler eatingSampler) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<Callable<Integer>> philosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT_BASE);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT_BASE - 1);
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating)));
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            long eatingStart = System.nanoTime();
            scope.join();
            eatingSampler.sampleNanos(System.nanoTime() - eatingStart);
        }
    }

    public static void main(String[] args) {
        JLBHOptions lth = new JLBHOptions()
                .warmUpIterations(7)
                .iterations(7)
                .throughput(1)//TODO ??? не увидел активного влияния, только индикативная функция в логах?
                .runs(3)
                .recordOSJitter(true)
                .accountForCoordinatedOmission(false)
                .jlbhTask(new BlockingReadingPhilosophersReentrantLockVirtualThreadsBenchmark());
        new JLBH(lth).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        eatingSampler = jlbh.addProbe("eating with ReentrantLock+VirtualThreads");
    }

    @Override
    public void run(long startTimeNS) {
        try {
            test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads(eatingSampler);
            jlbh.sample(System.nanoTime() - startTimeNS);
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}
