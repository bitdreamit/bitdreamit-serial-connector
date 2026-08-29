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
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
 * XStream registration is handled by SerialServerPlugin.init(), NOT here.
 * This class focuses only on connector lifecycle.
 *
 * =========================================================================
 * FIX (NO-DATA BUG):
 * The old readLoop() consumed EVERY incoming byte with
 * serialPort.readBytes() (SEMI_BLOCKING) and then, when a Mirth
 * transmission-mode provider (e.g. ASTM E1381) was selected, created a
 * StreamHandler over serialPort.getInputStream() — a SECOND reader on the
 * same port. The handler never saw the already-consumed bytes, waited for
 * ENQ until establishmentTimeout, threw, and the consumed chunk was
 * silently discarded. Result: connection OK but ZERO data received.
 *
 * jSerialComm rule: use EITHER readBytes() OR getInputStream() — never both.
 *
 * New design (matches the working monolithic bitdreamit-astm driver):
 *   - Provider mode (ASTM E1381 plugin): the StreamHandler is the ONLY
 *     reader. It owns serialPort.getInputStream() exclusively; readBytes()
 *     is NEVER called. ONE handler instance is created per connection so
 *     protocol state (session, frame numbering, retries) persists.
 *   - Built-in byte mode (RAW/LINE/FRAME/MLLP/ASTM fallback): readBytes()
 *     owns the port and the consumed bytes are processed directly.
 * =========================================================================
 */
