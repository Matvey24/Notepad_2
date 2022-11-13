package com.matvey.perelman.notepad2.utils.threads;

public class Locker {
    private final Object thread_locker, thread_semaphore, edit_locker;
    private int count;

    public Locker() {
        thread_locker = new Object();
        thread_semaphore = new Object();
        edit_locker = new Object();
    }

    public void lock() {
        synchronized (thread_semaphore) {
            synchronized (thread_locker) {
                if (check_count())
                    return;
                try {
                    thread_locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void free() {
        synchronized (edit_locker) {
            count++;
            if (count > 0)
                return;
        }
        synchronized (thread_locker) {
            thread_locker.notify();
        }
    }

    private boolean check_count() {
        synchronized (edit_locker) {
            count--;
            return count > -1;
        }
    }

}
