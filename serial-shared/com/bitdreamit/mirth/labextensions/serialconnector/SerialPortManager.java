package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SerialPortManager {
    private static final Logger logger = Logger.getLogger(SerialPortManager.class);
    private static final Map<String, SerialPort> openPorts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> portRefCount = new ConcurrentHashMap<>();

    public static SerialPort getOrOpenPort(SerialPortConfig config) throws Exception {
        String portName = config.getPortName();
        String lockKey = portName.intern();

        synchronized (lockKey) {
            SerialPort port = openPorts.get(portName);
            if (port != null && port.isOpen()) {
                portRefCount.get(portName).incrementAndGet();
                logger.info("Reusing existing port: " + portName);
                return port;
            }

            port = SerialPort.getCommPort(portName);
            port.setComPortParameters(
                    config.getBaudRate(),
                    config.getDataBits(),
                    config.getStopBits(),
                    config.getParity()
            );
            port.setFlowControl(config.getFlowControl());
            port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    config.getReadTimeout(),
                    config.getWriteTimeout()
            );

            if (config.isSendBreak()) {
                port.openPort();
                port.setBreak();
                Thread.sleep(config.getBreakDuration());
                port.clearBreak();
                port.closePort();
            }

            if (!port.openPort()) {
                throw new Exception("Failed to open serial port: " + portName);
            }

            if (config.isFlushOnOpen()) {
                port.flushIOBuffers();
            }

            if (config.isSetDtr()) port.setDTR();
            else port.clearDTR();

            if (config.isSetRts()) port.setRTS();
            else port.clearRTS();

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
            Thread.sleep(50);
        }
        throw new Exception("Timeout waiting for " + name + " on " + port.getSystemPortName());
    }

    public static void releasePort(String portName, boolean forceClose) {
        String lockKey = portName.intern();
        synchronized (lockKey) {
            AtomicInteger refCount = portRefCount.get(portName);
            if (refCount == null) return;

            int remaining = refCount.decrementAndGet();
            if (forceClose || remaining <= 0) {
                SerialPort port = openPorts.remove(portName);
                portRefCount.remove(portName);
                if (port != null && port.isOpen()) {
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