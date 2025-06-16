package ru.spb.kupchinolab.jvmday2025.dining_philosophers._010_reentrant_naive;

import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

class ReentrantPhilosopher implements Runnable {

    private final Chopstick firstChopstick;
    private final Chopstick secondChopstick;
    private final AtomicLong stats;
    private final CountDownLatch latch;

    ReentrantPhilosopher(int order, Chopstick leftChopstick, Chopstick rightChopstick, AtomicLong stats, CountDownLatch latch) {
        this.stats = stats;
        this.latch = latch;
        if (rightChopstick.getOrder() < leftChopstick.getOrder()) {
            assert order != 0;
            firstChopstick = rightChopstick;
            secondChopstick = leftChopstick;
        } else { // leftChopstick.order < rightChopstick.order
            assert order == 0;
            firstChopstick = leftChopstick;
            secondChopstick = rightChopstick;
        }
    }

    @Override
    public void run() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            firstChopstick.lock();
            try {
                secondChopstick.lock();
                try {
                    eat();
                } finally {
                    secondChopstick.unlock();
                }
            } finally {
                firstChopstick.unlock();
            }
        }
    }

    private void eat() {
        //NO_OP
        stats.incrementAndGet();
    }

}
