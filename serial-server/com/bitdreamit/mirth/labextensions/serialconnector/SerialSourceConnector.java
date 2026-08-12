package com.bitdreamit.mirth.labextensions.serialconnector;

import com.fazecast.jSerialComm.SerialPort;
import com.mirth.connect.donkey.model.message.RawMessage;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Universal Serial Source Connector — Transport layer only.
 * Works with ANY Mirth DataType (HL7, ASTM, Delimited, XML, etc.).
 * Transmission modes: BASIC | MLLP | ASTM_E1381
 */
public class SerialSourceConnector extends SourceConnector {
    private static final Logger logger = Logger.getLogger(SerialSourceConnector.class);

    // ASTM / MLLP control characters
    private static final byte ENQ = 0x05;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final byte EOT = 0x04;
    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte ETB = 0x17;
    private static final byte CR  = 0x0D;
    private static final byte LF  = 0x0A;
    private static final byte VT  = 0x0B;
    private static final byte FS  = 0x1C;

    private final SerialPortManager portManager = SerialPortManager.getInstance();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Thread> readerThreadRef = new AtomicReference<>();

    private SerialPort serialPort;
    private SerialPortConfig config;
    private SerialReceiverProperties props;
    private String portKey;
    private Thread healthThread;
    private SerialStatistics stats;
    private Charset charset;
    private SerialFrameAssembler frameAssembler;

    @Override
    public void onDeploy() {}

    @Override
    public void onUndeploy() {}

    @Override
    public void onStart() {
        props = (SerialReceiverProperties) getConnectorProperties();
        config = props.getPortConfig();
        portKey = getChannelId() + "@" + config.getPortName();
        charset = Charset.forName(config.getCharsetEncoding());
        frameAssembler = new SerialFrameAssembler(
            props.getTransmissionMode(),
            props.getMessageDelimiter(),
            config.getCharsetEncoding()
        );

        try {
            startReader();
            if (config.isEnableHealthMonitor()) {
                healthThread = new Thread(this::healthLoop, "BitDreamIT-SerialHealth-" + portKey);
                healthThread.start();
            }
            logger.info("Serial source started on " + portKey + " mode=" + props.getTransmissionMode());
        } catch (Exception e) {
            logger.error("Failed to start serial source on " + portKey, e);
            throw new RuntimeException("Failed to start serial source: " + e.getMessage(), e);
        }
    }

    private synchronized void startReader() throws Exception {
        if (readerThreadRef.get() != null && readerThreadRef.get().isAlive()) {
            return;
        }
        serialPort = portManager.getOrOpenPort(config, getChannelId());
        stats = portManager.getStatistics(portKey);
        running.set(true);
        Thread t = new Thread(this::readLoop, "BitDreamIT-SerialReader-" + portKey);
        readerThreadRef.set(t);
        t.start();
        logger.info("Reader thread started for " + portKey);
    }

