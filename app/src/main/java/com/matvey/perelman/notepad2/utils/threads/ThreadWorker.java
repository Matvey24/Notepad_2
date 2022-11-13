package com.matvey.perelman.notepad2.utils.threads;

public class ThreadWorker extends Thread {

    private final Tasks header;

    private final Locker locker;

    private boolean disposed;

    public ThreadWorker(Tasks header) {
        this.header = header;
        disposed = false;
        locker = new Locker();
        start();
    }

    @Override
    public void run() {
        lock();
        while (!disposed) {
            doQueue();
            lock();
        }
    }

    private void doQueue() {
        Runnable task;
        while ((task = header.getTask()) != null) {
            task.run();
        }
        header.onFinish(this);
    }

    private void lock() {
        if (!disposed)
            locker.lock();
    }

    public void begin() {
        locker.free();
    }

    public void dispose() {
        end();
        begin();
    }

    public void end() {
        disposed = true;
    }
}
