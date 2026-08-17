package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serial port manager with connection pooling.
 *
 * CRITICAL: This class MUST exist ONLY in serial-shared.jar.
 *
 * All errors are logged via log4j (mirth.log) — NOT to hardcoded file paths.
 */
public class SerialPortManager {
    private static final Logger logger = Logger.getLogger(SerialPortManager.class);
    private static final Map<String, SerialPort> openPorts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> portRefCount = new ConcurrentHashMap<>();

    public static SerialPort getOrOpenPort(SerialPortConfig config) throws Exception {
        String portName = config.getPortName();
        if (portName == null || portName.trim().isEmpty()) {
            throw new Exception("SerialPortManager: portName is null/empty");
        }
        String lockKey = portName.intern();

        synchronized (lockKey) {
            SerialPort port = openPorts.get(portName);
            if (port != null && port.isOpen()) {
                portRefCount.get(portName).incrementAndGet();
                logger.info("Reusing existing port: " + portName);
                return port;
            }

            try {
                port = SerialPort.getCommPort(portName);
            } catch (Exception e) {
                throw new Exception("SerialPortManager: getCommPort('" + portName + "') failed: " + e.getMessage(), e);
            }

            try {
                port.setComPortParameters(
                        config.getBaudRate(),
                        config.getDataBits(),
                        config.getStopBits(),
                        config.getParity()
                );
                port.setFlowControl(config.getFlowControl());
                // Combine read semi-blocking + write blocking so BOTH timeouts are honored.
                // NOTE: jSerialComm 2.10.4 has TIMEOUT_WRITE_BLOCKING (not TIMEOUT_WRITE_SEMI_BLOCKING).
                port.setComPortTimeouts(
                        SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                        config.getReadTimeout(),
                        config.getWriteTimeout()
                );
            } catch (Exception e) {
                throw new Exception("SerialPortManager: failed to configure " + portName +
                        " (baud=" + config.getBaudRate() + ",data=" + config.getDataBits() +
                        ",stop=" + config.getStopBits() + ",parity=" + config.getParity() + "): " + e.getMessage(), e);
            }

            if (config.isSendBreak()) {
                if (!port.openPort()) {
                    throw new Exception("SerialPortManager: openPort() for sendBreak failed on " + portName);
                }
                port.setBreak();
                try { Thread.sleep(config.getBreakDuration()); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                port.clearBreak();
                port.closePort();
            }

            if (!port.openPort()) {
                // Surface the most common silent-fail cause: port missing / in use / permission denied
                throw new Exception("SerialPortManager: openPort() returned false for " + portName +
                        " — port is missing, already in use, or permission denied");
            }

            if (config.isFlushOnOpen()) {
                try { port.flushIOBuffers(); } catch (Throwable ignored) {}
            }

            if (config.isSetDtr()) port.setDTR();  else port.clearDTR();
            if (config.isSetRts()) port.setRTS();  else port.clearRTS();

            if (config.isWaitCts() && !port.getCTS()) {
                waitForSignal(port, config.getSignalTimeout(), "CTS", SerialPort::getCTS);
            }
            if (config.isWaitDsr() && !port.getDSR()) {
                waitForSignal(port, config.getSignalTimeout(), "DSR", SerialPort::getDSR);
            }
            if (config.isWaitDcd() && !port.getDCD()) {
                waitForSignal(port, config.getSignalTimeout(), "DCD", SerialPort::getDCD);
            }

            openPorts.put(portName, port);
            portRefCount.put(portName, new AtomicInteger(1));
            logger.info("Opened serial port: " + portName + " @ " + config.getBaudRate());
            return port;
        }
    }

    @FunctionalInterface
    private interface SignalCheck {
        boolean check(SerialPort port);
    }

    private static void waitForSignal(SerialPort port, int timeout, String name, SignalCheck check) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            if (check.check(port)) return;
            try { Thread.sleep(50); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new Exception("Timeout waiting for " + name + " on " + port.getSystemPortName());
    }

    public static void releasePort(String portName, boolean forceClose) {
        if (portName == null) return;
        String lockKey = portName.intern();
        synchronized (lockKey) {
            AtomicInteger refCount = portRefCount.get(portName);
            if (refCount == null) return;

            int remaining = refCount.decrementAndGet();
            if (forceClose || remaining <= 0) {
                SerialPort port = openPorts.remove(portName);
                portRefCount.remove(portName);
                if (port != null && port.isOpen()) {
                    try { port.flushIOBuffers(); } catch (Throwable ignored) {}
                    port.closePort();
                    logger.info("Closed serial port: " + portName);
                }
            }
        }
    }

    public static SerialPort getOpenPort(String portName) {
        return openPorts.get(portName);
    }
}