    private void readLoop() {
        byte[] buffer = new byte[config.getBufferSize()];

        while (running.get()) {
            SerialPort localPort = serialPort;
            if (localPort == null || !localPort.isOpen()) {
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                continue;
            }

            try {
                int len = localPort.readBytes(buffer, buffer.length);
                if (len > 0) {
                    byte[] chunk = new byte[len];
                    System.arraycopy(buffer, 0, chunk, 0, len);

                    if (stats != null) stats.recordRead(len);
                    if (config.isEnableProtocolAnalyzer()) {
                        portManager.logProtocol(portKey, ProtocolLogEntry.Direction.IN, chunk,
                                "Read " + len + " bytes", config.getMaxProtocolLogEntries());
                    }

                    List<SerialFrameAssembler.Frame> frames = frameAssembler.process(chunk);
                    for (SerialFrameAssembler.Frame frame : frames) {
                        handleFrame(frame, localPort);
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    if (stats != null) stats.recordError();
                    logger.error("Serial read error on " + portKey, e);
                }
            }
        }
        logger.info("Serial read loop exited for " + portKey);
    }

    private void handleFrame(SerialFrameAssembler.Frame frame, SerialPort port) throws Exception {
        String mode = props.getTransmissionMode();

        if (frame.type == SerialFrameAssembler.Frame.Type.CONTROL) {
            // ASTM E1381: ENQ -> ACK
            if ("ASTM_E1381".equals(mode) && frame.bytes.length == 1 && frame.bytes[0] == ENQ) {
                port.writeBytes(new byte[]{ACK}, 1);
                if (config.isEnableProtocolAnalyzer()) {
                    portManager.logProtocol(portKey, ProtocolLogEntry.Direction.OUT, new byte[]{ACK},
                            "ASTM ACK -> ENQ", config.getMaxProtocolLogEntries());
                }
                logger.debug("Sent ACK in response to ENQ on " + portKey);
            }
            return;
        }

        // DATA frame
        String payload;
        if ("ASTM_E1381".equals(mode)) {
            // Send ACK for each ASTM data frame
            port.writeBytes(new byte[]{ACK}, 1);
            if (config.isEnableProtocolAnalyzer()) {
                portManager.logProtocol(portKey, ProtocolLogEntry.Direction.OUT, new byte[]{ACK},
                        "ASTM ACK -> DATA", config.getMaxProtocolLogEntries());
            }
            payload = extractASTMPayload(frame.bytes, charset);
        } else if ("MLLP".equals(mode)) {
            payload = extractMLLPPayload(frame.bytes, charset);
        } else {
            // BASIC mode
            payload = config.isBinaryMode() ? bytesToHex(frame.bytes) : new String(frame.bytes, charset);
        }

        if (payload != null && !payload.isEmpty()) {
            dispatchRawMessage(new RawMessage(payload));
        }
    }

    private String extractASTMPayload(byte[] frame, Charset charset) {
        if (frame == null || frame.length < 7) return "";
        // frame: STX [seq] payload [ETX|ETB] CHK1 CHK2 CR LF
        int end = frame.length - 5; // before checksum
        if (end < 2) return "";
        int start = 2; // after STX and sequence number
        if (start >= end) return "";
        return new String(frame, start, end - start, charset);
    }

    private String extractMLLPPayload(byte[] frame, Charset charset) {
        if (frame == null || frame.length < 3) return "";
        int start = 0;
        if (frame[0] == VT) start = 1;
        int end = frame.length;
        if (end >= 2 && frame[end - 2] == FS && frame[end - 1] == CR) end = end - 2;
        else if (end >= 1 && frame[end - 1] == FS) end = end - 1;
        if (start >= end) return "";
        return new String(frame, start, end - start, charset);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    private void healthLoop() {
        int attempts = 0;
        while (running.get()) {
            try {
                Thread.sleep(config.getHealthCheckInterval());
                SerialPort current = serialPort;
                if (current == null || !current.isOpen()) {
                    if (stats != null) stats.recordError();
                    logger.warn("Serial port " + portKey + " disconnected. Reconnecting...");

                    if (attempts < config.getMaxReconnectAttempts()) {
                        Thread.sleep(config.getReconnectDelay());
                        try {
                            portManager.closePort(portKey);
                            startReader();
                            attempts = 0;
                            logger.info("Serial port " + portKey + " reconnected and reader restarted");
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

    @Override
    public void onStop() {
        running.set(false);
        Thread rt = readerThreadRef.getAndSet(null);
        if (rt != null) {
            try { rt.join(2000); } catch (InterruptedException ignored) {}
        }
        if (healthThread != null) {
            try { healthThread.join(1000); } catch (InterruptedException ignored) {}
        }
        if (!props.isKeepConnectionOpen()) {
            portManager.closePort(portKey);
        }
        logger.info("Serial source stopped on " + portKey);
    }

    @Override
    public void onHalt() {
        running.set(false);
        portManager.closePort(portKey);
    }

    @Override
    public void handleRecoveredResponse(DispatchResult dispatchResult) {}
}
