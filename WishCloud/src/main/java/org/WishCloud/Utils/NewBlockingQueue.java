package org.WishCloud.Utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NewBlockingQueue<T> {
    private final Queue<T> queue;
    private final int capacity;
    private int length;
    private final Lock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    public NewBlockingQueue(int capacity){
        this.queue = new LinkedList<>();
        this.capacity = capacity;
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.offer(item);
            length++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            T item = queue.poll();
            length--;

            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty(){
        return length == 0;
    }

    public boolean isFull(){
        return length == capacity;
    }

    public int size(){
        return length;
    }

    public boolean contains(T item){
        return queue.contains(item);
    }
}

