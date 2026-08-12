package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;
import java.util.Objects;

public class SerialPortConfig implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String portName = "COM1";
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = 1;
    private int parity = 0;
    private int flowControl = 0;
    private String charset = "UTF-8";
    private boolean binaryMode = false;
    private int readTimeout = 1000;
    private int writeTimeout = 1000;
    private int bufferSize = 4096;
    private boolean setDtr = true;
    private boolean setRts = true;
    private boolean waitCts = false;
    private boolean waitDsr = false;
    private boolean waitDcd = false;
    private int signalTimeout = 1000;
    private boolean sendBreak = false;
    private int breakDuration = 100;
    private boolean flushOnOpen = true;
    private boolean flushOnClose = true;
    private boolean autoDetectPort = false;
    private boolean autoDetectBaud = false;

    // Health monitor
    private boolean healthMonitorEnabled = true;
    private int healthInterval = 5000;
    private int maxReconnects = 10;
    private int reconnectDelay = 5000;

    // Protocol analyzer
    private boolean protocolLoggingEnabled = false;
    private int maxLogEntries = 1000;

    // Transmission Mode (like TCP Listener)
    private String transmissionMode = "RAW"; // RAW, LINE, FRAME, MLLP, ASTM
    private String lineDelimiter = "\\r\\n"; // for LINE mode: \r\n, \n, \r, or custom
    private String frameStartBytes = ""; // hex string for FRAME mode
    private String frameEndBytes = ""; // hex string for FRAME mode

    // Getters and Setters
    public String getPortName() { return portName; }
    public void setPortName(String portName) { this.portName = portName; }
    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }
    public int getDataBits() { return dataBits; }
    public void setDataBits(int dataBits) { this.dataBits = dataBits; }
    public int getStopBits() { return stopBits; }
    public void setStopBits(int stopBits) { this.stopBits = stopBits; }
    public int getParity() { return parity; }
    public void setParity(int parity) { this.parity = parity; }
    public int getFlowControl() { return flowControl; }
    public void setFlowControl(int flowControl) { this.flowControl = flowControl; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
    public boolean isBinaryMode() { return binaryMode; }
    public void setBinaryMode(boolean binaryMode) { this.binaryMode = binaryMode; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public int getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }
    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    public boolean isSetDtr() { return setDtr; }
    public void setSetDtr(boolean setDtr) { this.setDtr = setDtr; }
    public boolean isSetRts() { return setRts; }
    public void setSetRts(boolean setRts) { this.setRts = setRts; }
    public boolean isWaitCts() { return waitCts; }
    public void setWaitCts(boolean waitCts) { this.waitCts = waitCts; }
    public boolean isWaitDsr() { return waitDsr; }
    public void setWaitDsr(boolean waitDsr) { this.waitDsr = waitDsr; }
    public boolean isWaitDcd() { return waitDcd; }
    public void setWaitDcd(boolean waitDcd) { this.waitDcd = waitDcd; }
    public int getSignalTimeout() { return signalTimeout; }
    public void setSignalTimeout(int signalTimeout) { this.signalTimeout = signalTimeout; }
    public boolean isSendBreak() { return sendBreak; }
    public void setSendBreak(boolean sendBreak) { this.sendBreak = sendBreak; }
    public int getBreakDuration() { return breakDuration; }
    public void setBreakDuration(int breakDuration) { this.breakDuration = breakDuration; }
    public boolean isFlushOnOpen() { return flushOnOpen; }
    public void setFlushOnOpen(boolean flushOnOpen) { this.flushOnOpen = flushOnOpen; }
    public boolean isFlushOnClose() { return flushOnClose; }
    public void setFlushOnClose(boolean flushOnClose) { this.flushOnClose = flushOnClose; }
    public boolean isAutoDetectPort() { return autoDetectPort; }
    public void setAutoDetectPort(boolean autoDetectPort) { this.autoDetectPort = autoDetectPort; }
    public boolean isAutoDetectBaud() { return autoDetectBaud; }
    public void setAutoDetectBaud(boolean autoDetectBaud) { this.autoDetectBaud = autoDetectBaud; }

    public boolean isHealthMonitorEnabled() { return healthMonitorEnabled; }
    public void setHealthMonitorEnabled(boolean healthMonitorEnabled) { this.healthMonitorEnabled = healthMonitorEnabled; }
    public int getHealthInterval() { return healthInterval; }
    public void setHealthInterval(int healthInterval) { this.healthInterval = healthInterval; }
    public int getMaxReconnects() { return maxReconnects; }
    public void setMaxReconnects(int maxReconnects) { this.maxReconnects = maxReconnects; }
    public int getReconnectDelay() { return reconnectDelay; }
    public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
    public boolean isProtocolLoggingEnabled() { return protocolLoggingEnabled; }
    public void setProtocolLoggingEnabled(boolean protocolLoggingEnabled) { this.protocolLoggingEnabled = protocolLoggingEnabled; }
    public int getMaxLogEntries() { return maxLogEntries; }
    public void setMaxLogEntries(int maxLogEntries) { this.maxLogEntries = maxLogEntries; }

    public String getTransmissionMode() { return transmissionMode; }
    public void setTransmissionMode(String transmissionMode) { this.transmissionMode = transmissionMode; }
    public String getLineDelimiter() { return lineDelimiter; }
    public void setLineDelimiter(String lineDelimiter) { this.lineDelimiter = lineDelimiter; }
    public String getFrameStartBytes() { return frameStartBytes; }
    public void setFrameStartBytes(String frameStartBytes) { this.frameStartBytes = frameStartBytes; }
    public String getFrameEndBytes() { return frameEndBytes; }
    public void setFrameEndBytes(String frameEndBytes) { this.frameEndBytes = frameEndBytes; }

    public int[] getAutoBaudRates() {
        return new int[]{9600, 19200, 38400, 57600, 115200};
    }

    @Override
    public SerialPortConfig clone() {
        try {
            return (SerialPortConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerialPortConfig that = (SerialPortConfig) o;
        return baudRate == that.baudRate && dataBits == that.dataBits && stopBits == that.stopBits
                && parity == that.parity && flowControl == that.flowControl && binaryMode == that.binaryMode
                && readTimeout == that.readTimeout && writeTimeout == that.writeTimeout && bufferSize == that.bufferSize
                && setDtr == that.setDtr && setRts == that.setRts && waitCts == that.waitCts
                && waitDsr == that.waitDsr && waitDcd == that.waitDcd && signalTimeout == that.signalTimeout
                && sendBreak == that.sendBreak && breakDuration == that.breakDuration
                && flushOnOpen == that.flushOnOpen && flushOnClose == that.flushOnClose
                && autoDetectPort == that.autoDetectPort && autoDetectBaud == that.autoDetectBaud
                && healthMonitorEnabled == that.healthMonitorEnabled && healthInterval == that.healthInterval
                && maxReconnects == that.maxReconnects && reconnectDelay == that.reconnectDelay
                && protocolLoggingEnabled == that.protocolLoggingEnabled && maxLogEntries == that.maxLogEntries
                && Objects.equals(portName, that.portName) && Objects.equals(charset, that.charset)
                && Objects.equals(transmissionMode, that.transmissionMode)
                && Objects.equals(lineDelimiter, that.lineDelimiter)
                && Objects.equals(frameStartBytes, that.frameStartBytes)
                && Objects.equals(frameEndBytes, that.frameEndBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portName, baudRate, dataBits, stopBits, parity, flowControl, charset,
                binaryMode, readTimeout, writeTimeout, bufferSize, setDtr, setRts, waitCts, waitDsr,
                waitDcd, signalTimeout, sendBreak, breakDuration, flushOnOpen, flushOnClose,
                autoDetectPort, autoDetectBaud, healthMonitorEnabled, healthInterval, maxReconnects,
                reconnectDelay, protocolLoggingEnabled, maxLogEntries, transmissionMode,
                lineDelimiter, frameStartBytes, frameEndBytes);
    }
}