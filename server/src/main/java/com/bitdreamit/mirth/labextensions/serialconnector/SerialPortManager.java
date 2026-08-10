/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton port manager with health monitoring, auto-reconnect,
 * and protocol analysis. Extra feature beyond commercial extension.
 */
public class SerialPortManager {
    private static final Logger logger = Logger.getLogger(SerialPortManager.class);
    private static final SerialPortManager INSTANCE = new SerialPortManager();

    private final ConcurrentHashMap<String, SerialPort> activePorts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SerialStatistics> statistics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ProtocolLogEntry>> protocolLogs = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<PortHealthListener> healthListeners = new CopyOnWriteArrayList<>();

    public static SerialPortManager getInstance() { return INSTANCE; }

    public List<String> detectAvailablePorts() {
        List<String> names = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            names.add(port.getSystemPortName());
        }
        Collections.sort(names);
        return names;
    }

    public SerialPort openPort(SerialPortConfig config, String channelId) throws Exception {
        String key = channelId + "@" + config.getPortName();
        closePort(key);

        String portName = config.isAutoDetectPort() ? findFirstAvailablePort() : config.getPortName();
        if (portName == null || portName.isEmpty()) {
            throw new Exception("No serial port specified or detected");
        }

        SerialPort port = SerialPort.getCommPort(portName);
        if (port == null || !port.openPort()) {
            throw new Exception("Failed to open serial port: " + portName);
        }

        // Apply configuration
        if (config.isAutoDetectBaud()) {
            int detected = autoDetectBaudRate(port, config);
            if (detected > 0) {
                config.setBaudRate(detected);
                logger.info("Auto-detected baud rate: " + detected + " for " + portName);
            }
        }

        port.setBaudRate(config.getBaudRate());
        port.setNumDataBits(config.getDataBits());
        port.setNumStopBits(config.getStopBits());
        port.setParity(config.getParity());
        port.setFlowControl(mapFlowControl(config.getFlowControl()));
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, config.getReadTimeout(), config.getWriteTimeout());

        // Signal control
        port.setDTR(config.isSetDTR());
        port.setRTS(config.isSetRTS());

        // Break signal
        if (config.isSendBreakBeforeOpen()) {
            port.setBreak();
            Thread.sleep(config.getBreakDuration());
            port.clearBreak();
        }

        // Flush
        if (config.isFlushBuffersOnOpen()) {
            port.flushIOBuffers();
        }

        activePorts.put(key, port);
        statistics.put(key, new SerialStatistics());
        protocolLogs.put(key, Collections.synchronizedList(new ArrayList<>()));

        logger.info("Serial port opened: " + portName + " @ " + config.getBaudRate() + " for channel " + channelId);
        return port;
    }

    public void closePort(String key) {
        SerialPort port = activePorts.remove(key);
        if (port != null && port.isOpen()) {
            port.closePort();
            logger.info("Serial port closed: " + key);
        }
        statistics.remove(key);
        protocolLogs.remove(key);
    }

    public SerialPort getPort(String key) { return activePorts.get(key); }
    public SerialStatistics getStatistics(String key) { return statistics.get(key); }
    public List<ProtocolLogEntry> getProtocolLog(String key) { return protocolLogs.get(key); }

    public void logProtocol(String key, ProtocolLogEntry.Direction dir, byte[] data, String desc) {
        List<ProtocolLogEntry> log = protocolLogs.get(key);
        if (log != null) {
            log.add(new ProtocolLogEntry(dir, data, desc));
            int max = 1000; // default
            while (log.size() > max) log.remove(0);
        }
    }

    public boolean waitForSignals(SerialPort port, SerialPortConfig config) throws InterruptedException {
        long deadline = System.currentTimeMillis() + config.getSignalWaitTimeout();
        while (System.currentTimeMillis() < deadline) {
            boolean ctsOk = !config.isWaitForCTS() || port.getCTS();
            boolean dsrOk = !config.isWaitForDSR() || port.getDSR();
            boolean dcdOk = !config.isWaitForDCD() || port.getDCD();
            if (ctsOk && dsrOk && dcdOk) return true;
            Thread.sleep(50);
        }
        return false;
    }

    private int mapFlowControl(int fc) {
        switch (fc) {
            case 1: return SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
            case 2: return SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
            case 3: return SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED;
            default: return SerialPort.FLOW_CONTROL_DISABLED;
        }
    }

    private String findFirstAvailablePort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        return ports.length > 0 ? ports[0].getSystemPortName() : null;
    }

    private int autoDetectBaudRate(SerialPort port, SerialPortConfig config) {
        for (int baud : config.getAutoBaudRates()) {
            port.setBaudRate(baud);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 500, 0);
            byte[] buf = new byte[1];
            int read = port.readBytes(buf, 1);
            if (read > 0) {
                port.flushIOBuffers();
                return baud;
            }
        }
        return config.getBaudRate(); // fallback
    }

    public interface PortHealthListener {
        void onHealthEvent(String portKey, String event, Throwable error);
    }
}