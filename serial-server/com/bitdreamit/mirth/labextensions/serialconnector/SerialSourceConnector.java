/*
 * BitDreamIT Mirth Lab Extensions
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);
    private final SerialPortManager portManager = SerialPortManager.getInstance();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private SerialPort serialPort;
    private SerialPortConfig config;
    private String portKey;
    private Thread readerThread;
    private Thread healthThread;
    private SerialStatistics stats;

    @Override
    public void onDeploy() {}

    @Override
    public void onUndeploy() {}

    @Override
    public void onStart() {
        SerialReceiverProperties props = (SerialReceiverProperties) getConnectorProperties();
        config = props.getPortConfig();
        portKey = getChannelId() + "@" + config.getPortName();

        try {
            serialPort = portManager.openPort(config, getChannelId());
            stats = portManager.getStatistics(portKey);
            running.set(true);

            readerThread = new Thread(this::readLoop, "BitDreamIT-SerialReader-" + portKey);
            readerThread.start();

            if (config.isEnableHealthMonitor()) {
                healthThread = new Thread(this::healthLoop, "BitDreamIT-SerialHealth-" + portKey);
                healthThread.start();
            }

            logger.info("Serial source started on " + portKey);
        } catch (Exception e) {
            logger.error("Failed to start serial source on " + portKey, e);
            throw new RuntimeException("Failed to start serial source: " + e.getMessage(), e);
        }
    }

    private void readLoop() {
        Charset charset = Charset.forName(config.getCharsetEncoding());
        byte[] buffer = new byte[config.getBufferSize()];

        while (running.get() && serialPort != null && serialPort.isOpen()) {
            try {
                int len = serialPort.readBytes(buffer, buffer.length);
                if (len > 0) {
                    byte[] data = new byte[len];
                    System.arraycopy(buffer, 0, data, 0, len);
                    stats.recordRead(len);

                    if (config.isEnableProtocolAnalyzer()) {
                        portManager.logProtocol(portKey, ProtocolLogEntry.Direction.IN, data, "Read " + len + " bytes");
                    }

                    String payload;
                    if (config.isBinaryMode()) {
                        payload = bytesToHex(data);
                    } else {
                        payload = new String(data, charset);
                    }

                    dispatchRawMessage(new RawMessage(payload));
                }
            } catch (Exception e) {
                if (running.get()) {
                    stats.recordError();
                    logger.error("Serial read error on " + portKey, e);
                }
            }
        }
    }

    private void healthLoop() {
        int attempts = 0;
        while (running.get()) {
            try {
                Thread.sleep(config.getHealthCheckInterval());
                if (!serialPort.isOpen()) {
                    stats.recordError();
                    logger.warn("Serial port " + portKey + " disconnected. Attempting reconnect...");

                    if (attempts < config.getMaxReconnectAttempts()) {
                        Thread.sleep(config.getReconnectDelay());
                        try {
                            serialPort = portManager.openPort(config, getChannelId());
                            stats.recordReconnect();
                            attempts = 0;
                            logger.info("Serial port " + portKey + " reconnected");
                        } catch (Exception re) {
                            attempts++;
                            logger.error("Reconnect attempt " + attempts + " failed for " + portKey, re);
                        }
                    } else {
                        logger.error("Max reconnect attempts reached for " + portKey);
                        running.set(false);
                    }
                } else {
                    attempts = 0;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    @Override
    public void onStop() {
        running.set(false);
        if (readerThread != null) {
            try { readerThread.join(2000); } catch (InterruptedException ignored) {}
        }
        if (healthThread != null) {
            try { healthThread.join(1000); } catch (InterruptedException ignored) {}
        }
        portManager.closePort(portKey);
        logger.info("Serial source stopped on " + portKey);
    }

    @Override
    public void onHalt() {
        running.set(false);
        portManager.closePort(portKey);
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        // Default empty implementation
    }
}