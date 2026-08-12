package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Objects;

/**
 * Serial Source Properties — Pure transport. No protocol awareness.
 * Protocol framing is handled by the channel's DataType plugin.
 */
@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements SourceConnectorPropertiesInterface {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private SourceConnectorProperties sourceConnectorProperties;

    public SerialReceiverProperties() {
        this.sourceConnectorProperties = new SourceConnectorProperties();
    }

    public SerialPortConfig getPortConfig() { return portConfig; }
    public void setPortConfig(SerialPortConfig portConfig) { this.portConfig = portConfig; }

    @Override public String getProtocol() { return "Serial"; }
    @Override public String getName() { return "Serial Reader"; }
    @Override public String toFormattedString() {
        return "Serial [" + portConfig.getPortName() + " @ " + portConfig.getBaudRate() + " baud]";
    }

    @Override
    public ConnectorProperties clone() {
        SerialReceiverProperties copy = new SerialReceiverProperties();
        copy.portConfig = this.portConfig != null ? this.portConfig.clone() : new SerialPortConfig();
        copy.sourceConnectorProperties = this.sourceConnectorProperties;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SerialReceiverProperties other = (SerialReceiverProperties) obj;
        return Objects.equals(portConfig, other.portConfig)
            && Objects.equals(sourceConnectorProperties, other.sourceConnectorProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portConfig, sourceConnectorProperties);
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
