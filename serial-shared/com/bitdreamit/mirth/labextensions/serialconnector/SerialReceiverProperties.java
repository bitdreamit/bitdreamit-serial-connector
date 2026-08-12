package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Objects;

/**
 * Serial Source Properties.
 * transmissionMode: BASIC | MLLP | ASTM_E1381
 * Works with ANY Mirth DataType (HL7, ASTM, Delimited, XML, etc.)
 */
@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements SourceConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private SourceConnectorProperties sourceConnectorProperties;

    // Transmission / Framing (transport-level, NOT message parsing)
    private String transmissionMode = "BASIC";   // BASIC | MLLP | ASTM_E1381
    private String messageDelimiter = "\\r\\n";   // For BASIC mode
    private boolean keepConnectionOpen = false;  // For source, usually false

    public SerialReceiverProperties() {
        this.sourceConnectorProperties = new SourceConnectorProperties();
    }

    public SerialPortConfig getPortConfig() { return portConfig; }
    public void setPortConfig(SerialPortConfig portConfig) { this.portConfig = portConfig; }

    public String getTransmissionMode() { return transmissionMode; }
    public void setTransmissionMode(String transmissionMode) { this.transmissionMode = transmissionMode; }
    public String getMessageDelimiter() { return messageDelimiter; }
    public void setMessageDelimiter(String messageDelimiter) { this.messageDelimiter = messageDelimiter; }
    public boolean isKeepConnectionOpen() { return keepConnectionOpen; }
    public void setKeepConnectionOpen(boolean keepConnectionOpen) { this.keepConnectionOpen = keepConnectionOpen; }

    @Override public String getProtocol() { return "Serial"; }
    @Override public String getName() { return "Serial Reader"; }
    @Override public String toFormattedString() {
        return "Serial [" + portConfig.getPortName() + " @ " + portConfig.getBaudRate() +
               ", mode=" + transmissionMode + "]";
    }

    @Override
    public ConnectorProperties clone() {
        SerialReceiverProperties copy = new SerialReceiverProperties();
        copy.portConfig = this.portConfig != null ? this.portConfig.clone() : new SerialPortConfig();
        copy.transmissionMode = this.transmissionMode;
        copy.messageDelimiter = this.messageDelimiter;
        copy.keepConnectionOpen = this.keepConnectionOpen;
        copy.sourceConnectorProperties = this.sourceConnectorProperties;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SerialReceiverProperties other = (SerialReceiverProperties) obj;
        return keepConnectionOpen == other.keepConnectionOpen &&
               Objects.equals(portConfig, other.portConfig) &&
               Objects.equals(transmissionMode, other.transmissionMode) &&
               Objects.equals(messageDelimiter, other.messageDelimiter) &&
               Objects.equals(sourceConnectorProperties, other.sourceConnectorProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portConfig, transmissionMode, messageDelimiter, keepConnectionOpen, sourceConnectorProperties);
    }

    @Override
    public SourceConnectorProperties getSourceConnectorProperties() {
        return sourceConnectorProperties;
    }

    @Override
    public boolean canBatch() {
        return false;
    }

    public void setSourceConnectorProperties(SourceConnectorProperties sourceConnectorProperties) {
        this.sourceConnectorProperties = sourceConnectorProperties;
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {

    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {

    }
}
