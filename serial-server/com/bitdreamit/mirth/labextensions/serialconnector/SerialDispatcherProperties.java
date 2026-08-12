package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

@XStreamAlias("serialDispatcherProperties")
public class SerialDispatcherProperties extends ConnectorProperties implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig;

    private boolean waitForAckAfterWrite = false;
    private int ackTimeout = 1000;
    private byte[] ackPattern = new byte[]{0x06};
    private boolean keepConnectionOpen = false;

    public SerialDispatcherProperties() {
        this.portConfig = new SerialPortConfig();
    }

    public SerialPortConfig getPortConfig() {
        if (portConfig == null) {
            portConfig = new SerialPortConfig();
        }
        return portConfig;
    }

    public void setPortConfig(SerialPortConfig portConfig) {
        this.portConfig = portConfig;
    }

    public boolean isWaitForAckAfterWrite() {
        return waitForAckAfterWrite;
    }

    public void setWaitForAckAfterWrite(boolean waitForAckAfterWrite) {
        this.waitForAckAfterWrite = waitForAckAfterWrite;
    }

    public int getAckTimeout() {
        return ackTimeout;
    }

    public void setAckTimeout(int ackTimeout) {
        this.ackTimeout = ackTimeout;
    }

    public byte[] getAckPattern() {
        return ackPattern;
    }

    public void setAckPattern(byte[] ackPattern) {
        this.ackPattern = ackPattern;
    }

    public boolean isKeepConnectionOpen() {
        return keepConnectionOpen;
    }

    public void setKeepConnectionOpen(boolean keepConnectionOpen) {
        this.keepConnectionOpen = keepConnectionOpen;
    }

    @Override
    public String getProtocol() {
        return "Serial";
    }

    @Override
    public String getName() {
        return "Serial Writer";
    }

    @Override
    public String toFormattedString() {
        SerialPortConfig c = getPortConfig();
        StringBuilder sb = new StringBuilder();
        sb.append("Port: ").append(c.getPortName())
                .append(", Baud: ").append(c.getBaudRate())
                .append(", Mode: ").append(c.getTransmissionMode());
        if (waitForAckAfterWrite) {
            sb.append(", ACK: ").append(ackTimeout).append("ms");
        }
        if (keepConnectionOpen) {
            sb.append(", Pooled");
        }
        return sb.toString();
    }

    @Override
    public SerialDispatcherProperties clone() {
        try {
            SerialDispatcherProperties copy = (SerialDispatcherProperties) super.clone();
            copy.portConfig = (this.portConfig != null) ? this.portConfig.clone() : new SerialPortConfig();
            if (this.ackPattern != null) {
                copy.ackPattern = this.ackPattern.clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialDispatcherProperties)) return false;
        SerialDispatcherProperties that = (SerialDispatcherProperties) o;
        return waitForAckAfterWrite == that.waitForAckAfterWrite &&
                ackTimeout == that.ackTimeout &&
                keepConnectionOpen == that.keepConnectionOpen &&
                Objects.equals(getPortConfig(), that.getPortConfig()) &&
                Arrays.equals(ackPattern, that.ackPattern);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getPortConfig(), waitForAckAfterWrite, ackTimeout, keepConnectionOpen);
        result = 31 * result + Arrays.hashCode(ackPattern);
        return result;
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {

    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {

    }
}