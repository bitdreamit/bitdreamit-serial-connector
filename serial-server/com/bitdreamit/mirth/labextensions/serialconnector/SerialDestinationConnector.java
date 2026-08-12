package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

import java.util.Arrays;

public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    private SerialDispatcherProperties connectorProperties;
    private SerialPort serialPort;

    @Override
    public void onDeploy() {
        this.connectorProperties = (SerialDispatcherProperties) getConnectorProperties();
    }

    @Override
    public void onUndeploy() {
    }

    @Override
    public void onStart() {
        if (connectorProperties.isKeepConnectionOpen()) {
            try {
                serialPort = SerialPortManager.getOrOpenPort(connectorProperties.getPortConfig());
                eventController.dispatchEvent(new ConnectionStatusEvent(
                        getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.CONNECTED));
                logger.info("Serial destination pooled on " + connectorProperties.getPortConfig().getPortName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to open serial destination: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onStop() {
        if (serialPort != null) {
            SerialPortManager.releasePort(serialPort.getSystemPortName(), !connectorProperties.isKeepConnectionOpen());
            serialPort = null;
            eventController.dispatchEvent(new ConnectionStatusEvent(
                    getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.DISCONNECTED));
        }
    }

    @Override
    public void onHalt() {
        onStop();
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties props, ConnectorMessage message) {
        // No dynamic property replacement needed for serial transport
    }

    @Override
    public Response send(ConnectorProperties properties, ConnectorMessage message) {
        SerialDispatcherProperties props = (SerialDispatcherProperties) properties;
        SerialPortConfig config = props.getPortConfig();
        SerialPort port = null;
        boolean pooled = props.isKeepConnectionOpen();

        try {
            if (pooled && serialPort != null && serialPort.isOpen()) {
                port = serialPort;
            } else {
                port = SerialPortManager.getOrOpenPort(config);
            }

            String payload = message.getEncoded().getContent();
            byte[] data;
            if (config.isBinaryMode()) {
                data = java.util.Base64.getDecoder().decode(payload);
            } else {
                data = payload.getBytes(config.getCharset());
            }

            int written = port.writeBytes(data, data.length);
            if (written < 0) {
                throw new Exception("Failed to write to serial port");
            }

            logger.info("Serial destination wrote " + written + " bytes to " + config.getPortName());

            if (props.isWaitForAckAfterWrite()) {
                byte[] ackBuffer = new byte[props.getAckPattern().length];
                long start = System.currentTimeMillis();
                int totalRead = 0;

                while (totalRead < ackBuffer.length && (System.currentTimeMillis() - start) < props.getAckTimeout()) {
                    int read = port.readBytes(ackBuffer, ackBuffer.length - totalRead);
                    if (read > 0) totalRead += read;
                    else Thread.sleep(10);
                }

                if (totalRead < ackBuffer.length || !Arrays.equals(ackBuffer, props.getAckPattern())) {
                    throw new Exception("ACK timeout or mismatch. Expected: " + Arrays.toString(props.getAckPattern())
                            + ", Received: " + (totalRead > 0 ? Arrays.toString(Arrays.copyOf(ackBuffer, totalRead)) : "none"));
                }
            }

            if (!pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }

            return new Response(Status.SENT, "Sent " + written + " bytes to " + config.getPortName());

        } catch (Exception e) {
            logger.error("Serial destination error on " + config.getPortName(), e);
            eventController.dispatchEvent(new ErrorEvent(
                    getChannelId(), getMetaDataId(), message.getMessageId(), ErrorEventType.DESTINATION_CONNECTOR,
                    getConnectorProperties().getName(), "Serial write error", e.getMessage(), e));
            if (port != null && !pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }
            return new Response(Status.ERROR, "Serial write failed: " + e.getMessage());
        }
    }
}