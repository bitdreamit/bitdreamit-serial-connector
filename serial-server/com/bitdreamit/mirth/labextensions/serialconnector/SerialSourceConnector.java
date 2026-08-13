package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.event.ConnectionStatusEventType;
import com.mirth.connect.donkey.model.event.ErrorEventType;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.ChannelException;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.donkey.server.event.ConnectionStatusEvent;
import com.mirth.connect.donkey.server.event.ErrorEvent;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serial Source Connector — reads from RS-232/RS-485 ports.
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 * It must NOT contain SerialReceiverProperties or SerialDispatcherProperties —
 * those live in serial-shared.jar.
 *
 * All diagnostic logging goes through log4j (mirth.log).
 * No hardcoded file paths.
 */
public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    static {
        registerXStreamPermission();
    }

    private static void registerXStreamPermission() {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                xstream.processAnnotations(SerialReceiverProperties.class);
                xstream.processAnnotations(SerialPortConfig.class);
                logger.info("SerialSourceConnector: XStream permission + annotations registered.");
            } else {
                logger.error("SerialSourceConnector: XStream instance is NULL — " +
                             "channel deserialization will fail with ForbiddenClassException!");
            }
        } catch (Throwable t) {
            logger.error("SerialSourceConnector: FAILED to register XStream permission", t);
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
                logger.warn("SerialSourceConnector: getXStream() threw: " + e.getMessage());
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
            logger.error("SerialSourceConnector: error finding XStream: " + t.getMessage(), t);
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

    private SerialReceiverProperties connectorProperties;
    private SerialPort serialPort;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicReference<Thread> readerThread = new AtomicReference<>();
    private AtomicReference<Thread> healthThread = new AtomicReference<>();
    private ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();

    @Override
    public void onDeploy() {
        Object raw = getConnectorProperties();
        if (raw == null) {
            throw new IllegalStateException("SerialSourceConnector.onDeploy: connectorProperties is null — " +
                "Mirth did not provide properties. Check that source.xml sharedClassName matches " +
                "the class in serial-shared.jar.");
        }
        if (!(raw instanceof SerialReceiverProperties)) {
            throw new IllegalStateException(
                "SerialSourceConnector.onDeploy: expected SerialReceiverProperties but got " +
                raw.getClass().getName() + ". This means the wrong class is being loaded — " +
                "clear <mirth>/extensions/.cache/ and restart Mirth.");
        }
        this.connectorProperties = (SerialReceiverProperties) raw;
        logger.info("SerialSourceConnector.onDeploy: properties loaded for channel " + getChannelId() +
                    ", port=" + connectorProperties.getPortConfig().getPortName());
    }

    @Override
    public void onUndeploy() {}

    @Override
    public void onStart() {
        running.set(true);
        try {
            if (connectorProperties == null) {
                throw new IllegalStateException("connectorProperties is null in onStart() — onDeploy() failed.");
            }
            logger.info("SerialSourceConnector.onStart: starting channel " + getChannelId() +
                        " on port " + connectorProperties.getPortConfig().getPortName());
            openPort();
            startReader();
            startHealthMonitor();
        } catch (Throwable t) {
            running.set(false);
            logger.error("SerialSourceConnector.onStart FAILED for channel " + getChannelId() +
                         ": " + t.getClass().getName() + ": " + t.getMessage(), t);
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new RuntimeException("Failed to start serial source: " + t.getMessage(), t);
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
        eventController.dispatchEvent(new ConnectionStatusEvent(
                getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.CONNECTED));
        logger.info("Serial source connected on " + connectorProperties.getPortConfig().getPortName());
    }

    private void closePort() {
        if (serialPort != null) {
            SerialPortManager.releasePort(serialPort.getSystemPortName(), true);
            serialPort = null;
            eventController.dispatchEvent(new ConnectionStatusEvent(
                    getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.DISCONNECTED));
        }
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
        SerialPortConfig config = connectorProperties.getPortConfig();
        if (!config.isHealthMonitorEnabled() || config.isAutoDetectPort()) return;
        Thread t = new Thread(this::healthLoop, "SerialHealth-" + getChannelId());
        t.setDaemon(true);
        healthThread.set(t);
        t.start();
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
                    processBytes(data, config);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Serial read error on " + config.getPortName(), e);
                eventController.dispatchEvent(new ErrorEvent(
                        getChannelId(), getMetaDataId(), null, ErrorEventType.SOURCE_CONNECTOR,
                        getConnectorProperties().getName(), "Serial read error", e.getMessage(), e));
            }
        }
    }

    private void processBytes(byte[] data, SerialPortConfig config) throws Exception {
        String mode = config.getTransmissionMode();
        switch (mode == null ? "RAW" : mode.toUpperCase()) {
            case "RAW":           dispatchRaw(data, config); break;
            case "LINE":          processLineMode(data, config); break;
            case "FRAME":         processFrameMode(data, config); break;
            case "MLLP":          processMllpMode(data, config); break;
            case "ASTM":          processAstmMode(data, config); break;
            case "BASIC":         processLineMode(data, config); break;
            case "ASTM_E1381":    processAstmMode(data, config); break;
            default:              dispatchRaw(data, config);
        }
    }

    private void dispatchRaw(byte[] data, SerialPortConfig config) {
        String payload = bytesToPayload(data, config);
        try { dispatchRawMessage(new RawMessage(payload)); }
        catch (ChannelException e) { logger.error("Failed to dispatch raw message", e); }
    }

    private void processLineMode(byte[] data, SerialPortConfig config) {
        frameBuffer.write(data, 0, data.length);
        String delimiter = unescapeDelimiter(config.getLineDelimiter());
        byte[] buffer = frameBuffer.toByteArray();
        Charset cs = Charset.forName(config.getCharset());
        String text = new String(buffer, cs);
        int idx;
        while ((idx = text.indexOf(delimiter)) >= 0) {
            String line = text.substring(0, idx);
            text = text.substring(idx + delimiter.length());
            if (!line.isEmpty()) {
                try { dispatchRawMessage(new RawMessage(line)); }
                catch (ChannelException e) { logger.error("Failed to dispatch line message", e); }
            }
        }
        frameBuffer.reset();
        if (!text.isEmpty()) {
            byte[] remaining = text.getBytes(cs);
            frameBuffer.write(remaining, 0, remaining.length);
        }
    }

    private void processFrameMode(byte[] data, SerialPortConfig config) {
        frameBuffer.write(data, 0, data.length);
        byte[] start = parseHexString(config.getStartOfMessageBytes());
        byte[] end = parseHexString(config.getEndOfMessageBytes());
        if (start.length == 0 || end.length == 0) { dispatchRaw(data, config); return; }
        byte[] buffer = frameBuffer.toByteArray();
        int searchStart = 0;
        while (true) {
            int frameStart = indexOf(buffer, start, searchStart);
            if (frameStart < 0) break;
            int payloadStart = frameStart + start.length;
            int frameEnd = indexOf(buffer, end, payloadStart);
            if (frameEnd < 0) break;
            byte[] payload = Arrays.copyOfRange(buffer, payloadStart, frameEnd);
            dispatchRaw(payload, config);
            searchStart = frameEnd + end.length;
        }
        if (searchStart > 0) {
            byte[] remaining = Arrays.copyOfRange(buffer, searchStart, buffer.length);
            frameBuffer.reset();
            frameBuffer.write(remaining, 0, remaining.length);
        }
    }

    private void processMllpMode(byte[] data, SerialPortConfig config) throws Exception {
        frameBuffer.write(data, 0, data.length);
        byte[] start = parseHexString(config.getStartOfMessageBytes());
        byte[] end = parseHexString(config.getEndOfMessageBytes());
        if (start.length == 0) start = new byte[]{0x0B};
        if (end.length == 0) end = new byte[]{0x1C, 0x0D};
        byte[] buffer = frameBuffer.toByteArray();
        int searchStart = 0;
        while (true) {
            int startIdx = indexOf(buffer, start, searchStart);
            if (startIdx < 0) break;
            int payloadStart = startIdx + start.length;
            int endIdx = indexOf(buffer, end, payloadStart);
            if (endIdx < 0) break;
            byte[] payload = Arrays.copyOfRange(buffer, payloadStart, endIdx);
            dispatchRaw(payload, config);
            if (config.isUseMLLPv2()) {
                byte[] ack = parseHexString(config.getCommitAckBytes());
                if (ack.length > 0) serialPort.writeBytes(ack, ack.length);
            }
            searchStart = endIdx + end.length;
        }
        if (searchStart > 0) {
            byte[] remaining = Arrays.copyOfRange(buffer, searchStart, buffer.length);
            frameBuffer.reset();
            frameBuffer.write(remaining, 0, remaining.length);
        }
    }

    private void processAstmMode(byte[] data, SerialPortConfig config) throws Exception {
        frameBuffer.write(data, 0, data.length);
        byte[] start = parseHexString(config.getStartOfMessageBytes());
        byte[] end = parseHexString(config.getEndOfMessageBytes());
        byte[] ackBytes = parseHexString(config.getCommitAckBytes());
        byte[] nakBytes = parseHexString(config.getCommitNakBytes());
        if (start.length == 0) start = new byte[]{0x02};
        if (end.length == 0) end = new byte[]{0x03};
        if (ackBytes.length == 0) ackBytes = new byte[]{0x06};
        if (nakBytes.length == 0) nakBytes = new byte[]{0x15};
        byte[] buffer = frameBuffer.toByteArray();
        int searchStart = 0;
        while (true) {
            int enqIdx = indexOfByte(buffer, (byte) 0x05, searchStart);
            if (enqIdx >= 0) {
                serialPort.writeBytes(ackBytes, ackBytes.length);
                searchStart = enqIdx + 1;
                continue;
            }
            int stxIdx = indexOf(buffer, start, searchStart);
            if (stxIdx < 0) break;
            int payloadStart = stxIdx + start.length;
            int etxIdx = indexOf(buffer, end, payloadStart);
            if (etxIdx < 0) break;
            if (etxIdx + 4 >= buffer.length) break;
            byte[] payload = Arrays.copyOfRange(buffer, payloadStart, etxIdx);
            byte chk1 = buffer[etxIdx + end.length];
            byte chk2 = buffer[etxIdx + end.length + 1];
            if (buffer[etxIdx + end.length + 2] == 0x0D && buffer[etxIdx + end.length + 3] == 0x0A) {
                int sum = 0;
                for (byte b : payload) sum = (sum + b) & 0xFF;
                String expectedChk = String.format("%02X", sum).substring(0, 2);
                String actualChk = String.format("%02c%02c", (char) chk1, (char) chk2);
                if (expectedChk.equals(actualChk)) {
                    dispatchRaw(payload, config);
                    serialPort.writeBytes(ackBytes, ackBytes.length);
                } else {
                    logger.warn("ASTM checksum mismatch on " + config.getPortName());
                    serialPort.writeBytes(nakBytes, nakBytes.length);
                }
                searchStart = etxIdx + end.length + 4;
            } else {
                searchStart = stxIdx + 1;
            }
        }
        if (searchStart > 0) {
            byte[] remaining = Arrays.copyOfRange(buffer, searchStart, buffer.length);
            frameBuffer.reset();
            frameBuffer.write(remaining, 0, remaining.length);
        }
    }

    private String bytesToPayload(byte[] data, SerialPortConfig config) {
        if (config.isBinaryMode()) return java.util.Base64.getEncoder().encodeToString(data);
        return new String(data, Charset.forName(config.getCharset()));
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

    private int indexOf(byte[] haystack, byte[] needle, int fromIndex) {
        outer: for (int i = fromIndex; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private int indexOfByte(byte[] haystack, byte needle, int fromIndex) {
        for (int i = fromIndex; i < haystack.length; i++) {
            if (haystack[i] == needle) return i;
        }
        return -1;
    }

    private void healthLoop() {
        SerialPortConfig config = connectorProperties.getPortConfig();
        int maxReconnect = config.getMaxReconnects();
        int reconnectDelay = config.getReconnectDelay();
        int reconnectAttempts = 0;
        while (running.get()) {
            try {
                Thread.sleep(reconnectDelay);
                if (!running.get()) break;
                boolean portDown = (serialPort == null || !serialPort.isOpen());
                boolean readerDead = (readerThread.get() == null || !readerThread.get().isAlive());
                if (portDown || readerDead) {
                    if (reconnectAttempts < maxReconnect) {
                        logger.warn("Reconnecting... (" + (reconnectAttempts + 1) + "/" + maxReconnect +
                                    ") portDown=" + portDown + " readerDead=" + readerDead);
                        try {
                            closePort();
                            openPort();
                            stopReader();
                            startReader();
                            reconnectAttempts = 0;
                        } catch (Exception e) {
                            reconnectAttempts++;
                            logger.error("Reconnect failed: " + e.getMessage());
                        }
                    } else {
                        logger.error("Max reconnects reached.");
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
