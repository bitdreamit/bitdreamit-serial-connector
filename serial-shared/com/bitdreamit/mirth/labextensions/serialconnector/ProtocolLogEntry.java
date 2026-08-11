/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Bit Dream IT — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Protocol analyzer log entry.
 * Extra feature: captures raw hex + ASCII for debugging.
 */
public class ProtocolLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    public enum Direction { IN, OUT, EVENT, ERROR }

    private long timestamp;
    private Direction direction;
    private byte[] rawData;
    private String description;

    public ProtocolLogEntry(Direction direction, byte[] rawData, String description) {
        this.timestamp = System.currentTimeMillis();
        this.direction = direction;
        this.rawData = rawData != null ? rawData.clone() : new byte[0];
        this.description = description;
    }

    public String getFormattedTime() { return fmt.format(new Date(timestamp)); }
    public Direction getDirection() { return direction; }
    public byte[] getRawData() { return rawData; }
    public String getDescription() { return description; }

    public String getHexDump() {
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < rawData.length; i++) {
            if (i > 0 && i % 16 == 0) {
                hex.append("  ").append(ascii).append("\n");
                ascii.setLength(0);
            }
            hex.append(String.format("%02X ", rawData[i] & 0xFF));
            char c = (char) (rawData[i] & 0xFF);
            ascii.append(c >= 32 && c < 127 ? c : '.');
        }
        if (ascii.length() > 0) {
            for (int i = rawData.length % 16; i < 16 && i != 0; i++) hex.append("   ");
            hex.append("  ").append(ascii);
        }
        return hex.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s — %s", getFormattedTime(), direction, description, getHexDump().replace("\n", " "));
    }
}