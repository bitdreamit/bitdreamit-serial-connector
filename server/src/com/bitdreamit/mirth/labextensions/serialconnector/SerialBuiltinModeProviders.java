package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Built-in server-side serial transmission mode providers.
 *
 * Registers all built-in modes (RAW, LINE, FRAME, MLLP, ASTM) into the
 * SerialTransmissionModeRegistry at startup.
 *
 * Each mode is a separate inner class so new modes can be added easily
 * without modifying the connector classes.
 */
public class SerialBuiltinModeProviders {

    // ===== RAW mode =====

    public static class RawProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "RAW";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                   SerialPortConfig config) throws Exception {
            Charset cs = Charset.forName(config.getCharset());
            return config.isBinaryMode()
                    ? java.util.Base64.getDecoder().decode(payload)
                    : payload.getBytes(cs);
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                      SerialPortConfig config) throws Exception {
            // RAW mode: every read is a message
            Charset cs = Charset.forName(config.getCharset());
            return new String[]{ new String(data, cs) };
        }

        @Override
        public void reset() {}
    }

    // ===== LINE mode =====

    public static class LineProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "LINE";
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                   SerialPortConfig config) throws Exception {
            Charset cs = Charset.forName(config.getCharset());
            byte[] payloadBytes = payload.getBytes(cs);
            String delimiter = unescape(config.getLineDelimiter());
            byte[] delimBytes = delimiter.getBytes(cs);
            byte[] result = new byte[payloadBytes.length + delimBytes.length];
            System.arraycopy(payloadBytes, 0, result, 0, payloadBytes.length);
            System.arraycopy(delimBytes, 0, result, payloadBytes.length, delimBytes.length);
            return result;
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                      SerialPortConfig config) throws Exception {
            buffer.write(data, 0, data.length);
            String delimiter = unescape(config.getLineDelimiter());
            Charset cs = Charset.forName(config.getCharset());
            String text = new String(buffer.toByteArray(), cs);
            List<String> messages = new ArrayList<>();
            int idx;
            while ((idx = text.indexOf(delimiter)) >= 0) {
                String line = text.substring(0, idx);
                text = text.substring(idx + delimiter.length());
                if (!line.isEmpty()) messages.add(line);
            }
            buffer.reset();
            if (!text.isEmpty()) {
                byte[] remaining = text.getBytes(cs);
                buffer.write(remaining, 0, remaining.length);
            }
            return messages.toArray(new String[0]);
        }

        @Override
        public void reset() {
            buffer.reset();
        }

        private String unescape(String delim) {
            if (delim == null) return "\r\n";
            return delim.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
        }
    }

    // ===== FRAME mode =====

    public static class FrameProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "FRAME";
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                   SerialPortConfig config) throws Exception {
            Charset cs = Charset.forName(config.getCharset());
            byte[] payloadBytes = payload.getBytes(cs);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            byte[] result = new byte[start.length + payloadBytes.length + end.length];
            System.arraycopy(start, 0, result, 0, start.length);
            System.arraycopy(payloadBytes, 0, result, start.length, payloadBytes.length);
            System.arraycopy(end, 0, result, start.length + payloadBytes.length, end.length);
            return result;
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                      SerialPortConfig config) throws Exception {
            buffer.write(data, 0, data.length);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            if (start.length == 0 || end.length == 0) return new String[0];

            byte[] buf = buffer.toByteArray();
            List<String> messages = new ArrayList<>();
            int searchStart = 0;
            while (true) {
                int frameStart = indexOf(buf, start, searchStart);
                if (frameStart < 0) break;
                int payloadStart = frameStart + start.length;
                int frameEnd = indexOf(buf, end, payloadStart);
                if (frameEnd < 0) break;
                byte[] payload = Arrays.copyOfRange(buf, payloadStart, frameEnd);
                messages.add(new String(payload, Charset.forName(config.getCharset())));
                searchStart = frameEnd + end.length;
            }
            if (searchStart > 0) {
                byte[] remaining = Arrays.copyOfRange(buf, searchStart, buf.length);
                buffer.reset();
                buffer.write(remaining, 0, remaining.length);
            }
            return messages.toArray(new String[0]);
        }

        @Override
        public void reset() {
            buffer.reset();
        }
    }

    // ===== MLLP mode =====

    public static class MllpProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "MLLP";
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                   SerialPortConfig config) throws Exception {
            Charset cs = Charset.forName(config.getCharset());
            byte[] payloadBytes = payload.getBytes(cs);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            if (start.length == 0) start = new byte[]{0x0B};
            if (end.length == 0) end = new byte[]{0x1C, 0x0D};
            byte[] result = new byte[start.length + payloadBytes.length + end.length];
            System.arraycopy(start, 0, result, 0, start.length);
            System.arraycopy(payloadBytes, 0, result, start.length, payloadBytes.length);
            System.arraycopy(end, 0, result, start.length + payloadBytes.length, end.length);
            return result;
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                      SerialPortConfig config) throws Exception {
            buffer.write(data, 0, data.length);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            if (start.length == 0) start = new byte[]{0x0B};
            if (end.length == 0) end = new byte[]{0x1C, 0x0D};

            byte[] buf = buffer.toByteArray();
            List<String> messages = new ArrayList<>();
            int searchStart = 0;
            while (true) {
                int startIdx = indexOf(buf, start, searchStart);
                if (startIdx < 0) break;
                int payloadStart = startIdx + start.length;
                int endIdx = indexOf(buf, end, payloadStart);
                if (endIdx < 0) break;
                byte[] payload = Arrays.copyOfRange(buf, payloadStart, endIdx);
                messages.add(new String(payload, Charset.forName(config.getCharset())));
                searchStart = endIdx + end.length;
            }
            if (searchStart > 0) {
                byte[] remaining = Arrays.copyOfRange(buf, searchStart, buf.length);
                buffer.reset();
                buffer.write(remaining, 0, remaining.length);
            }
            return messages.toArray(new String[0]);
        }

        @Override
        public boolean sendsAck() {
            return true;
        }

        @Override
        public byte[] buildAck(String payload, SerialTransmissionModeProperties props,
                               SerialPortConfig config) throws Exception {
            byte[] ack = parseHex(config.getCommitAckBytes());
            return ack.length > 0 ? ack : new byte[]{0x06};
        }

        @Override
        public void reset() {
            buffer.reset();
        }
    }

    // ===== ASTM mode =====

    public static class AstmProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "ASTM";
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                   SerialPortConfig config) throws Exception {
            Charset cs = Charset.forName(config.getCharset());
            byte[] payloadBytes = payload.getBytes(cs);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            if (start.length == 0) start = new byte[]{0x02};
            if (end.length == 0) end = new byte[]{0x03};

            // Calculate checksum
            byte[] chkBytes = calculateChecksum(payloadBytes, config.getChecksumAlgorithm(), cs);
            byte[] crlf = new byte[]{0x0D, 0x0A};
            byte[] result = new byte[start.length + payloadBytes.length + end.length + chkBytes.length + crlf.length];
            int pos = 0;
            System.arraycopy(start, 0, result, pos, start.length); pos += start.length;
            System.arraycopy(payloadBytes, 0, result, pos, payloadBytes.length); pos += payloadBytes.length;
            System.arraycopy(end, 0, result, pos, end.length); pos += end.length;
            System.arraycopy(chkBytes, 0, result, pos, chkBytes.length); pos += chkBytes.length;
            System.arraycopy(crlf, 0, result, pos, crlf.length);
            return result;
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                      SerialPortConfig config) throws Exception {
            buffer.write(data, 0, data.length);
            byte[] start = parseHex(config.getStartOfMessageBytes());
            byte[] end = parseHex(config.getEndOfMessageBytes());
            if (start.length == 0) start = new byte[]{0x02};
            if (end.length == 0) end = new byte[]{0x03};
            byte[] ackBytes = parseHex(config.getCommitAckBytes());
            if (ackBytes.length == 0) ackBytes = new byte[]{0x06};
            byte[] nakBytes = parseHex(config.getCommitNakBytes());
            if (nakBytes.length == 0) nakBytes = new byte[]{0x15};

            byte[] buf = buffer.toByteArray();
            List<String> messages = new ArrayList<>();
            int searchStart = 0;
            while (true) {
                int enqIdx = indexOfByte(buf, (byte) 0x05, searchStart);
                if (enqIdx >= 0) {
                    // ENQ — just skip, ACK is sent by caller
                    searchStart = enqIdx + 1;
                    continue;
                }
                int stxIdx = indexOf(buf, start, searchStart);
                if (stxIdx < 0) break;
                int payloadStart = stxIdx + start.length;
                int etxIdx = indexOf(buf, end, payloadStart);
                if (etxIdx < 0) break;
                if (etxIdx + 4 >= buf.length) break;
                byte[] payload = Arrays.copyOfRange(buf, payloadStart, etxIdx);
                byte chk1 = buf[etxIdx + end.length];
                byte chk2 = buf[etxIdx + end.length + 1];
                if (buf[etxIdx + end.length + 2] == 0x0D && buf[etxIdx + end.length + 3] == 0x0A) {
                    int sum = 0;
                    for (byte b : payload) sum = (sum + b) & 0xFF;
                    String expectedChk = String.format("%02X", sum).substring(0, 2);
                    String actualChk = String.format("%02c%02c", (char) chk1, (char) chk2);
                    if (expectedChk.equals(actualChk)) {
                        messages.add(new String(payload, Charset.forName(config.getCharset())));
                    }
                    searchStart = etxIdx + end.length + 4;
                } else {
                    searchStart = stxIdx + 1;
                }
            }
            if (searchStart > 0) {
                byte[] remaining = Arrays.copyOfRange(buf, searchStart, buf.length);
                buffer.reset();
                buffer.write(remaining, 0, remaining.length);
            }
            return messages.toArray(new String[0]);
        }

        @Override
        public boolean sendsAck() {
            return true;
        }

        @Override
        public byte[] buildAck(String payload, SerialTransmissionModeProperties props,
                               SerialPortConfig config) throws Exception {
            byte[] ack = parseHex(config.getCommitAckBytes());
            return ack.length > 0 ? ack : new byte[]{0x06};
        }

        @Override
        public void reset() {
            buffer.reset();
        }

        private byte[] calculateChecksum(byte[] data, String algorithm, Charset charset) {
            if (algorithm == null) algorithm = "ASTM_STANDARD";
            switch (algorithm.toUpperCase()) {
                case "NONE": return new byte[0];
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
                    int sum = 0;
                    for (byte b : data) sum = (sum + b) & 0xFF;
                    return String.format("%02X", sum).substring(0, 2).getBytes(charset);
                }
            }
        }
    }

    // ===== Utility methods =====

    public static void registerAll() {
        SerialTransmissionModeRegistry.registerServerProvider(new RawProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new LineProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new FrameProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new MllpProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new AstmProvider());
    }

    static byte[] parseHex(String hex) {
        if (hex == null || hex.trim().isEmpty()) return new byte[0];
        String clean = hex.replaceAll("\\s", "").toUpperCase();
        if (clean.length() % 2 != 0) clean = "0" + clean;
        byte[] result = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return result;
    }

    static int indexOf(byte[] haystack, byte[] needle, int fromIndex) {
        outer: for (int i = fromIndex; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    static int indexOfByte(byte[] haystack, byte needle, int fromIndex) {
        for (int i = fromIndex; i < haystack.length; i++) {
            if (haystack[i] == needle) return i;
        }
        return -1;
    }
}
