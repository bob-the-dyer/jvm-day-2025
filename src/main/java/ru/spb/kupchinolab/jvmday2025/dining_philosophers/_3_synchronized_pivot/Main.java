package ru.spb.kupchinolab.jvmday2025.dining_philosophers._3_synchronized_pivot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers._3_synchronized_pivot.Utils.PHILOSOPHERS_COUNT;

public class Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        List<Chopstick> chopsticks = new ArrayList<>();

        IntStream.range(0, PHILOSOPHERS_COUNT).forEach(i -> {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        });

        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("start at " + Instant.now());

        ThreadFactory factory = Thread.ofVirtual().factory();

        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
                Chopstick leftChopstick = chopsticks.get(i);
                Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
                SynchronizedPhilosopher p = new SynchronizedPhilosopher(i, leftChopstick, rightChopstick, latch);
                scope.fork(p);
            }
            System.out.println("count... " + Instant.now());
            latch.countDown();
            System.out.println("... down " + Instant.now());
            ShutdownOnSuccess<Integer> join = scope.join();
            System.out.println("attempts " + join.result());
            System.out.println("FINISH! " + Instant.now());
        }
    }

}