public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);
    private EventController eventController = ControllerFactory.getFactory().createEventController();

    private SerialReceiverProperties connectorProperties;
    private SerialPort serialPort;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicReference<Thread> readerThread = new AtomicReference<>();
    private AtomicReference<Thread> healthThread = new AtomicReference<>();
    private ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream autoBuffer = new ByteArrayOutputStream();

    // PREMIUM: Statistics and protocol logger
    private SerialStatistics statistics = new SerialStatistics();
    private ProtocolLogger protocolLogger = null;

    // FIX: ONE StreamHandler per connection — protocol state (session established,
    // expected frame number, retry counters) must persist across messages and across
    // ENQ/EOT cycles. Never create a handler per read chunk.
    private com.mirth.connect.donkey.server.message.StreamHandler modeHandler = null;
    private SerialPort modeHandlerPort = null;

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

        // Initialize protocol logger if enabled
        SerialPortConfig config = connectorProperties.getPortConfig();
        if (config.isProtocolLoggingEnabled()) {
            protocolLogger = new ProtocolLogger(getChannelId(), config.getPortName(), config.getMaxLogEntries());
            logger.info("SerialSourceConnector.onDeploy: protocol logging ENABLED for channel " + getChannelId());
        }

        // Reset statistics on deploy
        statistics.reset();
        logger.info("SerialSourceConnector.onDeploy: properties loaded for channel " + getChannelId() +
                ", port=" + config.getPortName());
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
        // FIX: invalidate any cached StreamHandler — it must be recreated against the
        // freshly opened port so its InputStream is bound to the live connection.
        modeHandler = null;
        modeHandlerPort = null;
        eventController.dispatchEvent(new ConnectionStatusEvent(
                getChannelId(), getMetaDataId(), getConnectorProperties().getName(), ConnectionStatusEventType.CONNECTED));
        logger.info("Serial source connected on " + connectorProperties.getPortConfig().getPortName());
    }

    private void closePort() {
        if (serialPort != null) {
            SerialPortManager.releasePort(serialPort.getSystemPortName(), true);
            serialPort = null;
            modeHandler = null;
            modeHandlerPort = null;
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

        // FIX: resolve mode + provider ONCE per connection (the old code did an
        // ExtensionController lookup on EVERY read chunk and then mis-routed the bytes).
        TransmissionModeProperties modeProps = connectorProperties.getTransmissionModeProperties();
        String modeName = (modeProps != null) ? modeProps.getPluginPointName() : config.getTransmissionMode();
        if (modeName == null || modeName.isEmpty()) {
            modeName = "RAW";
        }

        com.mirth.connect.plugins.TransmissionModeProvider provider = null;
        try {
            com.mirth.connect.server.controllers.ExtensionController extController =
                    com.mirth.connect.server.controllers.ControllerFactory.getFactory().createExtensionController();
            java.util.Map<String, com.mirth.connect.plugins.TransmissionModeProvider> providers =
                    extController.getTransmissionModeProviders();
            provider = providers.get(modeName);
        } catch (Throwable t) {
            logger.warn("Could not look up Mirth transmission mode provider for '" + modeName +
                    "': " + t.getMessage() + " — falling back to built-in mode handling");
            provider = null;
        }

        if (provider != null) {
            if (modeProps == null) {
                // Channel stored only the mode name (no polymorphic properties).
                // FIX (compile): Mirth's abstract TransmissionModeProvider does NOT
                // declare getDefaultProperties() — the concrete plugin class defines
                // it as an extra method, so resolve it reflectively at runtime.
                modeProps = resolveDefaultProperties(provider);
            }
            if (modeProps != null) {
                logger.info("Serial source using Mirth transmission mode provider: " + modeName);
                if (providerReadLoop(provider, modeProps, config)) {
                    // Provider mode ran until the channel stopped — done.
                    return;
                }
                logger.warn("Transmission mode '" + modeName + "' could not be initialized — " +
                        "falling back to built-in byte mode");
            } else {
                logger.warn("No transmission-mode properties available for '" + modeName +
                        "' — falling back to built-in byte mode");
            }
        }

        // ===== BUILT-IN BYTE MODE (RAW/LINE/FRAME/MLLP/ASTM fallback) =====
        // readBytes() is the single owner of the port in this mode.
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

                    // PREMIUM: Record statistics
                    statistics.recordRead(bytesRead);

                    // PREMIUM: Log protocol traffic
                    if (protocolLogger != null) {
                        protocolLogger.logIn(data, "raw read");
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Serial read " + bytesRead + " bytes from " + config.getPortName() +
                                " (total: " + statistics.getBytesRead() + " bytes, " +
                                statistics.getMessagesReceived() + " msgs)");
                    }
                    processBuiltIn(data, config);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Serial read error on " + config.getPortName(), e);
                statistics.recordError();
                eventController.dispatchEvent(new ErrorEvent(
                        getChannelId(), getMetaDataId(), null, ErrorEventType.SOURCE_CONNECTOR,
                        getConnectorProperties().getName(), "Serial read error", e.getMessage(), e));
            }
        }
    }

    /**
     * FIX: Provider-driven read loop (e.g. ASTM E1381 plugin).
     *
     * The StreamHandler is the ONLY reader of the port: it reads byte-by-byte from
     * serialPort.getInputStream() (exactly like the working monolithic driver does via
     * AbstractAstmConnection.doGetInputStream().read()). serialPort.readBytes() is
     * NEVER called in this mode, so the handler finally receives ENQ, frames, EOT.
     *
     * ONE handler instance is kept per connection: session state, frame numbering and
     * retry counters survive across messages, ENQ/EOT cycles and NAK retries.
     *
     * IOException from handler.read() (establishment/frame timeouts, protocol aborts)
     * is treated as RECOVERABLE: the port stays open, the handler keeps its state,
     * the loop retries after a short pause. Only a stopped channel exits the loop.
     */
    private boolean providerReadLoop(com.mirth.connect.plugins.TransmissionModeProvider provider,
                                     TransmissionModeProperties modeProps,
                                     SerialPortConfig config) {
        Charset cs = Charset.forName(config.getCharset());
        int handlerInitFailures = 0;

        while (running.get()) {
            try {
                if (serialPort == null || !serialPort.isOpen()) {
                    modeHandler = null;
                    modeHandlerPort = null;
                    Thread.sleep(100);
                    continue;
                }

                // (Re)create the handler ONLY when the port instance changed —
                // never per read chunk, otherwise the ASTM state machine resets.
                if (modeHandler == null || modeHandlerPort != serialPort) {
                    if (!createModeHandler(provider, modeProps, config)) {
                        handlerInitFailures++;
                        if (handlerInitFailures >= 3) {
                            // Provider cannot build a handler (e.g. properties type
                            // mismatch) — give up and let readLoop fall back to
                            // built-in byte mode so data can still flow.
                            return false;
                        }
                        Thread.sleep(1000);
                        continue;
                    }
                    handlerInitFailures = 0;
                }

                byte[] messageBytes = modeHandler.read();

                if (messageBytes != null && messageBytes.length > 0) {
                    if (protocolLogger != null) {
                        protocolLogger.logIn(messageBytes, "framed by " + modeProps.getPluginPointName());
                    }

                    String message = new String(messageBytes, cs);
                    try {
                        dispatchRawMessage(new RawMessage(message));
                        statistics.recordMessageReceived();
                    } catch (ChannelException e) {
                        logger.error("Failed to dispatch message via " +
                                modeProps.getPluginPointName() + " provider", e);
                        statistics.recordError();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (!running.get()) break;
                // Recoverable: session establishment timeout (idle line), frame timeout,
                // max transfer attempts, sender cancel (EOT). Handler state is kept.
                logger.debug("Transmission mode '" + modeProps.getPluginPointName() +
                        "' read cycle: " + e.getMessage());
                statistics.recordError();
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                if (!running.get()) break;
                logger.error("Serial read error on " + config.getPortName(), e);
                statistics.recordError();
                eventController.dispatchEvent(new ErrorEvent(
                        getChannelId(), getMetaDataId(), null, ErrorEventType.SOURCE_CONNECTOR,
                        getConnectorProperties().getName(), "Serial read error", e.getMessage(), e));
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Provider read loop exiting for channel " + getChannelId());
        return true;
    }

    /**
     * FIX (compile error "cannot find symbol: getDefaultProperties"):
     * Mirth's abstract com.mirth.connect.plugins.TransmissionModeProvider only
     * declares getPluginPointName() and getStreamHandler(...). The concrete
     * plugin class (e.g. ASTME1381TransmissionModePlugin) adds
     * getDefaultProperties() as an extra public method. serial-connector must
     * stay compile-independent of the E1381 plugin, so the method is resolved
     * reflectively at runtime.
     *
     * @return the provider's default properties, or null when the provider does
     *         not expose them (caller falls back to built-in byte mode).
     */
    private TransmissionModeProperties resolveDefaultProperties(
            com.mirth.connect.plugins.TransmissionModeProvider provider) {
        try {
            java.lang.reflect.Method m = provider.getClass().getMethod("getDefaultProperties");
            Object result = m.invoke(provider);
            if (result instanceof TransmissionModeProperties) {
                logger.info("Using provider default properties: " + result.getClass().getName());
                return (TransmissionModeProperties) result;
            }
        } catch (Throwable t) {
            logger.debug("Provider " + provider.getClass().getName() +
                    " does not expose getDefaultProperties(): " + t.getMessage());
        }
        return null;
    }

    /**
     * Creates the StreamHandler for the current port.
     *
     * @return true on success; false on ANY failure (port not open, null streams,
     *         or the provider rejecting the properties) — caller counts failures
     *         and falls back to built-in byte mode after 3 strikes.
     */
    private boolean createModeHandler(com.mirth.connect.plugins.TransmissionModeProvider provider,
                                      TransmissionModeProperties modeProps,
                                      SerialPortConfig config) {
        try {
            if (serialPort == null || !serialPort.isOpen()) {
                return false;
            }
            java.io.InputStream in = serialPort.getInputStream();
            java.io.OutputStream out = serialPort.getOutputStream();
            if (in == null || out == null) {
                logger.warn("Serial port " + config.getPortName() + " returned null stream(s)");
                return false;
            }
            modeHandler = provider.getStreamHandler(in, out, null, modeProps);
            modeHandlerPort = serialPort;
            logger.info("StreamHandler created for transmission mode: " +
                    modeProps.getPluginPointName());
            return true;
        } catch (Throwable t) {
            logger.error("Failed to create StreamHandler for '" +
                    modeProps.getPluginPointName() + "': " +
                    t.getClass().getName() + ": " + t.getMessage(), t);
            modeHandler = null;
            modeHandlerPort = null;
            return false;
        }
    }

    // ===== BUILT-IN MODE PROCESSING (byte mode) =====
    // The bytes passed in here were consumed by readBytes() in readLoop() —
    // this path owns them. Provider modes never reach this method.

    private void processBuiltIn(byte[] data, SerialPortConfig config) throws Exception {
        TransmissionModeProperties modeProps = connectorProperties.getTransmissionModeProperties();
        String modeName = (modeProps != null) ? modeProps.getPluginPointName() : config.getTransmissionMode();

        if (modeName == null || modeName.isEmpty()) {
            modeName = "RAW";
        }

        switch (modeName.toUpperCase()) {
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

    // ===== AUTO-DETECT MODE =====
    // Smart detection: examines byte patterns and routes to the correct handler.
    // Supports MLLP, ASTM, Frame, LINE, and RAW — all detected dynamically.

    private static final byte VT  = 0x0B; // MLLP start
    private static final byte FS  = 0x1C; // MLLP end part 1
    private static final byte CR  = 0x0D;
    private static final byte LF  = 0x0A;
    private static final byte STX = 0x02; // ASTM/Frame start
    private static final byte ETX = 0x03; // ASTM/Frame end
    private static final byte ENQ = 0x05; // ASTM inquiry
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final byte EOT = 0x04;

    /** Tracks detected mode so we don't re-analyze every chunk. */
    private String autoDetectedMode = null;

    private void processAutoDetect(byte[] data, SerialPortConfig config) throws Exception {
        if (data == null || data.length == 0) return;

        // Buffer the data
        autoBuffer.write(data, 0, data.length);
        byte[] buf = autoBuffer.toByteArray();

        // If we already detected the mode, keep using it
        if (autoDetectedMode != null) {
            processAutoWithKnownMode(buf, config);
            return;
        }

        // --- Detect mode by examining first significant byte ---
        // Find the first non-null byte
        int firstByteIdx = 0;
        while (firstByteIdx < buf.length && buf[firstByteIdx] == 0) {
            firstByteIdx++;
        }
        if (firstByteIdx >= buf.length) return; // all nulls, wait for more

        byte firstByte = buf[firstByteIdx];
        Charset cs = Charset.forName(config.getCharset());

        if (firstByte == VT) {
            // 0x0B → MLLP mode
            autoDetectedMode = "MLLP";
            logger.info("AUTO-DETECT: detected MLLP mode (VT byte 0x0B)");
            // Send remaining MLLP config defaults
            processMllpMode(buf, config);

        } else if (firstByte == STX) {
            // 0x02 → could be ASTM or generic Frame
            // Check if there's a checksum after ETX (2 hex chars + CRLF = ASTM)
            autoDetectedMode = "ASTM";
            logger.info("AUTO-DETECT: detected ASTM mode (STX byte 0x02)");
            processAstmMode(buf, config);

        } else if (firstByte == ENQ) {
            // 0x05 → ASTM ENQ (instrument wants to talk)
            autoDetectedMode = "ASTM";
            logger.info("AUTO-DETECT: detected ASTM mode (ENQ byte 0x05)");
            // Send ACK to allow the instrument to start sending
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.writeBytes(new byte[]{ACK}, 1);
                if (protocolLogger != null) protocolLogger.logOut(new byte[]{ACK}, "AUTO-ACK (ENQ response)");
            }
            // Remove the ENQ byte from buffer and continue
            autoBuffer.reset();
            if (buf.length > 1) {
                autoBuffer.write(buf, 1, buf.length - 1);
            }

        } else if (firstByte == EOT) {
            // 0x04 → End of Transmission (ASTM), just discard
            autoBuffer.reset();
            if (firstByteIdx + 1 < buf.length) {
                autoBuffer.write(buf, firstByteIdx + 1, buf.length - firstByteIdx - 1);
            }

        } else {
            // Check for LINE mode: look for \r\n in the data
            boolean hasCRLF = false;
            for (int i = 0; i < buf.length - 1; i++) {
                if (buf[i] == CR && buf[i + 1] == LF) {
                    hasCRLF = true;
                    break;
                }
            }

            if (hasCRLF) {
                autoDetectedMode = "LINE";
                logger.info("AUTO-DETECT: detected LINE mode (CRLF found)");
                processLineMode(buf, config);
            } else {
                // No known framing detected — check if we have enough data
                // If buffer is large or hasn't received data for a while, dispatch as RAW
                if (buf.length >= config.getBufferSize()) {
                    // Buffer full, no framing detected — dispatch as RAW
                    autoDetectedMode = "RAW";
                    logger.info("AUTO-DETECT: detected RAW mode (no framing, buffer full)");
                    dispatchRaw(buf, config);
                    autoBuffer.reset();
                }
                // Otherwise wait for more data
            }
        }
    }

    /** Process data using a previously detected mode. */
    private void processAutoWithKnownMode(byte[] buf, SerialPortConfig config) throws Exception {
        switch (autoDetectedMode) {
            case "MLLP":
                processMllpMode(buf, config);
                break;
            case "ASTM":
                // Also handle ENQ/EOT in the stream
                int i = 0;
                ByteArrayOutputStream cleanBuf = new ByteArrayOutputStream();
                while (i < buf.length) {
                    byte b = buf[i];
                    if (b == ENQ) {
                        // Respond to ENQ with ACK
                        if (serialPort != null && serialPort.isOpen()) {
                            serialPort.writeBytes(new byte[]{ACK}, 1);
                            if (protocolLogger != null) protocolLogger.logOut(new byte[]{ACK}, "AUTO-ACK (ENQ)");
                        }
                        i++;
                    } else if (b == EOT) {
                        // Skip EOT
                        i++;
                    } else if (b == ACK || b == NAK) {
                        // Skip ACK/NAK from instrument
                        i++;
                    } else {
                        cleanBuf.write(b);
                        i++;
                    }
                }
                byte[] cleanData = cleanBuf.toByteArray();
                if (cleanData.length > 0) {
                    // Process as ASTM (STX...ETX + checksum)
                    processAstmMode(cleanData, config);
                } else {
                    autoBuffer.reset();
                }
                break;
            case "LINE":
                processLineMode(buf, config);
                break;
            case "FRAME":
                processFrameMode(buf, config);
                break;
            default:
                dispatchRaw(buf, config);
                autoBuffer.reset();
                break;
        }
    }

    private void dispatchRaw(byte[] data, SerialPortConfig config) {
        String payload = bytesToPayload(data, config);
        try {
            dispatchRawMessage(new RawMessage(payload));
            statistics.recordMessageReceived();
        } catch (ChannelException e) {
            logger.error("Failed to dispatch raw message", e);
            statistics.recordError();
        }
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
                        statistics.recordReconnect();
                        try {
                            closePort();
                            openPort();
                            stopReader();
                            startReader();
                            reconnectAttempts = 0;
                        } catch (Exception e) {
                            reconnectAttempts++;
                            statistics.recordError();
                            logger.error("Reconnect failed: " + e.getMessage());
                        }
                    } else {
                        logger.error("Max reconnects reached. Stats: " + statistics);
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
