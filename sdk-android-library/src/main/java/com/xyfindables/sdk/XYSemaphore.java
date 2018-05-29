package com.xyfindables.sdk;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by arietrouw on 1/21/17.
 */

public class XYSemaphore extends Semaphore {

    final private static String TAG = XYSemaphore.class.getSimpleName();

    private int _permitCount;

    public XYSemaphore(int permits) {
        super(permits);
        _permitCount = permits;
    }

    public XYSemaphore(int permits, boolean fair) {
        super(permits, fair);
        _permitCount = permits;
    }

    @Override
    public void acquire() throws InterruptedException {
        super.acquire();
    }

    @Override
    public void acquire(int permits) throws InterruptedException {
        super.acquire(permits);
    }

    @Override
    public boolean tryAcquire() {
        return super.tryAcquire();
    }

    @Override
    public boolean tryAcquire(int permits) {
        return super.tryAcquire(permits);
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return super.tryAcquire(timeout, unit);
    }

    @Override
    public void release() {
        super.release();
    }

}