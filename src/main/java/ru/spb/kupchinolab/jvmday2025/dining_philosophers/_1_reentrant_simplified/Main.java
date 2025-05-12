package ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_simplified;

import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_simplified.Utils.PHILOSOPHERS_COUNT;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_simplified.Utils.exitAfterDelay;

public class Main {

    public static void main(String[] args) {

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
            ReentrantPhilosopher p = new ReentrantPhilosopher(i, leftChopstick, rightChopstick, stats, latch);
            Thread.startVirtualThread(p); //<----!!
//            Thread.ofPlatform().start(p); //<----!!
        });

        System.out.println("count... " + Instant.now());
        latch.countDown();
        System.out.println("... down " + Instant.now());

        exitAfterDelay(stats);
    }

}
