package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class SerialStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong reconnectCount = new AtomicLong(0);

    public void recordRead(int bytes) { bytesRead.addAndGet(bytes); }
    public void recordWrite(int bytes) { bytesWritten.addAndGet(bytes); }
    public void recordError() { errorCount.incrementAndGet(); }
    public void recordReconnect() { reconnectCount.incrementAndGet(); }

    public long getBytesRead() { return bytesRead.get(); }
    public long getBytesWritten() { return bytesWritten.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getReconnectCount() { return reconnectCount.get(); }
}
