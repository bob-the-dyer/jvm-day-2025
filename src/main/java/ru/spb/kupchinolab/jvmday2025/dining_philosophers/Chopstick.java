package ru.spb.kupchinolab.jvmday2025.dining_philosophers;

import java.util.concurrent.locks.ReentrantLock;

public class Chopstick extends ReentrantLock {

    final int order;

    public Chopstick(int order) {
        this.order = order;
    }

    public int getOrder(){
        return order;
    }
}
