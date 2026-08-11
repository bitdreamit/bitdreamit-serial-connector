/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Bit Dream IT — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time serial port statistics.
 * Extra feature beyond commercial extension.
 */
public class SerialStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong framesReceived = new AtomicLong(0);
    private final AtomicLong framesSent = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong reconnects = new AtomicLong(0);
    private volatile long lastReadTime = 0;
    private volatile long lastWriteTime = 0;
    private volatile long startTime = System.currentTimeMillis();

    public void recordRead(int bytes) {
        bytesRead.addAndGet(bytes);
        framesReceived.incrementAndGet();
        lastReadTime = System.currentTimeMillis();
    }

    public void recordWrite(int bytes) {
        bytesWritten.addAndGet(bytes);
        framesSent.incrementAndGet();
        lastWriteTime = System.currentTimeMillis();
    }

    public void recordError() { errors.incrementAndGet(); }
    public void recordReconnect() { reconnects.incrementAndGet(); }

    public long getBytesRead() { return bytesRead.get(); }
    public long getBytesWritten() { return bytesWritten.get(); }
    public long getFramesReceived() { return framesReceived.get(); }
    public long getFramesSent() { return framesSent.get(); }
    public long getErrors() { return errors.get(); }
    public long getReconnects() { return reconnects.get(); }
    public long getLastReadTime() { return lastReadTime; }
    public long getLastWriteTime() { return lastWriteTime; }
    public long getUptime() { return System.currentTimeMillis() - startTime; }

    public double getBytesPerSecond() {
        long uptime = getUptime();
        return uptime > 0 ? (bytesRead.get() + bytesWritten.get()) * 1000.0 / uptime : 0;
    }

    public void reset() {
        bytesRead.set(0); bytesWritten.set(0); framesReceived.set(0);
        framesSent.set(0); errors.set(0); reconnects.set(0);
        startTime = System.currentTimeMillis();
    }
}