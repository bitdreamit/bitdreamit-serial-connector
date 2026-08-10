/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.DeployException;
import com.mirth.connect.donkey.server.HaltException;
import com.mirth.connect.donkey.server.StartException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serial Source Connector with protocol analysis, health monitoring,
 * auto-reconnect, and signal monitoring.
 * Feature-complete replacement for commercial Serial Listener.
 */
public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);
    private final TemplateValueReplacer replacer = new TemplateValueReplacer();
    private final SerialPortManager portManager = SerialPortManager.getInstance();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private SerialPort serialPort;
    private SerialPortConfig config;
    private String portKey;
    private Thread readerThread;
    private Thread healthThread;
    private EventController eventController;
    private SerialStatistics stats;

    @Override
    public void onDeploy() throws DeployException {
        eventController = ControllerFactory.getFactory().createEventController();
    }

    @Override
    public void onStart() throws StartException {
        SerialReceiverProperties props = (SerialReceiverProperties) getConnectorProperties();
        config = props.getPortConfig();
        portKey = getChannelId() + "@" + replacer.replaceValues(config.getPortName(), getChannelId(), getChannelName());

        try {
            serialPort = portManager.openPort(config, getChannelId());
            stats = portManager.getStatistics(portKey);
            running.set(true);

            eventController.dispatchEvent(
                new ConnectionStatusEventType(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.CONNECTED)
            );

            readerThread = new Thread(this::readLoop, "BitDreamIT-SerialReader-" + portKey);
            readerThread.start();

            if (config.isEnableHealthMonitor()) {
                healthThread = new Thread(this::healthLoop, "BitDreamIT-SerialHealth-" + portKey);
                healthThread.start();
            }

            logger.info("Serial source started on " + portKey);
        } catch (Exception e) {
            eventController.dispatchEvent(
                new ErrorEventType(getChannelId(), getMetaDataId(), null, "Serial start failed: " + e.getMessage())
            );
            throw new StartException("Failed to start serial source: " + e.getMessage(), e);
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
                    eventController.dispatchEvent(
                        new ErrorEventType(getChannelId(), getMetaDataId(), null, "Serial read: " + e.getMessage())
                    );
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
                    eventController.dispatchEvent(
                        new ErrorEventType(getChannelId(), getMetaDataId(), null, "Serial disconnect detected, reconnecting...")
                    );

                    if (attempts < config.getMaxReconnectAttempts()) {
                        Thread.sleep(config.getReconnectDelay());
                        try {
                            serialPort = portManager.openPort(config, getChannelId());
                            stats.recordReconnect();
                            attempts = 0;
                            logger.info("Serial port " + portKey + " reconnected");
                            eventController.dispatchEvent(
                                new ConnectionStatusEventType(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.CONNECTED)
                            );
                        } catch (Exception re) {
                            attempts++;
                            logger.error("Reconnect attempt " + attempts + " failed for " + portKey, re);
                        }
                    } else {
                        logger.error("Max reconnect attempts reached for " + portKey);
                        running.set(false);
                    }
                } else {
                    attempts = 0; // reset on healthy
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
    public void onStop() throws StopException {
        running.set(false);
        if (readerThread != null) {
            try { readerThread.join(2000); } catch (InterruptedException ignored) {}
        }
        if (healthThread != null) {
            try { healthThread.join(1000); } catch (InterruptedException ignored) {}
        }
        portManager.closePort(portKey);
        eventController.dispatchEvent(
            new ConnectionStatusEventType(getChannelId(), getMetaDataId(), getSourceName(), ConnectionStatusEventType.DISCONNECTED)
        );
        logger.info("Serial source stopped on " + portKey);
    }

    @Override
    public void onHalt() throws HaltException {
        running.set(false);
        portManager.closePort(portKey);
    }
}