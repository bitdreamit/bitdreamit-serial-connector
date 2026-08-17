package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Objects;

/**
 * Serial Source Properties.
 *
 * Uses Mirth's TransmissionModeProperties framework — same as TCP.
 * Modes are loaded DYNAMICALLY from Mirth's extension system.
 *
 * CRITICAL: This class MUST exist ONLY in serial-shared.jar.
 */
@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements SourceConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private SourceConnectorProperties sourceConnectorProperties = new SourceConnectorProperties();

    // DYNAMIC: Stores the transmission mode properties (MLLP, ASTM, Frame, etc.)
    // This is polymorphic — the actual subclass depends on which mode was selected.
    // Loaded dynamically from Mirth's TransmissionModePlugin registry.
    private TransmissionModeProperties transmissionModeProperties;

    public SerialReceiverProperties() {
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
    public SourceConnectorProperties getSourceConnectorProperties() {
        if (sourceConnectorProperties == null) {
            sourceConnectorProperties = new SourceConnectorProperties();
        }
        return sourceConnectorProperties;
    }

    public void setSourceConnectorProperties(SourceConnectorProperties sourceConnectorProperties) {
        this.sourceConnectorProperties = sourceConnectorProperties;
    }

    // DYNAMIC: Transmission mode properties (polymorphic — stores MLLP, ASTM, Frame, etc.)
    public TransmissionModeProperties getTransmissionModeProperties() {
        return transmissionModeProperties;
    }

    public void setTransmissionModeProperties(TransmissionModeProperties transmissionModeProperties) {
        this.transmissionModeProperties = transmissionModeProperties;
    }

    @Override
    public String getProtocol() {
        return "serial";
    }

    @Override
    public String getName() {
        return "Serial Reader";
    }

    @Override
    public String toFormattedString() {
        SerialPortConfig c = getPortConfig();
        return "Port: " + c.getPortName()
                + ", Baud: " + c.getBaudRate()
                + ", Mode: " + c.getTransmissionMode();
    }

    @Override
    public boolean canBatch() {
        return false;
    }

    @Override
    public SerialReceiverProperties clone() {
        try {
            SerialReceiverProperties copy = (SerialReceiverProperties) super.clone();
            copy.portConfig = (this.portConfig != null) ? this.portConfig.clone() : new SerialPortConfig();
            copy.sourceConnectorProperties = this.sourceConnectorProperties;
            copy.transmissionModeProperties = this.transmissionModeProperties;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialReceiverProperties)) return false;
        SerialReceiverProperties that = (SerialReceiverProperties) o;
        return Objects.equals(getPortConfig(), that.getPortConfig())
                && Objects.equals(getSourceConnectorProperties(), that.getSourceConnectorProperties())
                && Objects.equals(getTransmissionModeProperties(), that.getTransmissionModeProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPortConfig(), getSourceConnectorProperties(),
                getTransmissionModeProperties());
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {
    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {
    }
}
