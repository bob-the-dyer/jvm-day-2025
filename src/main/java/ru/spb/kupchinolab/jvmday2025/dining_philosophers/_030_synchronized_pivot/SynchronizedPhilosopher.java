package ru.spb.kupchinolab.jvmday2025.dining_philosophers._030_synchronized_pivot;

import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.MAX_EAT_ATTEMPTS_BASE;

public class SynchronizedPhilosopher implements Callable<Integer> {

    private final Chopstick firstChopstick;
    private final Chopstick secondChopstick;
    private int stats;
    private final CyclicBarrier barrier;
    private final Consumer<Integer> eating;
    private final int eatingMultiplier;

    public SynchronizedPhilosopher(int order, Chopstick leftChopstick, Chopstick rightChopstick, CyclicBarrier barrier, Consumer<Integer> eating) {
        this(order, leftChopstick, rightChopstick, barrier, eating, 1);
    }

    public SynchronizedPhilosopher(int order, Chopstick leftChopstick, Chopstick rightChopstick, CyclicBarrier barrier, Consumer<Integer> eating, int eatingMultiplier) {
        this.barrier = barrier;
        this.eating = eating;
        this.eatingMultiplier = eatingMultiplier;
        this.stats = 0;
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
    public Integer call() {
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        while (!Thread.currentThread().isInterrupted() && ((MAX_EAT_ATTEMPTS_BASE * eatingMultiplier) > stats)) {
            synchronized (firstChopstick) {
                synchronized (secondChopstick) {
                    eat();
                }
            }
        }
        return stats;
    }

    private void eat() {
        eating.accept(stats++);
    }

    public static SynchronizedPhilosopher from(Integer order, Chopstick leftChopstick, Chopstick rightChopstick, CyclicBarrier barrier, Consumer<Integer> statsConsumer) {
        return new SynchronizedPhilosopher(order, leftChopstick, rightChopstick, barrier, statsConsumer);
    }

    public static SynchronizedPhilosopher from(Integer order, Chopstick leftChopstick, Chopstick rightChopstick, CyclicBarrier barrier, Consumer<Integer> statsConsumer, int eatingMultiplier) {
        return new SynchronizedPhilosopher(order, leftChopstick, rightChopstick, barrier, statsConsumer, eatingMultiplier);
    }

    public static SynchronizedPhilosopher from(List<Object> from) {
        return from(
                (Integer) from.get(0),
                (Chopstick) from.get(1),
                (Chopstick) from.get(2),
                (CyclicBarrier) from.get(3),
                (Consumer<Integer>) from.get(4)
        );
    }

    public static SynchronizedPhilosopher from(List<Object> from, int eatingMultiplier) {
        return from(
                (Integer) from.get(0),
                (Chopstick) from.get(1),
                (Chopstick) from.get(2),
                (CyclicBarrier) from.get(3),
                (Consumer<Integer>) from.get(4),
                eatingMultiplier
        );
    }
}
