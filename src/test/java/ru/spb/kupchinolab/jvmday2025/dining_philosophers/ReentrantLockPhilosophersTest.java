package ru.spb.kupchinolab.jvmday2025.dining_philosophers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._2_reentrant_pivot.ReentrantPhilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;

public class ReentrantLockPhilosophersTest {

    private static final int TEST_PHILOSOPHERS_COUNT = 1_000;

    static List<Chopstick> chopsticks = new ArrayList<>();
    static List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
    static CyclicBarrier barrier = new CyclicBarrier(1 + TEST_PHILOSOPHERS_COUNT);

    @BeforeAll
    static void initPhilosophersAndChopsticks() {
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

    @AfterEach
    void resetBarrierAndPhilosophers() {
        barrier.reset();
        reentrantPhilosophers.forEach(ReentrantPhilosopher::resetStats);
    }

    @RepeatedTest(100)
    void test_reentrant_lock_philosophers_with_virtual_threads() throws InterruptedException, BrokenBarrierException {
        test_reentrant_lock_philosophers_internal(Thread.ofVirtual().factory());
    }

    @RepeatedTest(100)
    void test_reentrant_lock_philosophers_with_platform_threads() throws InterruptedException, BrokenBarrierException {
        test_reentrant_lock_philosophers_internal(Thread.ofPlatform().factory());
    }

    private void test_reentrant_lock_philosophers_internal(ThreadFactory factory) throws InterruptedException, BrokenBarrierException {
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            reentrantPhilosophers.forEach(scope::fork);
            ReentrantPhilosopher.eating = (stats) -> {/*NO_OP*/};
            barrier.await();
            scope.join();
        }
    }
}
