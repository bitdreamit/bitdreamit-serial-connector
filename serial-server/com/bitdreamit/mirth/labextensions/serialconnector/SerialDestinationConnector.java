/*
 * BitDreamIT Mirth Lab Extensions
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.util.TemplateValueReplacer;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.Arrays;

public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);
    private final TemplateValueReplacer replacer = new TemplateValueReplacer();
    private final SerialPortManager portManager = SerialPortManager.getInstance();

    private SerialDispatcherProperties props;

    @Override
    public void onDeploy() {}

    @Override
    public void onUndeploy() {}

    @Override
    public void onStart() {
        props = (SerialDispatcherProperties) getConnectorProperties();
        logger.info("Serial destination started for channel " + getChannelId());
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties properties, ConnectorMessage message) {
        // Variable replacement happens here if needed
        // For serial port settings, no template replacement required
    }

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) throws InterruptedException {
        SerialDispatcherProperties currentProps = (SerialDispatcherProperties) connectorProperties;
        SerialPortConfig config = currentProps.getPortConfig();
        String resolvedPort = replacer.replaceValues(config.getPortName(), getChannelId(), getChannelId());
        String portKey = getChannelId() + "@" + resolvedPort;
        SerialStatistics stats = portManager.getStatistics(portKey);

        try {
            SerialPort port = portManager.openPort(config, getChannelId());
            if (stats == null) stats = portManager.getStatistics(portKey);

            // Wait for signals
            if (config.isWaitForCTS() || config.isWaitForDSR() || config.isWaitForDCD()) {
                boolean ok = portManager.waitForSignals(port, config);
                if (!ok) {
                    return new Response("ERROR: Serial signals not ready (CTS/DSR/DCD)");
                }
            }

            // Get payload from message
            String payload = connectorMessage.getEncoded() != null
                    ? connectorMessage.getEncoded().getContent()
                    : "";

            byte[] bytes;
            if (config.isBinaryMode()) {
                bytes = hexToBytes(payload.replaceAll("\\s", ""));
            } else {
                bytes = payload.getBytes(Charset.forName(config.getCharsetEncoding()));
            }

            if (config.isEnableProtocolAnalyzer()) {
                portManager.logProtocol(portKey, ProtocolLogEntry.Direction.OUT, bytes, "Write " + bytes.length + " bytes");
            }

            int written = port.writeBytes(bytes, bytes.length);
            if (written < bytes.length) {
                if (stats != null) stats.recordError();
                return new Response("ERROR: Serial write incomplete: " + written + "/" + bytes.length);
            }
            if (stats != null) stats.recordWrite(written);

            // Wait for ACK if configured
            if (currentProps.isWaitForAckAfterWrite()) {
                byte[] ackBuf = new byte[currentProps.getAckPattern().length];
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, currentProps.getAckTimeout(), 0);
                int read = port.readBytes(ackBuf, ackBuf.length);
                if (read != ackBuf.length || !Arrays.equals(ackBuf, currentProps.getAckPattern())) {
                    if (stats != null) stats.recordError();
                    return new Response("ERROR: ACK not received or mismatch after write");
                }
                if (config.isEnableProtocolAnalyzer()) {
                    portManager.logProtocol(portKey, ProtocolLogEntry.Direction.IN, ackBuf, "ACK received");
                }
            }

            if (!currentProps.isKeepConnectionOpen()) {
                portManager.closePort(portKey);
            }

            return new Response("Serial write successful: " + written + " bytes");

        } catch (Exception e) {
            if (stats != null) stats.recordError();
            logger.error("Serial write error on " + portKey, e);
            return new Response("ERROR: " + e.getMessage());
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
    public void onStop() {
        logger.info("Serial destination stopped for channel " + getChannelId());
    }

    @Override
    public void onHalt() {
        // Emergency shutdown
    }
}