/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Bit Dream IT — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

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
        return "Serial [" + portConfig.getPortName() + " @ " + portConfig.getBaudRate() +
                " baud, Health=" + portConfig.isEnableHealthMonitor() + "]";
    }

    @Override
    public ConnectorProperties clone() {
        SerialReceiverProperties copy = new SerialReceiverProperties();
        copy.portConfig = this.portConfig;
        copy.sourceConnectorProperties = this.sourceConnectorProperties;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SerialReceiverProperties other = (SerialReceiverProperties) obj;
        return java.util.Objects.equals(portConfig, other.portConfig)
                && java.util.Objects.equals(sourceConnectorProperties, other.sourceConnectorProperties);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(portConfig, sourceConnectorProperties);
    }

    // Only getter is @Override — setter is NOT part of the interface in Mirth 4.x
    @Override
    public SourceConnectorProperties getSourceConnectorProperties() {
        return sourceConnectorProperties;
    }

    @Override
    public boolean canBatch() {
        return false;
    }

    // Regular method, NOT @Override
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