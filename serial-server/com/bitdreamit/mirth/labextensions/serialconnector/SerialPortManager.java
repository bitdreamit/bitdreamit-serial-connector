package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SerialPortManager {
    private static final Logger logger = Logger.getLogger(SerialPortManager.class);
    private static final SerialPortManager INSTANCE = new SerialPortManager();

    private final ConcurrentHashMap<String, SerialPort> activePorts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SerialStatistics> statistics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ProtocolLogEntry>> protocolLogs = new ConcurrentHashMap<>();

    public static SerialPortManager getInstance() { return INSTANCE; }

    public List<String> detectAvailablePorts() {
        List<String> names = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            names.add(port.getSystemPortName());
        }
        Collections.sort(names);
        return names;
    }

    /**
     * True connection pooling: returns existing open port or opens a new one.
     * Thread-safe per physical port via key.intern() locking.
     */
    public SerialPort getOrOpenPort(SerialPortConfig config, String channelId) throws Exception {
        String key = channelId + "@" + config.getPortName();
        SerialPort port = activePorts.get(key);
        if (port != null && port.isOpen()) {
            logger.debug("Reusing pooled port: " + key);
            return port;
        }
        return openPort(config, channelId);
    }

    public SerialPort openPort(SerialPortConfig config, String channelId) throws Exception {
        String key = channelId + "@" + config.getPortName();
        synchronized (key.intern()) {
            // Double-check after acquiring lock
            SerialPort existing = activePorts.get(key);
            if (existing != null && existing.isOpen()) {
                return existing;
            }

            closePortUnsafe(key);

            String portName = config.isAutoDetectPort() ? findFirstAvailablePort() : config.getPortName();
            if (portName == null || portName.trim().isEmpty()) {
                throw new Exception("No serial port specified or detected");
            }

            SerialPort port = SerialPort.getCommPort(portName);
            if (port == null) {
                throw new Exception("Serial port not found: " + portName);
            }
            if (!port.openPort()) {
                throw new Exception("Failed to open serial port: " + portName);
            }

            // Auto-baud detection
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

            // PRODUCTION FIX: Use SEMI_BLOCKING with user-configurable timeouts
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                config.getReadTimeout(),
                config.getWriteTimeout()
            );

            // Signal control (jSerialComm 2.x uses setDTR()/clearDTR())
            if (config.isSetDTR()) { port.setDTR(); } else { port.clearDTR(); }
            if (config.isSetRTS()) { port.setRTS(); } else { port.clearRTS(); }

            // Break signal
            if (config.isSendBreakBeforeOpen()) {
                port.setBreak();
                Thread.sleep(config.getBreakDuration());
                port.clearBreak();
            }

            // Flush on open
            if (config.isFlushBuffersOnOpen()) {
                port.flushIOBuffers();
            }

            activePorts.put(key, port);
            statistics.put(key, new SerialStatistics());
            protocolLogs.put(key, Collections.synchronizedList(new ArrayList<>()));

            logger.info("Serial port opened: " + portName + " @ " + config.getBaudRate() + " for channel " + channelId);
            return port;
        }
    }

    public void closePort(String key) {
        synchronized (key.intern()) {
            closePortUnsafe(key);
        }
    }

    private void closePortUnsafe(String key) {
        SerialPort port = activePorts.remove(key);
        if (port != null && port.isOpen()) {
            // We don't have config here; flush is best-effort
            try { port.flushIOBuffers(); } catch (Exception ignored) {}
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
        logProtocol(key, dir, data, desc, 1000);
    }

    public void logProtocol(String key, ProtocolLogEntry.Direction dir, byte[] data, String desc, int maxEntries) {
        List<ProtocolLogEntry> log = protocolLogs.get(key);
        if (log != null) {
            log.add(new ProtocolLogEntry(dir, data, desc));
            while (log.size() > maxEntries) {
                log.remove(0);
            }
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

    /**
     * Production-grade auto-baud: reads a burst and validates printable/control chars.
     */
    private int autoDetectBaudRate(SerialPort port, SerialPortConfig config) {
        for (int baud : config.getAutoBaudRates()) {
            port.setBaudRate(baud);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
            byte[] buf = new byte[8];
            int read = port.readBytes(buf, buf.length);
            if (read >= 4) {
                boolean valid = true;
                for (int i = 0; i < read; i++) {
                    byte b = buf[i];
                    // Allow printable ASCII, CR, LF, TAB, and ASTM control chars
                    boolean ok = (b >= 0x20 && b <= 0x7E) || b == 0x0D || b == 0x0A || b == 0x09
                              || b == 0x02 || b == 0x03 || b == 0x05 || b == 0x06 || b == 0x17;
                    if (!ok) { valid = false; break; }
                }
                if (valid) {
                    port.flushIOBuffers();
                    return baud;
                }
            }
        }
        return config.getBaudRate();
    }

    public interface PortHealthListener {
        void onHealthEvent(String portKey, String event, Throwable error);
    }
}
