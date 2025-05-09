package ru.spb.kupchinolab.jvmday2025.dining_philosophers._1_reentrant_lock_old_simplified;

import java.util.concurrent.locks.ReentrantLock;

class Chopstick extends ReentrantLock {

    final int order;

    Chopstick(int order) {
        this.order = order;
    }
}
