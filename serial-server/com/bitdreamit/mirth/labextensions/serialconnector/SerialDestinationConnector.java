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

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Serial Destination Connector — writes to RS-232/RS-485 ports.
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 * It must NOT contain SerialReceiverProperties or SerialDispatcherProperties.
 *
 * XStream registration is handled by SerialServerPlugin.init(), NOT here.
 */
public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    private SerialDispatcherProperties connectorProperties;
    private SerialPort serialPort;

    @Override
    public void onUndeploy() {}

    @Override
    public void onStart() {
        if (connectorProperties == null) {
            throw new IllegalStateException("connectorProperties is null in onStart()");
        }
        if (connectorProperties.isKeepConnectionOpen()) {
            try {
                serialPort = SerialPortManager.getOrOpenPort(connectorProperties.getPortConfig());
                eventController.dispatchEvent(new ConnectionStatusEvent(
                        getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.CONNECTED));
                logger.info("Serial destination pooled on " + connectorProperties.getPortConfig().getPortName());
            } catch (Exception e) {
                logger.error("SerialDestinationConnector.onStart FAILED for channel " + getChannelId() +
                             ": " + e.getMessage(), e);
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

    // PREMIUM: Statistics and protocol logger
    private SerialStatistics statistics = new SerialStatistics();
    private ProtocolLogger protocolLogger = null;

    @Override
    public void onDeploy() {
        Object raw = getConnectorProperties();
        if (raw == null) {
            throw new IllegalStateException("SerialDestinationConnector.onDeploy: connectorProperties is null");
        }
        if (!(raw instanceof SerialDispatcherProperties)) {
            throw new IllegalStateException(
                "SerialDestinationConnector.onDeploy: expected SerialDispatcherProperties but got " +
                raw.getClass().getName() + ". Clear <mirth>/extensions/.cache/ and restart Mirth.");
        }
        this.connectorProperties = (SerialDispatcherProperties) raw;

        // Initialize protocol logger if enabled
        SerialPortConfig config = connectorProperties.getPortConfig();
        if (config.isProtocolLoggingEnabled()) {
            protocolLogger = new ProtocolLogger(getChannelId(), config.getPortName(), config.getMaxLogEntries());
            logger.info("SerialDestinationConnector.onDeploy: protocol logging ENABLED");
        }
        statistics.reset();
        logger.info("SerialDestinationConnector.onDeploy: properties loaded for channel " + getChannelId() +
                    ", port=" + config.getPortName());
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties props, ConnectorMessage message) {
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

            // PREMIUM: Apply template if enabled (NextGen-style)
            String payload = message.getEncoded().getContent();
            if (config.isUseTemplate() && config.getMessageTemplate() != null
                    && !config.getMessageTemplate().isEmpty()) {
                payload = applyTemplate(config.getMessageTemplate(), payload);
            }

            byte[] data = buildFrame(payload, config);

            // PREMIUM: Log outgoing data
            if (protocolLogger != null) {
                protocolLogger.logOut(data, "message " + message.getMessageId());
            }

            int written = port.writeBytes(data, data.length);
            if (written < 0) {
                throw new Exception("Failed to write to serial port " + config.getPortName());
            }

            // PREMIUM: Record statistics
            statistics.recordWrite(written);
            statistics.recordMessageSent();

            logger.info("Wrote " + written + " bytes to " + config.getPortName() +
                        " (total: " + statistics.getBytesWritten() + " bytes, " +
                        statistics.getMessagesSent() + " msgs)");

            // ACK handling
            if (props.isWaitForAckAfterWrite()) {
                byte[] ackBuffer = new byte[props.getAckPattern().length];
                long start = System.currentTimeMillis();
                int totalRead = 0;

                while (totalRead < ackBuffer.length && (System.currentTimeMillis() - start) < props.getAckTimeout()) {
                    int read = port.readBytes(ackBuffer, ackBuffer.length - totalRead);
                    if (read > 0) {
                        totalRead += read;
                        if (protocolLogger != null) {
                            byte[] ackData = new byte[read];
                            System.arraycopy(ackBuffer, totalRead - read, ackData, 0, read);
                            protocolLogger.logIn(ackData, "ACK");
                        }
                    }
                    else Thread.sleep(10);
                }

                if (totalRead < ackBuffer.length || !Arrays.equals(ackBuffer, props.getAckPattern())) {
                    statistics.recordError();
                    throw new Exception("ACK timeout or mismatch on " + config.getPortName());
                }
            }

            // PREMIUM: Response processing
            String responseStr = null;
            if (config.isProcessResponse()) {
                responseStr = readResponse(port, config);
                if (protocolLogger != null && responseStr != null) {
                    protocolLogger.logIn(responseStr.getBytes(Charset.forName(config.getCharset())), "response");
                }
            }

            if (!pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }

            return new Response(Status.SENT, responseStr != null ? responseStr : "Sent " + written + " bytes");

        } catch (Throwable t) {
            statistics.recordError();
            logger.error("Serial write error on " + config.getPortName() + ": " + t.getMessage(), t);
            eventController.dispatchEvent(new ErrorEvent(
                    getChannelId(), getMetaDataId(), message.getMessageId(), ErrorEventType.DESTINATION_CONNECTOR,
                    getConnectorProperties().getName(), "Serial write error", t.getMessage(), t));
            if (port != null && !pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }
            return new Response(Status.ERROR, "Serial write failed: " + t.getMessage());
        }
    }

    /**
     * PREMIUM: Apply a NextGen-style template to the payload.
     * Template can contain ${message} placeholder which is replaced with the actual message.
     * Example template: "MSH|^~\\&|${message}|\r"
     */
    private String applyTemplate(String template, String message) {
        return template.replace("${message}", message)
                       .replace("${msg}", message)
                       .replace("${payload}", message);
    }

    /**
     * PREMIUM: Read response from the serial port after sending.
     * Reads until delimiter is found or timeout is reached.
     */
    private String readResponse(SerialPort port, SerialPortConfig config) {
        String delimiter = config.getResponseDelimiter();
        if (delimiter == null || delimiter.isEmpty()) delimiter = "\\r\\n";
        delimiter = delimiter.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");

        Charset charset = Charset.forName(config.getCharset());
        long start = System.currentTimeMillis();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] buf = new byte[256];

        while (System.currentTimeMillis() - start < config.getResponseTimeout()) {
            int read = port.readBytes(buf, buf.length);
            if (read > 0) {
                buffer.write(buf, 0, read);
                String current = new String(buffer.toByteArray(), charset);
                if (current.contains(delimiter)) {
                    return current.substring(0, current.indexOf(delimiter));
                }
            } else {
                try { Thread.sleep(10); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return buffer.size() > 0 ? new String(buffer.toByteArray(), charset) : null;
    }

    private byte[] buildFrame(String payload, SerialPortConfig config) throws Exception {
        String mode = config.getTransmissionMode();
        if (mode == null) mode = "RAW";

        // PREMIUM: Use dynamic transmission mode provider (like TCP connector)
        SerialTransmissionModeProvider provider =
                SerialTransmissionModeRegistry.getServerProvider(mode);

        if (provider == null) {
            // Fallback to RAW bytes if provider not found
            logger.warn("Transmission mode provider not found for '" + mode +
                        "', falling back to RAW. Available modes: " +
                        java.util.Arrays.toString(SerialTransmissionModeRegistry.getAvailableModes()));
            Charset cs = Charset.forName(config.getCharset());
            return config.isBinaryMode()
                    ? java.util.Base64.getDecoder().decode(payload)
                    : payload.getBytes(cs);
        }

        // Use the provider to frame the outgoing message
        return provider.frameMessage(payload, provider.getDefaultProperties(), config);
    }

    private String unescapeDelimiter(String delim) {
        if (delim == null) return "\r\n";
        return delim.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
    }

    /**
     * PREMIUM: Calculate checksum using the configured algorithm.
     * Supports: ASTM_STANDARD (mod 256 hex), MOD256, XOR, NONE
     */
    private byte[] calculateChecksum(byte[] data, String algorithm, Charset charset) {
        if (algorithm == null) algorithm = "ASTM_STANDARD";

        switch (algorithm.toUpperCase()) {
            case "NONE":
                return new byte[0];

            case "XOR": {
                int xor = 0;
                for (byte b : data) xor ^= (b & 0xFF);
                return String.format("%02X", xor & 0xFF).getBytes(charset);
            }

            case "MOD256": {
                int sum = 0;
                for (byte b : data) sum = (sum + (b & 0xFF)) % 256;
                return String.format("%03d", sum).getBytes(charset);
            }

            case "ASTM_STANDARD":
            default: {
                // Standard ASTM E1381 checksum: sum of bytes mod 256, as 2-digit hex
                int sum = 0;
                for (byte b : data) sum = (sum + b) & 0xFF;
                return String.format("%02X", sum).substring(0, 2).getBytes(charset);
            }
        }
    }

    private byte[] parseHexString(String hex) {
        if (hex == null || hex.trim().isEmpty()) return new byte[0];
        String clean = hex.replaceAll("\\s", "").toUpperCase();
        if (clean.length() % 2 != 0) clean = "0" + clean;
        byte[] result = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return result;
    }
}
