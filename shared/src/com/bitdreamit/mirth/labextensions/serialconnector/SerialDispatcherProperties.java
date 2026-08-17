package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Arrays;
import java.util.Objects;

/**
 * Serial Destination Properties.
 *
 * Uses Mirth's TransmissionModeProperties framework — same as TCP.
 * Modes are loaded DYNAMICALLY from Mirth's extension system.
 *
 * CRITICAL: This class MUST exist ONLY in serial-shared.jar.
 */
@XStreamAlias("serialDispatcherProperties")
public class SerialDispatcherProperties extends ConnectorProperties implements DestinationConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private DestinationConnectorProperties destinationConnectorProperties = new DestinationConnectorProperties();

    // DYNAMIC: Stores the transmission mode properties (MLLP, ASTM, Frame, etc.)
    private TransmissionModeProperties transmissionModeProperties;

    private boolean waitForAckAfterWrite = false;
    private int ackTimeout = 1000;
    private byte[] ackPattern = new byte[]{0x06};
    private boolean keepConnectionOpen = false;

    public SerialDispatcherProperties() {
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

    @Override
    public DestinationConnectorProperties getDestinationConnectorProperties() {
        if (destinationConnectorProperties == null) {
            destinationConnectorProperties = new DestinationConnectorProperties();
        }
        return destinationConnectorProperties;
    }

    public void setDestinationConnectorProperties(DestinationConnectorProperties destinationConnectorProperties) {
        this.destinationConnectorProperties = destinationConnectorProperties;
    }

    public boolean isWaitForAckAfterWrite() { return waitForAckAfterWrite; }
    public void setWaitForAckAfterWrite(boolean waitForAckAfterWrite) { this.waitForAckAfterWrite = waitForAckAfterWrite; }

    public int getAckTimeout() { return ackTimeout; }
    public void setAckTimeout(int ackTimeout) { this.ackTimeout = ackTimeout; }

    public byte[] getAckPattern() { return ackPattern; }
    public void setAckPattern(byte[] ackPattern) {
        this.ackPattern = ackPattern != null ? ackPattern.clone() : null;
    }

    public boolean isKeepConnectionOpen() { return keepConnectionOpen; }
    public void setKeepConnectionOpen(boolean keepConnectionOpen) { this.keepConnectionOpen = keepConnectionOpen; }

    // DYNAMIC: Transmission mode properties
    public TransmissionModeProperties getTransmissionModeProperties() { return transmissionModeProperties; }
    public void setTransmissionModeProperties(TransmissionModeProperties transmissionModeProperties) {
        this.transmissionModeProperties = transmissionModeProperties;
    }

    @Override
    public String getProtocol() { return "serial"; }

    @Override
    public String getName() { return "Serial Writer"; }

    @Override
    public String toFormattedString() {
        SerialPortConfig c = getPortConfig();
        StringBuilder sb = new StringBuilder();
        sb.append("Port: ").append(c.getPortName())
                .append(", Baud: ").append(c.getBaudRate());
        if (transmissionModeProperties != null) {
            sb.append(", Mode: ").append(transmissionModeProperties.getPluginPointName());
        }
        if (waitForAckAfterWrite) sb.append(", ACK: ").append(ackTimeout).append("ms");
        if (keepConnectionOpen)   sb.append(", Pooled");
        return sb.toString();
    }

    @Override
    public boolean canValidateResponse() { return false; }

    @Override
    public SerialDispatcherProperties clone() {
        try {
            SerialDispatcherProperties copy = (SerialDispatcherProperties) super.clone();
            copy.portConfig = (this.portConfig != null) ? this.portConfig.clone() : new SerialPortConfig();
            if (this.ackPattern != null) {
                copy.ackPattern = this.ackPattern.clone();
            }
            copy.destinationConnectorProperties = this.destinationConnectorProperties;
            copy.transmissionModeProperties = this.transmissionModeProperties;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialDispatcherProperties)) return false;
        SerialDispatcherProperties that = (SerialDispatcherProperties) o;
        return waitForAckAfterWrite == that.waitForAckAfterWrite
                && ackTimeout == that.ackTimeout
                && keepConnectionOpen == that.keepConnectionOpen
                && Objects.equals(getPortConfig(), that.getPortConfig())
                && Objects.equals(getDestinationConnectorProperties(), that.getDestinationConnectorProperties())
                && Objects.equals(getTransmissionModeProperties(), that.getTransmissionModeProperties())
                && Arrays.equals(ackPattern, that.ackPattern);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getPortConfig(), getDestinationConnectorProperties(),
                getTransmissionModeProperties(),
                waitForAckAfterWrite, ackTimeout, keepConnectionOpen);
        return 31 * result + Arrays.hashCode(ackPattern);
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) { }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) { }
}
