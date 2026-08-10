/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 * Package: com.bitdreamit.mirth.labextensions
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.Serializable;

/**
 * Comprehensive serial port configuration.
 * Covers all features of commercial Serial Connector + extras.
 */
public class SerialPortConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    // Basic settings
    private String portName = "COM1";
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = 1;   // 1=ONE, 2=ONE_POINT_FIVE, 3=TWO
    private int parity = 0;     // 0=NONE, 1=ODD, 2=EVEN, 3=MARK, 4=SPACE
    private int flowControl = 0; // 0=NONE, 1=RTS_CTS, 2=XON_XOFF, 3=DSR_DTR

    // Advanced settings (beyond commercial)
    private boolean autoDetectPort = false;
    private boolean autoDetectBaud = false;
    private int[] autoBaudRates = {9600, 19200, 38400, 57600, 115200};
    private boolean binaryMode = false;
    private String charsetEncoding = "UTF-8";
    private int readTimeout = 5000;
    private int writeTimeout = 5000;
    private int bufferSize = 65536;

    // Signal control (beyond commercial)
    private boolean setDTR = true;
    private boolean setRTS = true;
    private boolean waitForCTS = false;
    private boolean waitForDSR = false;
    private boolean waitForDCD = false;
    private int signalWaitTimeout = 1000;

    // Break & flush
    private boolean sendBreakBeforeOpen = false;
    private int breakDuration = 100;
    private boolean flushBuffersOnOpen = true;
    private boolean flushBuffersOnClose = true;

    // Health monitoring
    private boolean enableHealthMonitor = true;
    private int healthCheckInterval = 30000;
    private int maxReconnectAttempts = 10;
    private int reconnectDelay = 5000;

    // Protocol analyzer
    private boolean enableProtocolAnalyzer = false;
    private int maxProtocolLogEntries = 1000;

    // Getters & Setters
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
    public boolean isAutoDetectPort() { return autoDetectPort; }
    public void setAutoDetectPort(boolean autoDetectPort) { this.autoDetectPort = autoDetectPort; }
    public boolean isAutoDetectBaud() { return autoDetectBaud; }
    public void setAutoDetectBaud(boolean autoDetectBaud) { this.autoDetectBaud = autoDetectBaud; }
    public int[] getAutoBaudRates() { return autoBaudRates; }
    public void setAutoBaudRates(int[] autoBaudRates) { this.autoBaudRates = autoBaudRates; }
    public boolean isBinaryMode() { return binaryMode; }
    public void setBinaryMode(boolean binaryMode) { this.binaryMode = binaryMode; }
    public String getCharsetEncoding() { return charsetEncoding; }
    public void setCharsetEncoding(String charsetEncoding) { this.charsetEncoding = charsetEncoding; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public int getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }
    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    public boolean isSetDTR() { return setDTR; }
    public void setSetDTR(boolean setDTR) { this.setDTR = setDTR; }
    public boolean isSetRTS() { return setRTS; }
    public void setSetRTS(boolean setRTS) { this.setRTS = setRTS; }
    public boolean isWaitForCTS() { return waitForCTS; }
    public void setWaitForCTS(boolean waitForCTS) { this.waitForCTS = waitForCTS; }
    public boolean isWaitForDSR() { return waitForDSR; }
    public void setWaitForDSR(boolean waitForDSR) { this.waitForDSR = waitForDSR; }
    public boolean isWaitForDCD() { return waitForDCD; }
    public void setWaitForDCD(boolean waitForDCD) { this.waitForDCD = waitForDCD; }
    public int getSignalWaitTimeout() { return signalWaitTimeout; }
    public void setSignalWaitTimeout(int signalWaitTimeout) { this.signalWaitTimeout = signalWaitTimeout; }
    public boolean isSendBreakBeforeOpen() { return sendBreakBeforeOpen; }
    public void setSendBreakBeforeOpen(boolean sendBreakBeforeOpen) { this.sendBreakBeforeOpen = sendBreakBeforeOpen; }
    public int getBreakDuration() { return breakDuration; }
    public void setBreakDuration(int breakDuration) { this.breakDuration = breakDuration; }
    public boolean isFlushBuffersOnOpen() { return flushBuffersOnOpen; }
    public void setFlushBuffersOnOpen(boolean flushBuffersOnOpen) { this.flushBuffersOnOpen = flushBuffersOnOpen; }
    public boolean isFlushBuffersOnClose() { return flushBuffersOnClose; }
    public void setFlushBuffersOnClose(boolean flushBuffersOnClose) { this.flushBuffersOnClose = flushBuffersOnClose; }
    public boolean isEnableHealthMonitor() { return enableHealthMonitor; }
    public void setEnableHealthMonitor(boolean enableHealthMonitor) { this.enableHealthMonitor = enableHealthMonitor; }
    public int getHealthCheckInterval() { return healthCheckInterval; }
    public void setHealthCheckInterval(int healthCheckInterval) { this.healthCheckInterval = healthCheckInterval; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
    public int getReconnectDelay() { return reconnectDelay; }
    public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
    public boolean isEnableProtocolAnalyzer() { return enableProtocolAnalyzer; }
    public void setEnableProtocolAnalyzer(boolean enableProtocolAnalyzer) { this.enableProtocolAnalyzer = enableProtocolAnalyzer; }
    public int getMaxProtocolLogEntries() { return maxProtocolLogEntries; }
    public void setMaxProtocolLogEntries(int maxProtocolLogEntries) { this.maxProtocolLogEntries = maxProtocolLogEntries; }
}