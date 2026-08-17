package com.bitdreamit.mirth.labextensions.serialconnector;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Protocol traffic logger — writes incoming/outgoing serial data to mirth.log
 * and maintains an in-memory ring buffer for UI display.
 *
 * Wired into SerialSourceConnector and SerialDestinationConnector when
 * protocolLoggingEnabled = true in SerialPortConfig.
 */
public class ProtocolLogger {
    private static final Logger logger = Logger.getLogger(ProtocolLogger.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final String channelId;
    private final String portName;
    private final int maxEntries;
    private final List<ProtocolLogEntry> entries;
    private final Object lock = new Object();

    public ProtocolLogger(String channelId, String portName, int maxEntries) {
        this.channelId = channelId;
        this.portName = portName;
        this.maxEntries = maxEntries > 0 ? maxEntries : 1000;
        this.entries = new LinkedList<>();
    }

    /**
     * Log incoming data (received from serial port).
     */
    public void logIn(byte[] data, String description) {
        addEntry(ProtocolLogEntry.Direction.IN, data, description);
    }

    /**
     * Log outgoing data (written to serial port).
     */
    public void logOut(byte[] data, String description) {
        addEntry(ProtocolLogEntry.Direction.OUT, data, description);
    }

    private void addEntry(ProtocolLogEntry.Direction direction, byte[] data, String description) {
        ProtocolLogEntry entry = new ProtocolLogEntry(direction, data, description);

        synchronized (lock) {
            entries.add(entry);
            while (entries.size() > maxEntries) {
                entries.remove(0);
            }
        }

        // Also log to mirth.log (log4j) for server-side visibility
        String dirStr = direction == ProtocolLogEntry.Direction.IN ? "IN " : "OUT";
        String hexPreview = toHexPreview(data, 32);
        logger.info("[Serial " + dirStr + "] channel=" + channelId + " port=" + portName +
                    " bytes=" + data.length + " hex=" + hexPreview +
                    (description != null && !description.isEmpty() ? " desc=" + description : ""));
    }

    /**
     * Convert bytes to a hex+ASCII preview string.
     * Example: "0B 4D 53 48 7C ... | .MSH|"
     */
    private String toHexPreview(byte[] data, int maxBytes) {
        if (data == null || data.length == 0) return "[]";
        int len = Math.min(data.length, maxBytes);
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < len; i++) {
            byte b = data[i];
            hex.append(String.format("%02X", b & 0xFF));
            if (i < len - 1) hex.append(" ");
            if (b >= 32 && b < 127) {
                ascii.append((char) b);
            } else {
                ascii.append('.');
            }
        }
        if (data.length > maxBytes) {
            hex.append("...(+").append(data.length - maxBytes).append(")");
            ascii.append("...");
        }
        return "[" + hex + " | " + ascii + "]";
    }

    /**
     * Get a snapshot of log entries (for UI display).
     */
    public List<ProtocolLogEntry> getEntries() {
        synchronized (lock) {
            return new ArrayList<>(entries);
        }
    }

    /**
     * Clear all entries.
     */
    public void clear() {
        synchronized (lock) {
            entries.clear();
        }
    }

    /**
     * Export entries to a CSV file.
     */
    public void exportToCsv(File file) throws Exception {
        synchronized (lock) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("timestamp,direction,bytes,description,hex");
                for (ProtocolLogEntry e : entries) {
                    pw.printf("%s,%s,%d,%s,%s%n",
                            DATE_FMT.format(new Date(e.getTimestamp())),
                            e.getDirection(),
                            e.getData().length,
                            e.getDescription() != null ? e.getDescription().replace(",", ";") : "",
                            toHexPreview(e.getData(), 64));
                }
            }
        }
    }
}
