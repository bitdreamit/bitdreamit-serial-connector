/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Bit Dream IT — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("serialDispatcherProperties")
public class SerialDispatcherProperties extends ConnectorProperties implements DestinationConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private boolean waitForAckAfterWrite = false;
    private int ackTimeout = 1000;
    private byte[] ackPattern = new byte[]{0x06};
    private boolean keepConnectionOpen = false;

    private DestinationConnectorProperties destinationConnectorProperties;

    public SerialDispatcherProperties() {
        this.destinationConnectorProperties = new DestinationConnectorProperties();
    }

    public SerialPortConfig getPortConfig() { return portConfig; }
    public void setPortConfig(SerialPortConfig portConfig) { this.portConfig = portConfig; }
    public boolean isWaitForAckAfterWrite() { return waitForAckAfterWrite; }
    public void setWaitForAckAfterWrite(boolean waitForAckAfterWrite) { this.waitForAckAfterWrite = waitForAckAfterWrite; }
    public int getAckTimeout() { return ackTimeout; }
    public void setAckTimeout(int ackTimeout) { this.ackTimeout = ackTimeout; }
    public byte[] getAckPattern() { return ackPattern; }
    public void setAckPattern(byte[] ackPattern) { this.ackPattern = ackPattern; }
    public boolean isKeepConnectionOpen() { return keepConnectionOpen; }
    public void setKeepConnectionOpen(boolean keepConnectionOpen) { this.keepConnectionOpen = keepConnectionOpen; }

    @Override public String getProtocol() { return "Serial"; }
    @Override public String getName() { return "Serial Writer"; }
    @Override public String toFormattedString() {
        return "Serial [" + portConfig.getPortName() + " @ " + portConfig.getBaudRate() +
                ", WaitACK=" + waitForAckAfterWrite + "]";
    }

    @Override
    public ConnectorProperties clone() {
        SerialDispatcherProperties copy = new SerialDispatcherProperties();
        copy.portConfig = this.portConfig;
        copy.waitForAckAfterWrite = this.waitForAckAfterWrite;
        copy.ackTimeout = this.ackTimeout;
        copy.ackPattern = this.ackPattern;
        copy.keepConnectionOpen = this.keepConnectionOpen;
        copy.destinationConnectorProperties = this.destinationConnectorProperties;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SerialDispatcherProperties other = (SerialDispatcherProperties) obj;
        return waitForAckAfterWrite == other.waitForAckAfterWrite
                && ackTimeout == other.ackTimeout
                && keepConnectionOpen == other.keepConnectionOpen
                && java.util.Objects.equals(portConfig, other.portConfig)
                && java.util.Arrays.equals(ackPattern, other.ackPattern)
                && java.util.Objects.equals(destinationConnectorProperties, other.destinationConnectorProperties);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(portConfig, waitForAckAfterWrite, ackTimeout,
                keepConnectionOpen, java.util.Arrays.hashCode(ackPattern), destinationConnectorProperties);
    }

    // Only getter is @Override — setter is NOT part of the interface in Mirth 4.x
    @Override
    public DestinationConnectorProperties getDestinationConnectorProperties() {
        return destinationConnectorProperties;
    }

    @Override
    public boolean canValidateResponse() {
        return false;
    }

    // Regular method, NOT @Override
    public void setDestinationConnectorProperties(DestinationConnectorProperties destinationConnectorProperties) {
        this.destinationConnectorProperties = destinationConnectorProperties;
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {

    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {

    }
}