package ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_reentrant_naive;

import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class Main {

    private final static int TIME_TO_RUN_IN_MILLIS = 10_000;

    public static void main(String[] args) {

        List<Chopstick> chopsticks = new ArrayList<>();

        IntStream.range(0, PHILOSOPHERS_COUNT_BASE).forEach(i -> {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        });

        AtomicLong stats = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("start at " + Instant.now());

        IntStream.range(0, PHILOSOPHERS_COUNT_BASE).forEach(i -> {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT_BASE - 1);
            ReentrantPhilosopher p = new ReentrantPhilosopher(i, leftChopstick, rightChopstick, stats, latch);
            Thread.startVirtualThread(p); //<----!!
//            Thread.ofPlatform().start(p); //<----!!
        });

        System.out.println("count... " + Instant.now());
        latch.countDown();
        System.out.println("... down " + Instant.now());

        exitAfterDelay(stats);
    }

    public static void exitAfterDelay(AtomicLong stats) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("  end at " + Instant.now());
                System.out.println(format("stopping with overall eat attempts: %d", stats.longValue()));
                System.out.println("platform threads count estimate: " + Thread.activeCount());
                System.exit(0);
            }
        }, TIME_TO_RUN_IN_MILLIS);
    }

}
