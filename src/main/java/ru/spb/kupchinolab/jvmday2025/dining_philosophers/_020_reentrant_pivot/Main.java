package ru.spb.kupchinolab.jvmday2025.dining_philosophers._020_reentrant_pivot;

import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException, BrokenBarrierException {

        List<Chopstick> chopsticks = new ArrayList<>();

        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }

        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT_BASE);

        System.out.println("start at " + Instant.now());

        ThreadFactory factory = Thread.ofPlatform().factory();

        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
                Chopstick leftChopstick = chopsticks.get(i);
                Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT_BASE - 1);
                scope.fork(new ReentrantPhilosopher(i, leftChopstick, rightChopstick, barrier, _ -> {/*NO_OP*/}));
            }
            System.out.println("count... " + Instant.now());
            barrier.await();
            System.out.println("... down " + Instant.now());
            ShutdownOnSuccess<Integer> join = scope.join();
            System.out.println("attempts " + join.result());
            System.out.println("FINISH! " + Instant.now());
        }
    }

}
