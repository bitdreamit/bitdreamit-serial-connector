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
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Serial Destination Connector — writes to RS-232/RS-485 ports.
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 * It must NOT contain SerialReceiverProperties or SerialDispatcherProperties.
 *
 * All diagnostic logging goes through log4j (mirth.log).
 */
public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    private SerialDispatcherProperties connectorProperties;
    private SerialPort serialPort;

    static {
        registerXStreamPermission();
    }

    private static void registerXStreamPermission() {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                xstream.processAnnotations(SerialDispatcherProperties.class);
                xstream.processAnnotations(SerialPortConfig.class);
                logger.info("SerialDestinationConnector: XStream permission + annotations registered.");
            } else {
                logger.error("SerialDestinationConnector: XStream instance is NULL — " +
                             "channel deserialization will fail with ForbiddenClassException!");
            }
        } catch (Throwable t) {
            logger.error("SerialDestinationConnector: FAILED to register XStream permission", t);
        }
    }

    @SuppressWarnings("unchecked")
    private static XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) return null;

            try {
                java.lang.reflect.Method m = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = m.invoke(serializer);
                if (val instanceof XStream) return (XStream) val;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                logger.warn("SerialDestinationConnector: getXStream() threw: " + e.getMessage());
            }

            XStream found = findXStreamField(serializer, serializer.getClass());
            if (found != null) return found;

            for (java.lang.reflect.Field f : serializer.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) {
                        XStream inner = findXStreamField(val, val.getClass());
                        if (inner != null) return inner;
                    }
                } catch (Exception ignored) {}
            }
            return null;
        } catch (Throwable t) {
            logger.error("SerialDestinationConnector: error finding XStream: " + t.getMessage(), t);
            return null;
        }
    }

    private static XStream findXStreamField(Object target, Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(target);
                        if (val instanceof XStream) return (XStream) val;
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

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
        logger.info("SerialDestinationConnector.onDeploy: properties loaded for channel " + getChannelId() +
                    ", port=" + connectorProperties.getPortConfig().getPortName());
    }

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

            String payload = message.getEncoded().getContent();
            byte[] data = buildFrame(payload, config);

            int written = port.writeBytes(data, data.length);
            if (written < 0) {
                throw new Exception("Failed to write to serial port " + config.getPortName());
            }

            logger.info("Wrote " + written + " bytes to " + config.getPortName());

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
                    throw new Exception("ACK timeout or mismatch on " + config.getPortName());
                }
            }

            if (!pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }

            return new Response(Status.SENT, "Sent " + written + " bytes");

        } catch (Throwable t) {
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

    private byte[] buildFrame(String payload, SerialPortConfig config) throws Exception {
        String mode = config.getTransmissionMode();
        Charset charset = Charset.forName(config.getCharset());
        byte[] payloadBytes = config.isBinaryMode()
                ? java.util.Base64.getDecoder().decode(payload)
                : payload.getBytes(charset);

        if (mode == null) mode = "RAW";
        switch (mode.toUpperCase()) {
            case "RAW":
                return payloadBytes;

            case "LINE": {
                String delimiter = unescapeDelimiter(config.getLineDelimiter());
                byte[] delimBytes = delimiter.getBytes(charset);
                byte[] lineResult = new byte[payloadBytes.length + delimBytes.length];
                System.arraycopy(payloadBytes, 0, lineResult, 0, payloadBytes.length);
                System.arraycopy(delimBytes, 0, lineResult, payloadBytes.length, delimBytes.length);
                return lineResult;
            }

            case "FRAME": {
                byte[] start = parseHexString(config.getStartOfMessageBytes());
                byte[] end = parseHexString(config.getEndOfMessageBytes());
                byte[] frameResult = new byte[start.length + payloadBytes.length + end.length];
                System.arraycopy(start, 0, frameResult, 0, start.length);
                System.arraycopy(payloadBytes, 0, frameResult, start.length, payloadBytes.length);
                System.arraycopy(end, 0, frameResult, start.length + payloadBytes.length, end.length);
                return frameResult;
            }

            case "MLLP": {
                byte[] start = parseHexString(config.getStartOfMessageBytes());
                byte[] end = parseHexString(config.getEndOfMessageBytes());
                if (start.length == 0) start = new byte[]{0x0B};
                if (end.length == 0) end = new byte[]{0x1C, 0x0D};
                byte[] mllpResult = new byte[start.length + payloadBytes.length + end.length];
                System.arraycopy(start, 0, mllpResult, 0, start.length);
                System.arraycopy(payloadBytes, 0, mllpResult, start.length, payloadBytes.length);
                System.arraycopy(end, 0, mllpResult, start.length + payloadBytes.length, end.length);
                return mllpResult;
            }

            case "ASTM": {
                byte[] start = parseHexString(config.getStartOfMessageBytes());
                byte[] end = parseHexString(config.getEndOfMessageBytes());
                if (start.length == 0) start = new byte[]{0x02};
                if (end.length == 0) end = new byte[]{0x03};

                int sum = 0;
                for (byte b : payloadBytes) sum = (sum + b) & 0xFF;
                String chkStr = String.format("%02X", sum).substring(0, 2);
                byte[] chkBytes = chkStr.getBytes(charset);

                byte[] crlf = new byte[]{0x0D, 0x0A};
                byte[] astmResult = new byte[start.length + payloadBytes.length + end.length + chkBytes.length + crlf.length];
                int pos = 0;
                System.arraycopy(start, 0, astmResult, pos, start.length); pos += start.length;
                System.arraycopy(payloadBytes, 0, astmResult, pos, payloadBytes.length); pos += payloadBytes.length;
                System.arraycopy(end, 0, astmResult, pos, end.length); pos += end.length;
                System.arraycopy(chkBytes, 0, astmResult, pos, chkBytes.length); pos += chkBytes.length;
                System.arraycopy(crlf, 0, astmResult, pos, crlf.length);
                return astmResult;
            }

            case "BASIC":
                return buildFrame(payload, "LINE", config, charset, payloadBytes);

            case "ASTM_E1381":
                return buildFrame(payload, "ASTM", config, charset, payloadBytes);

            default:
                return payloadBytes;
        }
    }

    private byte[] buildFrame(String payload, String mode, SerialPortConfig config,
                              Charset charset, byte[] payloadBytes) throws Exception {
        // Internal helper for BASIC→LINE and ASTM_E1381→ASTM compatibility
        String save = config.getTransmissionMode();
        try {
            config.setTransmissionMode(mode);
            return buildFrame(payload, config);
        } finally {
            config.setTransmissionMode(save);
        }
    }

    private String unescapeDelimiter(String delim) {
        if (delim == null) return "\r\n";
        return delim.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
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
