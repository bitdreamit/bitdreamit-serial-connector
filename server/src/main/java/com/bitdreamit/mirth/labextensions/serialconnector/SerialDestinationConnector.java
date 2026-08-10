/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.DeployException;
import com.mirth.connect.donkey.server.HaltException;
import com.mirth.connect.donkey.server.StartException;
import com.mirth.connect.donkey.server.StopException;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.util.TemplateValueReplacer;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Serial Destination Connector with signal waiting, ACK detection,
 * protocol analysis, and connection pooling.
 * Feature-complete replacement for commercial Serial Sender.
 */
public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);
    private final TemplateValueReplacer replacer = new TemplateValueReplacer();
    private final SerialPortManager portManager = SerialPortManager.getInstance();

    private SerialPort serialPort;
    private SerialPortConfig config;
    private SerialDispatcherProperties props;
    private String portKey;
    private SerialStatistics stats;

    @Override
    public void onDeploy() throws DeployException {}

    @Override
    public void onStart() throws StartException {
        props = (SerialDispatcherProperties) getConnectorProperties();
        config = props.getPortConfig();
        String resolvedPort = replacer.replaceValues(config.getPortName(), getChannelId(), getChannelName());
        portKey = getChannelId() + "@" + resolvedPort;

        try {
            if (!props.isKeepConnectionOpen()) {
                // Will open/close per message
                serialPort = null;
            } else {
                serialPort = portManager.openPort(config, getChannelId());
                stats = portManager.getStatistics(portKey);
            }
            logger.info("Serial destination started for " + portKey);
        } catch (Exception e) {
            throw new StartException("Failed to start serial destination: " + e.getMessage(), e);
        }
    }

    @Override
    public Response send(ConnectorMessage message) {
        try {
            SerialPort port = serialPort;
            if (!props.isKeepConnectionOpen() || port == null || !port.isOpen()) {
                port = portManager.openPort(config, getChannelId());
                stats = portManager.getStatistics(portKey);
            }

            // Wait for signals
            if (config.isWaitForCTS() || config.isWaitForDSR() || config.isWaitForDCD()) {
                boolean ok = portManager.waitForSignals(port, config);
                if (!ok) {
                    return new Response(Status.ERROR, null, "Serial signals not ready (CTS/DSR/DCD)");
                }
            }

            String payload = message.getEncodedData();
            byte[] bytes;
            if (config.isBinaryMode()) {
                bytes = hexToBytes(payload.replaceAll("\s", ""));
            } else {
                bytes = payload.getBytes(Charset.forName(config.getCharsetEncoding()));
            }

            if (config.isEnableProtocolAnalyzer()) {
                portManager.logProtocol(portKey, ProtocolLogEntry.Direction.OUT, bytes, "Write " + bytes.length + " bytes");
            }

            int written = port.writeBytes(bytes, bytes.length);
            if (written < bytes.length) {
                stats.recordError();
                return new Response(Status.ERROR, null, "Serial write incomplete: " + written + "/" + bytes.length);
            }
            stats.recordWrite(written);

            // Wait for ACK if configured
            if (props.isWaitForAckAfterWrite()) {
                byte[] ackBuf = new byte[props.getAckPattern().length];
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, props.getAckTimeout(), 0);
                int read = port.readBytes(ackBuf, ackBuf.length);
                if (read != ackBuf.length || !Arrays.equals(ackBuf, props.getAckPattern())) {
                    stats.recordError();
                    return new Response(Status.ERROR, null, "ACK not received or mismatch after write");
                }
                if (config.isEnableProtocolAnalyzer()) {
                    portManager.logProtocol(portKey, ProtocolLogEntry.Direction.IN, ackBuf, "ACK received");
                }
            }

            if (!props.isKeepConnectionOpen()) {
                portManager.closePort(portKey);
            }

            return new Response(Status.SENT);
        } catch (Exception e) {
            if (stats != null) stats.recordError();
            logger.error("Serial write error on " + portKey, e);
            return new Response(Status.ERROR, null, e.getMessage());
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public void onStop() throws StopException {
        if (!props.isKeepConnectionOpen()) {
            portManager.closePort(portKey);
        } else if (serialPort != null) {
            portManager.closePort(portKey);
        }
        logger.info("Serial destination stopped for " + portKey);
    }

    @Override
    public void onHalt() throws HaltException {
        portManager.closePort(portKey);
    }
}