package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    private SerialReceiverProperties connectorProperties;
    private SerialPort serialPort;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicReference<Thread> readerThread = new AtomicReference<>();
    private AtomicReference<Thread> healthThread = new AtomicReference<>();

    @Override
    public void onDeploy() {
        this.connectorProperties = (SerialReceiverProperties) getConnectorProperties();
    }

    @Override
    public void onUndeploy() {
    }

    @Override
    public void onStart() {
        running.set(true);
        try {
            openPort();
            startReader();
            startHealthMonitor();
        } catch (Exception e) {
            running.set(false);
            throw new RuntimeException("Failed to start serial source: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStop() {
        running.set(false);
        stopReader();
        stopHealthMonitor();
        closePort();
    }

    @Override
    public void onHalt() {
        onStop();
    }

    private void openPort() throws Exception {
        serialPort = SerialPortManager.getOrOpenPort(connectorProperties.getPortConfig());
        connected.set(true);
        eventController.dispatchEvent(new ConnectionStatusEvent(
                getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.CONNECTED));
        logger.info("Serial source connected on " + connectorProperties.getPortConfig().getPortName());
    }

    private void closePort() {
        if (serialPort != null) {
            SerialPortManager.releasePort(serialPort.getSystemPortName(), true);
            serialPort = null;
        }
        connected.set(false);
        eventController.dispatchEvent(new ConnectionStatusEvent(
                getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.DISCONNECTED));
    }

    private void startReader() {
        Thread t = new Thread(this::readLoop, "SerialReader-" + getChannelId());
        t.setDaemon(true);
        readerThread.set(t);
        t.start();
    }

    private void stopReader() {
        Thread t = readerThread.getAndSet(null);
        if (t != null) {
            t.interrupt();
            try { t.join(2000); } catch (InterruptedException ignored) {}
        }
    }

    private void startHealthMonitor() {
        if (!connectorProperties.getPortConfig().isAutoDetectPort()) {
            Thread t = new Thread(this::healthLoop, "SerialHealth-" + getChannelId());
            t.setDaemon(true);
            healthThread.set(t);
            t.start();
        }
    }

    private void stopHealthMonitor() {
        Thread t = healthThread.getAndSet(null);
        if (t != null) {
            t.interrupt();
            try { t.join(2000); } catch (InterruptedException ignored) {}
        }
    }

    private void readLoop() {
        SerialPortConfig config = connectorProperties.getPortConfig();
        byte[] buffer = new byte[config.getBufferSize()];

        while (running.get()) {
            try {
                if (serialPort == null || !serialPort.isOpen()) {
                    Thread.sleep(100);
                    continue;
                }

                int bytesRead = serialPort.readBytes(buffer, buffer.length);
                if (bytesRead > 0) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Serial read " + bytesRead + " bytes from " + config.getPortName());
                    }

                    String payload;
                    if (config.isBinaryMode()) {
                        payload = java.util.Base64.getEncoder().encodeToString(data);
                    } else {
                        payload = new String(data, config.getCharset());
                    }

                    dispatchRawMessage(new RawMessage(payload));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Serial read error on " + config.getPortName(), e);
                eventController.dispatchEvent(new ErrorEvent(
                        getChannelId(), getMetaDataId(), null, ErrorEventType.SOURCE_CONNECTOR,
                        getConnectorProperties().getName(), "Serial read error", e.getMessage(), e));
                connected.set(false);
            }
        }
    }

    private void healthLoop() {
        SerialPortConfig config = connectorProperties.getPortConfig();
        int maxReconnect = 10;
        int reconnectDelay = 5000;
        int reconnectAttempts = 0;

        while (running.get()) {
            try {
                Thread.sleep(reconnectDelay);
                if (!running.get()) break;

                if (serialPort == null || !serialPort.isOpen()) {
                    if (reconnectAttempts < maxReconnect) {
                        logger.warn("Serial port disconnected, reconnecting... (" + (reconnectAttempts + 1) + "/" + maxReconnect + ")");
                        try {
                            closePort();
                            openPort();
                            startReader();
                            reconnectAttempts = 0;
                            logger.info("Serial source reconnected on " + config.getPortName());
                        } catch (Exception e) {
                            reconnectAttempts++;
                            logger.error("Reconnect failed: " + e.getMessage());
                        }
                    } else {
                        logger.error("Max reconnects reached. Stopping.");
                        eventController.dispatchEvent(new ErrorEvent(
                                getChannelId(), getMetaDataId(), null, ErrorEventType.SOURCE_CONNECTOR,
                                getConnectorProperties().getName(), "Max reconnects reached", "Serial source stopped", null));
                        break;
                    }
                } else {
                    reconnectAttempts = 0;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {
    }
}