package com.felhr.usbserial;

abstract class AbstractWorkerThread extends Thread {
    boolean firstTime = true;
    private volatile boolean keep = true;

    void stopThread() {
        keep = false;
    }

    public final void run() {
        while (keep) {
            doRun();
        }
    }

    abstract void doRun();
}
