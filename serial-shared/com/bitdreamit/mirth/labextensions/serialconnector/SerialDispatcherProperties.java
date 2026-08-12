package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Arrays;
import java.util.Objects;

/**
 * Serial Destination Properties.
 * transmissionMode: BASIC | MLLP | ASTM_E1381
 * Works with ANY Mirth DataType.
 */
@XStreamAlias("serialDispatcherProperties")
public class SerialDispatcherProperties extends ConnectorProperties implements DestinationConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private DestinationConnectorProperties destinationConnectorProperties;

    // Transmission / Framing
    private String transmissionMode = "BASIC";   // BASIC | MLLP | ASTM_E1381
    private String messageDelimiter = "\\r\\n";   // For BASIC mode

    // Destination-specific
    private boolean waitForAckAfterWrite = false;
    private int ackTimeout = 1000;
    private byte[] ackPattern = new byte[]{0x06};
    private boolean keepConnectionOpen = false;

    public SerialDispatcherProperties() {
        this.destinationConnectorProperties = new DestinationConnectorProperties();
    }

    public SerialPortConfig getPortConfig() { return portConfig; }
    public void setPortConfig(SerialPortConfig portConfig) { this.portConfig = portConfig; }

    public String getTransmissionMode() { return transmissionMode; }
    public void setTransmissionMode(String transmissionMode) { this.transmissionMode = transmissionMode; }
    public String getMessageDelimiter() { return messageDelimiter; }
    public void setMessageDelimiter(String messageDelimiter) { this.messageDelimiter = messageDelimiter; }

    public boolean isWaitForAckAfterWrite() { return waitForAckAfterWrite; }
    public void setWaitForAckAfterWrite(boolean waitForAckAfterWrite) { this.waitForAckAfterWrite = waitForAckAfterWrite; }
    public int getAckTimeout() { return ackTimeout; }
    public void setAckTimeout(int ackTimeout) { this.ackTimeout = ackTimeout; }
    public byte[] getAckPattern() { return ackPattern; }
    public void setAckPattern(byte[] ackPattern) { this.ackPattern = ackPattern != null ? ackPattern.clone() : null; }
    public boolean isKeepConnectionOpen() { return keepConnectionOpen; }
    public void setKeepConnectionOpen(boolean keepConnectionOpen) { this.keepConnectionOpen = keepConnectionOpen; }

    @Override public String getProtocol() { return "Serial"; }
    @Override public String getName() { return "Serial Writer"; }
    @Override public String toFormattedString() {
        return "Serial [" + portConfig.getPortName() + " @ " + portConfig.getBaudRate() +
               ", mode=" + transmissionMode + ", pool=" + keepConnectionOpen + "]";
    }

    @Override
    public ConnectorProperties clone() {
        SerialDispatcherProperties copy = new SerialDispatcherProperties();
        copy.portConfig = this.portConfig != null ? this.portConfig.clone() : new SerialPortConfig();
        copy.transmissionMode = this.transmissionMode;
        copy.messageDelimiter = this.messageDelimiter;
        copy.waitForAckAfterWrite = this.waitForAckAfterWrite;
        copy.ackTimeout = this.ackTimeout;
        copy.ackPattern = this.ackPattern != null ? this.ackPattern.clone() : null;
        copy.keepConnectionOpen = this.keepConnectionOpen;
        copy.destinationConnectorProperties = this.destinationConnectorProperties;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SerialDispatcherProperties other = (SerialDispatcherProperties) obj;
        return waitForAckAfterWrite == other.waitForAckAfterWrite &&
               ackTimeout == other.ackTimeout &&
               keepConnectionOpen == other.keepConnectionOpen &&
               Objects.equals(portConfig, other.portConfig) &&
               Objects.equals(transmissionMode, other.transmissionMode) &&
               Objects.equals(messageDelimiter, other.messageDelimiter) &&
               Arrays.equals(ackPattern, other.ackPattern) &&
               Objects.equals(destinationConnectorProperties, other.destinationConnectorProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portConfig, transmissionMode, messageDelimiter, waitForAckAfterWrite,
                ackTimeout, keepConnectionOpen, Arrays.hashCode(ackPattern), destinationConnectorProperties);
    }

    @Override
    public DestinationConnectorProperties getDestinationConnectorProperties() {
        return destinationConnectorProperties;
    }

    @Override
    public boolean canValidateResponse() {
        return false;
    }

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
