package ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_lock_old_simplified;

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
        if (rightChopstick.order < leftChopstick.order) {
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
                    //NO_OP
                    stats.incrementAndGet();
                } finally {
                    secondChopstick.unlock();
                }
            } finally {
                firstChopstick.unlock();
            }
        }
    }

}
