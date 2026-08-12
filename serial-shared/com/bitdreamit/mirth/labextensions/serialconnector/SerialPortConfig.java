package com.bitdreamit.mirth.labextensions.serialconnector;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Pure serial port configuration. NO protocol/transmission logic here.
 * Protocol handling lives in the connector properties (transmissionMode).
 */
@XStreamAlias("serialPortConfig")
public class SerialPortConfig implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    // Basic
    private String portName = "";
    private boolean autoDetectPort = false;
    private int baudRate = 9600;
    private boolean autoDetectBaud = false;
    private int[] autoBaudRates = new int[]{9600, 19200, 38400, 57600, 115200};
    private int dataBits = 8;
    private int stopBits = 1;
    private int parity = 0;
    private int flowControl = 0;
    private String charsetEncoding = "UTF-8";
    private boolean binaryMode = false;

    // Timeouts & Buffers
    private int readTimeout = 1000;
    private int writeTimeout = 1000;
    private int bufferSize = 4096;

    // Signals
    private boolean setDTR = true;
    private boolean setRTS = true;
    private boolean waitForCTS = false;
    private boolean waitForDSR = false;
    private boolean waitForDCD = false;
    private int signalWaitTimeout = 1000;

    // Break & Flush
    private boolean sendBreakBeforeOpen = false;
    private int breakDuration = 100;
    private boolean flushBuffersOnOpen = true;
    private boolean flushBuffersOnClose = true;

    // Health Monitor
    private boolean enableHealthMonitor = true;
    private int healthCheckInterval = 30000;
    private int maxReconnectAttempts = 10;
    private int reconnectDelay = 5000;

    // Protocol Analyzer
    private boolean enableProtocolAnalyzer = false;
    private int maxProtocolLogEntries = 1000;

    // --- Getters & Setters ---
    public String getPortName() { return portName; }
    public void setPortName(String portName) { this.portName = portName; }
    public boolean isAutoDetectPort() { return autoDetectPort; }
    public void setAutoDetectPort(boolean autoDetectPort) { this.autoDetectPort = autoDetectPort; }
    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }
    public boolean isAutoDetectBaud() { return autoDetectBaud; }
    public void setAutoDetectBaud(boolean autoDetectBaud) { this.autoDetectBaud = autoDetectBaud; }
    public int[] getAutoBaudRates() { return autoBaudRates; }
    public void setAutoBaudRates(int[] autoBaudRates) { this.autoBaudRates = autoBaudRates; }
    public int getDataBits() { return dataBits; }
    public void setDataBits(int dataBits) { this.dataBits = dataBits; }
    public int getStopBits() { return stopBits; }
    public void setStopBits(int stopBits) { this.stopBits = stopBits; }
    public int getParity() { return parity; }
    public void setParity(int parity) { this.parity = parity; }
    public int getFlowControl() { return flowControl; }
    public void setFlowControl(int flowControl) { this.flowControl = flowControl; }
    public String getCharsetEncoding() { return charsetEncoding; }
    public void setCharsetEncoding(String charsetEncoding) { this.charsetEncoding = charsetEncoding; }
    public boolean isBinaryMode() { return binaryMode; }
    public void setBinaryMode(boolean binaryMode) { this.binaryMode = binaryMode; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialPortConfig)) return false;
        SerialPortConfig that = (SerialPortConfig) o;
        return autoDetectPort == that.autoDetectPort && baudRate == that.baudRate &&
               autoDetectBaud == that.autoDetectBaud && dataBits == that.dataBits &&
               stopBits == that.stopBits && parity == that.parity && flowControl == that.flowControl &&
               binaryMode == that.binaryMode && readTimeout == that.readTimeout &&
               writeTimeout == that.writeTimeout && bufferSize == that.bufferSize &&
               setDTR == that.setDTR && setRTS == that.setRTS && waitForCTS == that.waitForCTS &&
               waitForDSR == that.waitForDSR && waitForDCD == that.waitForDCD &&
               signalWaitTimeout == that.signalWaitTimeout && sendBreakBeforeOpen == that.sendBreakBeforeOpen &&
               breakDuration == that.breakDuration && flushBuffersOnOpen == that.flushBuffersOnOpen &&
               flushBuffersOnClose == that.flushBuffersOnClose && enableHealthMonitor == that.enableHealthMonitor &&
               healthCheckInterval == that.healthCheckInterval && maxReconnectAttempts == that.maxReconnectAttempts &&
               reconnectDelay == that.reconnectDelay && enableProtocolAnalyzer == that.enableProtocolAnalyzer &&
               maxProtocolLogEntries == that.maxProtocolLogEntries &&
               Objects.equals(portName, that.portName) &&
               Objects.equals(charsetEncoding, that.charsetEncoding) &&
               Arrays.equals(autoBaudRates, that.autoBaudRates);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(portName, autoDetectPort, baudRate, autoDetectBaud, dataBits, stopBits, parity,
                flowControl, charsetEncoding, binaryMode, readTimeout, writeTimeout, bufferSize, setDTR, setRTS,
                waitForCTS, waitForDSR, waitForDCD, signalWaitTimeout, sendBreakBeforeOpen, breakDuration,
                flushBuffersOnOpen, flushBuffersOnClose, enableHealthMonitor, healthCheckInterval,
                maxReconnectAttempts, reconnectDelay, enableProtocolAnalyzer, maxProtocolLogEntries);
        result = 31 * result + Arrays.hashCode(autoBaudRates);
        return result;
    }

    @Override
    public SerialPortConfig clone() {
        try {
            SerialPortConfig copy = (SerialPortConfig) super.clone();
            if (this.autoBaudRates != null) copy.autoBaudRates = this.autoBaudRates.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
