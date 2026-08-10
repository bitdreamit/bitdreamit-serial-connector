/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.ResponseSelectorProperties;
import com.mirth.connect.donkey.model.channel.QueueConnectorProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("serialReceiverProperties")
public class SerialReceiverProperties extends ConnectorProperties implements SourceConnectorProperties {
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

    // SourceConnectorProperties delegation
    @Override public ResponseSelectorProperties getResponseSelectorProperties() { return sourceConnectorProperties.getResponseSelectorProperties(); }
    @Override public void setResponseSelectorProperties(ResponseSelectorProperties rsp) { sourceConnectorProperties.setResponseSelectorProperties(rsp); }
    @Override public boolean isRespondAfterProcessing() { return sourceConnectorProperties.isRespondAfterProcessing(); }
    @Override public void setRespondAfterProcessing(boolean v) { sourceConnectorProperties.setRespondAfterProcessing(v); }
    @Override public boolean isProcessBatch() { return sourceConnectorProperties.isProcessBatch(); }
    @Override public void setProcessBatch(boolean v) { sourceConnectorProperties.setProcessBatch(v); }
    @Override public boolean isFirstResponse() { return sourceConnectorProperties.isFirstResponse(); }
    @Override public void setFirstResponse(boolean v) { sourceConnectorProperties.setFirstResponse(v); }
    @Override public int getProcessingThreads() { return sourceConnectorProperties.getProcessingThreads(); }
    @Override public void setProcessingThreads(int v) { sourceConnectorProperties.setProcessingThreads(v); }
    @Override public QueueConnectorProperties getQueueConnectorProperties() { return sourceConnectorProperties.getQueueConnectorProperties(); }
    @Override public void setQueueConnectorProperties(QueueConnectorProperties v) { sourceConnectorProperties.setQueueConnectorProperties(v); }
}