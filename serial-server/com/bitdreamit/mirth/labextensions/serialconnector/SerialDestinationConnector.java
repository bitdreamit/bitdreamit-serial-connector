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
                throw new Exception("Failed to write to serial port");
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
                    throw new Exception("ACK timeout or mismatch");
                }
            }

            if (!pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }

            return new Response(Status.SENT, "Sent " + written + " bytes");

        } catch (Exception e) {
            logger.error("Serial write error", e);
            eventController.dispatchEvent(new ErrorEvent(
                    getChannelId(), getMetaDataId(), message.getMessageId(), ErrorEventType.DESTINATION_CONNECTOR,
                    getConnectorProperties().getName(), "Serial write error", e.getMessage(), e));
            if (port != null && !pooled) {
                SerialPortManager.releasePort(port.getSystemPortName(), true);
            }
            return new Response(Status.ERROR, "Serial write failed: " + e.getMessage());
        }
    }

    private byte[] buildFrame(String payload, SerialPortConfig config) throws Exception {
        String mode = config.getTransmissionMode();
        Charset charset = Charset.forName(config.getCharset());
        byte[] payloadBytes = config.isBinaryMode()
                ? java.util.Base64.getDecoder().decode(payload)
                : payload.getBytes(charset);

        switch (mode) {
            case "RAW":
                return payloadBytes;

            case "LINE":
                String delimiter = unescapeDelimiter(config.getLineDelimiter());
                byte[] delimBytes = delimiter.getBytes(charset);
                byte[] lineResult = new byte[payloadBytes.length + delimBytes.length];
                System.arraycopy(payloadBytes, 0, lineResult, 0, payloadBytes.length);
                System.arraycopy(delimBytes, 0, lineResult, payloadBytes.length, delimBytes.length);
                return lineResult;

            case "FRAME":
                byte[] start = parseHexString(config.getFrameStartBytes());
                byte[] end = parseHexString(config.getFrameEndBytes());
                byte[] frameResult = new byte[start.length + payloadBytes.length + end.length];
                System.arraycopy(start, 0, frameResult, 0, start.length);
                System.arraycopy(payloadBytes, 0, frameResult, start.length, payloadBytes.length);
                System.arraycopy(end, 0, frameResult, start.length + payloadBytes.length, end.length);
                return frameResult;

            case "MLLP":
                byte[] mllpResult = new byte[1 + payloadBytes.length + 2];
                mllpResult[0] = 0x0B;
                System.arraycopy(payloadBytes, 0, mllpResult, 1, payloadBytes.length);
                mllpResult[mllpResult.length - 2] = 0x1C;
                mllpResult[mllpResult.length - 1] = 0x0D;
                return mllpResult;

            case "ASTM":
                int sum = 0;
                for (byte b : payloadBytes) sum = (sum + b) & 0xFF;
                String chkStr = String.format("%02X", sum).substring(0, 2);
                byte[] chkBytes = chkStr.getBytes(charset);
                byte[] astmResult = new byte[1 + payloadBytes.length + 1 + 2 + 2];
                astmResult[0] = 0x02;
                System.arraycopy(payloadBytes, 0, astmResult, 1, payloadBytes.length);
                astmResult[1 + payloadBytes.length] = 0x03;
                astmResult[1 + payloadBytes.length + 1] = chkBytes[0];
                astmResult[1 + payloadBytes.length + 2] = chkBytes[1];
                astmResult[astmResult.length - 2] = 0x0D;
                astmResult[astmResult.length - 1] = 0x0A;
                return astmResult;

            default:
                return payloadBytes;
        }
    }

    private String unescapeDelimiter(String delim) {
        if (delim == null) return "\r\n";
        return delim.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
    }

    private byte[] parseHexString(String hex) {
        if (hex == null || hex.trim().isEmpty()) return new byte[0];
        String clean = hex.replaceAll("\\s", "");
        if (clean.length() % 2 != 0) clean = "0" + clean;
        byte[] result = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return result;
    }
}