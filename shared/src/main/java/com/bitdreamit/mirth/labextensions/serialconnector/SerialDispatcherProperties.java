/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.QueueConnectorProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("serialDispatcherProperties")
public class SerialDispatcherProperties extends ConnectorProperties implements DestinationConnectorProperties {
    private static final long serialVersionUID = 1L;

    private SerialPortConfig portConfig = new SerialPortConfig();
    private boolean waitForAckAfterWrite = false;
    private int ackTimeout = 1000;
    private byte[] ackPattern = new byte[]{0x06}; // Default ACK
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

    // DestinationConnectorProperties delegation
    @Override public DestinationConnectorProperties getDestinationConnectorProperties() { return destinationConnectorProperties; }
    @Override public void setDestinationConnectorProperties(DestinationConnectorProperties v) { this.destinationConnectorProperties = v; }
    @Override public boolean isQueueEnabled() { return destinationConnectorProperties.isQueueEnabled(); }
    @Override public void setQueueEnabled(boolean v) { destinationConnectorProperties.setQueueEnabled(v); }
    @Override public boolean isSendFirst() { return destinationConnectorProperties.isSendFirst(); }
    @Override public void setSendFirst(boolean v) { destinationConnectorProperties.setSendFirst(v); }
    @Override public int getRetryIntervalMillis() { return destinationConnectorProperties.getRetryIntervalMillis(); }
    @Override public void setRetryIntervalMillis(int v) { destinationConnectorProperties.setRetryIntervalMillis(v); }
    @Override public boolean isRegenerateTemplate() { return destinationConnectorProperties.isRegenerateTemplate(); }
    @Override public void setRegenerateTemplate(boolean v) { destinationConnectorProperties.setRegenerateTemplate(v); }
    @Override public int getRetryCount() { return destinationConnectorProperties.getRetryCount(); }
    @Override public void setRetryCount(int v) { destinationConnectorProperties.setRetryCount(v); }
    @Override public boolean isRotate() { return destinationConnectorProperties.isRotate(); }
    @Override public void setRotate(boolean v) { destinationConnectorProperties.setRotate(v); }
    @Override public boolean isIncludeFilterTransformer() { return destinationConnectorProperties.isIncludeFilterTransformer(); }
    @Override public void setIncludeFilterTransformer(boolean v) { destinationConnectorProperties.setIncludeFilterTransformer(v); }
    @Override public int getThreadCount() { return destinationConnectorProperties.getThreadCount(); }
    @Override public void setThreadCount(int v) { destinationConnectorProperties.setThreadCount(v); }
    @Override public String getThreadAssignmentVariable() { return destinationConnectorProperties.getThreadAssignmentVariable(); }
    @Override public void setThreadAssignmentVariable(String v) { destinationConnectorProperties.setThreadAssignmentVariable(v); }
    @Override public boolean isValidateResponse() { return destinationConnectorProperties.isValidateResponse(); }
    @Override public void setValidateResponse(boolean v) { destinationConnectorProperties.setValidateResponse(v); }
    @Override public QueueConnectorProperties getQueueConnectorProperties() { return destinationConnectorProperties.getQueueConnectorProperties(); }
    @Override public void setQueueConnectorProperties(QueueConnectorProperties v) { destinationConnectorProperties.setQueueConnectorProperties(v); }
    @Override public boolean isReattachAttachments() { return destinationConnectorProperties.isReattachAttachments(); }
    @Override public void setReattachAttachments(boolean v) { destinationConnectorProperties.setReattachAttachments(v); }
}