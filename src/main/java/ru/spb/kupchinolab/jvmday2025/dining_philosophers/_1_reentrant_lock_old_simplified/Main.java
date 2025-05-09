package ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_lock_old_simplified;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.exitAfterDelay;

public class Main {

    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {

        List<Chopstick> chopsticks = new ArrayList<>();

        IntStream.range(0, PHILOSOPHERS_COUNT).forEach(i -> {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        });

        AtomicLong stats = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("start at " + Instant.now());

        IntStream.range(0, PHILOSOPHERS_COUNT).forEach(i -> {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT - 1);
            Philosopher p = new Philosopher(i, leftChopstick, rightChopstick, stats, latch);
            Thread.startVirtualThread(p); //<----!!
//            Thread.ofPlatform().start(p); //<----!!
        });

        System.out.println("before countDown " + Instant.now());
        latch.countDown();
        System.out.println(" after countDown " + Instant.now());

        exitAfterDelay(stats);
    }

}
