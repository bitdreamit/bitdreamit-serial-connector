package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.Objects;

@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig;

    public SerialReceiverProperties() {
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
    public SerialReceiverProperties clone() {
        try {
            SerialReceiverProperties copy = (SerialReceiverProperties) super.clone();
            copy.portConfig = (this.portConfig != null) ? this.portConfig.clone() : new SerialPortConfig();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialReceiverProperties)) return false;
        SerialReceiverProperties that = (SerialReceiverProperties) o;
        return Objects.equals(getPortConfig(), that.getPortConfig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPortConfig());
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {

    }

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {

    }
}