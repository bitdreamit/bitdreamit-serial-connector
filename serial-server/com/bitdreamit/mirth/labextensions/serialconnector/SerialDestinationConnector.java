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

/**
 * Universal Serial Destination Connector — Transport layer only.
 * Works with ANY Mirth DataType (HL7, ASTM, Delimited, XML, etc.).
 * Transmission modes: BASIC | MLLP | ASTM_E1381
 */
public class SerialDestinationConnector extends DestinationConnector {
    private static final Logger logger = Logger.getLogger(SerialDestinationConnector.class);

    // ASTM / MLLP control characters
    private static final byte ENQ = 0x05;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final byte EOT = 0x04;
    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte CR  = 0x0D;
    private static final byte LF  = 0x0A;
    private static final byte VT  = 0x0B;
    private static final byte FS  = 0x1C;

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
        logger.info("Serial destination started for channel " + getChannelId() + " mode=" + props.getTransmissionMode());
    }

    @Override
    public void replaceConnectorProperties(ConnectorProperties properties, ConnectorMessage message) {}

    @Override
    public Response send(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) throws InterruptedException {
        SerialDispatcherProperties currentProps = (SerialDispatcherProperties) connectorProperties;
        SerialPortConfig currentConfig = currentProps.getPortConfig();
        String resolvedPort = replacer.replaceValues(currentConfig.getPortName(), getChannelId(), getChannelId());
        String currentPortKey = getChannelId() + "@" + resolvedPort;
        String mode = currentProps.getTransmissionMode();

        try {
            SerialPort port = portManager.getOrOpenPort(currentConfig, getChannelId());
            SerialStatistics stats = portManager.getStatistics(currentPortKey);

            if (currentConfig.isWaitForCTS() || currentConfig.isWaitForDSR() || currentConfig.isWaitForDCD()) {
                boolean ok = portManager.waitForSignals(port, currentConfig);
                if (!ok) {
                    return new Response("ERROR: Serial signals not ready (CTS/DSR/DCD)");
                }
            }

            String payload = connectorMessage.getEncoded() != null
                    ? connectorMessage.getEncoded().getContent()
                    : "";

            byte[] bytesToWrite;
            if ("ASTM_E1381".equals(mode)) {
                bytesToWrite = buildASTMFrame(payload, currentConfig);
            } else if ("MLLP".equals(mode)) {
                bytesToWrite = buildMLLPFrame(payload, currentConfig);
            } else {
                // BASIC mode
                if (currentConfig.isBinaryMode()) {
                    bytesToWrite = hexToBytes(payload.replaceAll("\\s", ""));
                } else {
                    bytesToWrite = payload.getBytes(Charset.forName(currentConfig.getCharsetEncoding()));
                }
                // Append delimiter if configured
                String delim = currentProps.getMessageDelimiter();
                if (delim != null && !delim.isEmpty()) {
                    String d = delim.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
                    byte[] delimBytes = d.getBytes(Charset.forName(currentConfig.getCharsetEncoding()));
                    byte[] combined = new byte[bytesToWrite.length + delimBytes.length];
                    System.arraycopy(bytesToWrite, 0, combined, 0, bytesToWrite.length);
                    System.arraycopy(delimBytes, 0, combined, bytesToWrite.length, delimBytes.length);
                    bytesToWrite = combined;
                }
            }

            if (currentConfig.isEnableProtocolAnalyzer()) {
                portManager.logProtocol(currentPortKey, ProtocolLogEntry.Direction.OUT, bytesToWrite,
                        "Write " + bytesToWrite.length + " bytes mode=" + mode, currentConfig.getMaxProtocolLogEntries());
            }

            int written = port.writeBytes(bytesToWrite, bytesToWrite.length);
            if (written < bytesToWrite.length) {
                if (stats != null) stats.recordError();
                return new Response("ERROR: Serial write incomplete: " + written + "/" + bytesToWrite.length);
            }
            if (stats != null) stats.recordWrite(written);

            // ACK check for BASIC mode (if configured) and ASTM_E1381
            if (currentProps.isWaitForAckAfterWrite() || "ASTM_E1381".equals(mode)) {
                byte[] ackBuf = new byte[currentProps.getAckPattern().length];
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, currentProps.getAckTimeout(), 0);
                int read = port.readBytes(ackBuf, ackBuf.length);
                if (read != ackBuf.length || !Arrays.equals(ackBuf, currentProps.getAckPattern())) {
                    if (stats != null) stats.recordError();
                    return new Response("ERROR: ACK not received or mismatch after write");
                }
                if (currentConfig.isEnableProtocolAnalyzer()) {
                    portManager.logProtocol(currentPortKey, ProtocolLogEntry.Direction.IN, ackBuf,
                            "ACK received", currentConfig.getMaxProtocolLogEntries());
                }
            }

            if (!currentProps.isKeepConnectionOpen()) {
                portManager.closePort(currentPortKey);
            }

            return new Response("Serial write successful: " + written + " bytes");

        } catch (Exception e) {
            SerialStatistics stats = portManager.getStatistics(currentPortKey);
            if (stats != null) stats.recordError();
            logger.error("Serial write error on " + currentPortKey, e);
            return new Response("ERROR: " + e.getMessage());
        }
    }

    private byte[] buildMLLPFrame(String payload, SerialPortConfig config) {
        Charset cs = Charset.forName(config.getCharsetEncoding());
        byte[] data = payload.getBytes(cs);
        byte[] frame = new byte[data.length + 3]; // VT + data + FS + CR
        frame[0] = VT;
        System.arraycopy(data, 0, frame, 1, data.length);
        frame[frame.length - 2] = FS;
        frame[frame.length - 1] = CR;
        return frame;
    }

    private byte[] buildASTMFrame(String payload, SerialPortConfig config) {
        Charset cs = Charset.forName(config.getCharsetEncoding());
        byte[] data = payload.getBytes(cs);
        // STX [seq=1] payload ETX CHK1 CHK2 CR LF
        byte[] frame = new byte[data.length + 7];
        frame[0] = STX;
        frame[1] = '1'; // sequence number
        System.arraycopy(data, 0, frame, 2, data.length);
        int chkPos = 2 + data.length;
        frame[chkPos] = ETX;
        // Simple checksum: sum of bytes between STX and ETX inclusive, mod 256
        int sum = 0;
        for (int i = 1; i <= chkPos; i++) sum += frame[i] & 0xFF;
        sum &= 0xFF;
        frame[chkPos + 1] = (byte) (((sum >> 4) & 0x0F) + '0'); // high nibble as hex char
        frame[chkPos + 2] = (byte) ((sum & 0x0F) + '0');         // low nibble as hex char
        frame[chkPos + 3] = CR;
        frame[chkPos + 4] = LF;
        return frame;
    }

    private byte[] hexToBytes(String hex) {
        String h = hex.replaceAll("\\s", "");
        int len = h.length();
        if (len % 2 != 0) throw new IllegalArgumentException("Hex string must have even length");
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(h.charAt(i), 16) << 4)
                                 + Character.digit(h.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public void onStop() {
        if (props != null) {
            String resolvedPort = replacer.replaceValues(props.getPortConfig().getPortName(), getChannelId(), getChannelId());
            portManager.closePort(getChannelId() + "@" + resolvedPort);
        }
        logger.info("Serial destination stopped for channel " + getChannelId());
    }

    @Override
    public void onHalt() {
        if (props != null) {
            String resolvedPort = replacer.replaceValues(props.getPortConfig().getPortName(), getChannelId(), getChannelId());
            portManager.closePort(getChannelId() + "@" + resolvedPort);
        }
    }
}
