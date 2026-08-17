package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Objects;

/**
 * Serial Source Properties.
 *
 * CRITICAL: This class MUST exist ONLY in serial-shared.jar.
 * Do NOT duplicate it in serial-server or serial-client.
 *
 * Implements SourceConnectorPropertiesInterface — REQUIRED by Mirth for source connectors.
 * Without this interface, channel enable throws ClassCastException silently.
 */
@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements SourceConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private SourceConnectorProperties sourceConnectorProperties = new SourceConnectorProperties();

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
            // Shallow copy of sourceConnectorProperties — its clone() is protected
            // in some Mirth SDK versions and cannot be called directly.
            // This is safe: Mirth re-populates sourceConnectorProperties from the
            // channel XML on deployment, so the shared reference is overwritten.
            copy.sourceConnectorProperties = this.sourceConnectorProperties;
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
                && Objects.equals(getSourceConnectorProperties(), that.getSourceConnectorProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPortConfig(), getSourceConnectorProperties());
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {
    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {
    }
}
