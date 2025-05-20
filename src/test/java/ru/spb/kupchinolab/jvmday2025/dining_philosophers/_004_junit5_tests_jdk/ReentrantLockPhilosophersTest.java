package ru.spb.kupchinolab.jvmday2025.dining_philosophers._004_junit5_tests_jdk;

import org.junit.jupiter.api.RepeatedTest;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._002_reentrant_pivot.ReentrantPhilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class ReentrantLockPhilosophersTest {

    @RepeatedTest(200)
    void test_reentrant_lock_philosophers_with_platform_threads() throws InterruptedException, BrokenBarrierException, ExecutionException {
        test_reentrant_lock_philosophers_internal(Thread.ofPlatform().factory());
    }

    @RepeatedTest(200)
    void test_reentrant_lock_philosophers_with_virtual_threads() throws InterruptedException, BrokenBarrierException, ExecutionException {
        test_reentrant_lock_philosophers_internal(Thread.ofVirtual().factory());
    }

    private void test_reentrant_lock_philosophers_internal(ThreadFactory factory) throws InterruptedException, BrokenBarrierException, ExecutionException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<ReentrantPhilosopher> reentrantPhilosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT_BASE);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT_BASE - 1);
            reentrantPhilosophers.add(new ReentrantPhilosopher(i, leftChopstick, rightChopstick, barrier, _ -> {/*NO_OP*/}));
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            reentrantPhilosophers.forEach(scope::fork);
            barrier.await();
            System.out.println("max eat attempts has reached: " + scope.join().result());
        }
    }
}
