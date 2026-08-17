package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serial statistics counters — tracks bytes read/written, errors, reconnects, and messages.
 *
 * Wired into SerialSourceConnector and SerialDestinationConnector.
 * Can be exposed via JMX or a status servlet in the future.
 */
public class SerialStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong reconnectCount = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong lastActivityTime = new AtomicLong(0);

    public void recordRead(int bytes) {
        bytesRead.addAndGet(bytes);
        lastActivityTime.set(System.currentTimeMillis());
    }
    public void recordWrite(int bytes) {
        bytesWritten.addAndGet(bytes);
        lastActivityTime.set(System.currentTimeMillis());
    }
    public void recordError() { errorCount.incrementAndGet(); }
    public void recordReconnect() { reconnectCount.incrementAndGet(); }
    public void recordMessageReceived() { messagesReceived.incrementAndGet(); }
    public void recordMessageSent() { messagesSent.incrementAndGet(); }

    public long getBytesRead() { return bytesRead.get(); }
    public long getBytesWritten() { return bytesWritten.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getReconnectCount() { return reconnectCount.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
    public long getMessagesSent() { return messagesSent.get(); }
    public long getLastActivityTime() { return lastActivityTime.get(); }

    /** Reset all counters (called on channel redeploy). */
    public void reset() {
        bytesRead.set(0);
        bytesWritten.set(0);
        errorCount.set(0);
        reconnectCount.set(0);
        messagesReceived.set(0);
        messagesSent.set(0);
        lastActivityTime.set(0);
    }

    @Override
    public String toString() {
        return "SerialStatistics{" +
               "bytesRead=" + bytesRead +
               ", bytesWritten=" + bytesWritten +
               ", messagesReceived=" + messagesReceived +
               ", messagesSent=" + messagesSent +
               ", errors=" + errorCount +
               ", reconnects=" + reconnectCount +
               ", lastActivity=" + lastActivityTime +
               '}';
    }
}
