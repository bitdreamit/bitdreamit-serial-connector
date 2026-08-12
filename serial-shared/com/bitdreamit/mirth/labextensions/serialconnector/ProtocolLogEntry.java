package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.util.Arrays;

public class ProtocolLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Direction { IN, OUT }

    private final long timestamp;
    private final Direction direction;
    private final byte[] data;
    private final String description;

    public ProtocolLogEntry(Direction direction, byte[] data, String description) {
        this.timestamp = System.currentTimeMillis();
        this.direction = direction;
        this.data = data != null ? data.clone() : new byte[0];
        this.description = description;
    }

    public long getTimestamp() { return timestamp; }
    public Direction getDirection() { return direction; }
    public byte[] getData() { return data.clone(); }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "ProtocolLogEntry{" +
                "timestamp=" + timestamp +
                ", direction=" + direction +
                ", data=" + Arrays.toString(data) +
                ", description=\"" + description + "\"" +
                '}';
    }
}