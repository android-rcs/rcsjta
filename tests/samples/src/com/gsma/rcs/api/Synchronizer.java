package com.gsma.rcs.api;

public class Synchronizer {
    
    public void doWait() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public void doNotify() {
        synchronized (this) {
            this.notify();
        }
    }
}
