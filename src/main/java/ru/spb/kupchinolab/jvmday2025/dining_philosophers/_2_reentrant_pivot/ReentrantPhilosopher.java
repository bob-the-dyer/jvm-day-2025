package ru.spb.kupchinolab.jvmday2025.dining_philosophers._2_reentrant_pivot;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers._2_reentrant_pivot.Utils.MAX_EAT_ATTEMPTS;

class ReentrantPhilosopher implements Callable<Integer> {

    private final Chopstick firstChopstick;
    private final Chopstick secondChopstick;
    private int stats;
    private final CountDownLatch latch;

    ReentrantPhilosopher(int order, Chopstick leftChopstick, Chopstick rightChopstick, CountDownLatch latch) {
        this.stats = 0;
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
    public Integer call() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (!Thread.currentThread().isInterrupted() && MAX_EAT_ATTEMPTS > stats) {
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

        return stats;
    }

    private void eat() {
        //NO_OP
        stats++;
    }

}
