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

    private static final org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(SerialBuiltinModeProviders.class);

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

    // ===== DIMENSION mode (Siemens Dimension PN D00396) =====

    /**
     * Dimension provider — native Siemens Dimension host-link framing for
     * serial lines (PN D00396 Rev.2). Mirrors the DLC rules of the
     * bitdreamit-dimension-transmission extension:
     *
     *   Frame  = STX + TYPE + fields(FS-delimited) + CHK(2 hex) + ETX
     *   CHK    = 8-bit sum mod 256 of all bytes between STX and CHK,
     *            two UPPERCASE ASCII hex digits (chk sits BEFORE ETX)
     *   ACK    0x06 after every correct frame (1-second instrument timer)
     *   NAK    0x15 on bad checksum; sender retransmits max 4 times
     *   ENQ    from the receiver on line error - answered with ACK
     *
     * Tolerant application answers (same policy as the extension's
     * auto-response path, so a lone serial deployment stays protocol-clean):
     *   P (poll)  or I (query)  ->  No Request frame  <STX>N<FS>6A<ETX>
     *   R (result) or C (calib) ->  Acceptance frame  <STX>M<FS>A<FS><FS>E2<ETX>
     *   M / D / W               ->  data-link ACK only
     *
     * When the bitdreamit-dimension-transmission extension is installed, the
     * SerialSourceConnector prefers the full Mirth "Dimension" provider
     * (order download via DimensionOrderRegistry); this built-in mode is the
     * self-contained fallback that keeps instruments from erroring 318/321.
     */
    public static class DimensionProvider extends SerialTransmissionModeProvider {
        public static final String NAME = "DIMENSION";
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private int consecutiveNaks = 0;
        private java.util.List<byte[]> lastAnswers = new java.util.ArrayList<byte[]>();

        // Frame control bytes (PN D00396 p.1-5)
        private static final byte DIM_STX = 0x02;
        private static final byte DIM_ETX = 0x03;
        private static final byte DIM_FS  = 0x1C;
        private static final byte DIM_ENQ = 0x05;
        private static final byte DIM_ACK = 0x06;
        private static final byte DIM_NAK = 0x15;

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
            // Payload is the inner content (TYPE + fields). Dimension field
            // layout ends with a trailing FS before CHK - add one if absent.
            String body = (payload != null) ? payload : "";
            if (!body.isEmpty() && body.charAt(body.length() - 1) != (char) (DIM_FS & 0xFF)) {
                body = body + (char) (DIM_FS & 0xFF);
            }
            byte[] bodyBytes = body.getBytes(cs);
            int sum = 0;
            for (byte b : bodyBytes) sum = (sum + (b & 0xFF)) & 0xFF;
            byte[] chk = String.format("%02X", sum).getBytes(cs);
            byte[] out = new byte[1 + bodyBytes.length + chk.length + 1];
            int pos = 0;
            out[pos++] = DIM_STX;
            System.arraycopy(bodyBytes, 0, out, pos, bodyBytes.length); pos += bodyBytes.length;
            System.arraycopy(chk, 0, out, pos, chk.length); pos += chk.length;
            out[pos] = DIM_ETX;
            return out;
        }

        @Override
        public String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                     SerialPortConfig config) throws Exception {
            buffer.write(data, 0, data.length);
            byte[] buf = buffer.toByteArray();
            Charset cs = Charset.forName(config.getCharset());
            java.util.List<String> messages = new java.util.ArrayList<String>();
            java.util.List<byte[]> answers = new java.util.ArrayList<byte[]>();
            int pos = 0;

            while (pos < buf.length) {
                byte b = buf[pos];

                if (b == DIM_STX) {
                    // Find the frame end.
                    int etxIdx = indexOfByte(buf, DIM_ETX, pos + 1);
                    if (etxIdx < 0) break; // incomplete - wait for more bytes

                    int segmentLen = etxIdx - pos; // STX .. before ETX
                    if (segmentLen < 4) {
                        // Shorter than STX + TYPE + CHK(2) - NAK and drop.
                        answers.add(new byte[]{DIM_NAK});
                        noteNak(config);
                        pos = etxIdx + 1;
                        continue;
                    }
                    byte[] body = Arrays.copyOfRange(buf, pos + 1, etxIdx - 2); // TYPE..fields (no CHK)
                    byte chk1 = buf[etxIdx - 2];
                    byte chk2 = buf[etxIdx - 1];

                    int sum = 0;
                    for (byte x : body) sum = (sum + (x & 0xFF)) & 0xFF;
                    String expected = String.format("%02X", sum);
                    String actual = "" + (char) (chk1 & 0xFF) + (char) (chk2 & 0xFF);

                    if (expected.equalsIgnoreCase(actual)) {
                        consecutiveNaks = 0;
                        answers.add(new byte[]{DIM_ACK});
                        String payload = new String(body, cs);
                        messages.add(payload);
                        // Tolerant application answers (kept inside the 1 s timer).
                        if (payload.length() > 0) {
                            char type = Character.toUpperCase(payload.charAt(0));
                            if (type == 'P' || type == 'I') {
                                answers.add(buildFrame("N", props, config));
                            } else if (type == 'R' || type == 'C') {
                                answers.add(buildFrame("M", props, config));
                            }
                            // M / D / W: data-link ACK only.
                        }
                    } else {
                        consecutiveNaks++;
                        if (consecutiveNaks >= 4) {
                            // PN D00396 instrument error 318 semantics - abort the read.
                            logger.error("DIMENSION mode: 4 consecutive checksum failures on " +
                                    config.getPortName() + " - aborting read cycle (expected " +
                                    expected + ", received " + actual + ")");
                            buffer.reset();
                            consecutiveNaks = 0;
                            return messages.toArray(new String[0]);
                        }
                        logger.warn("DIMENSION checksum mismatch on " + config.getPortName() +
                                " (expected " + expected + ", received " + actual + ") - NAK");
                        answers.add(new byte[]{DIM_NAK});
                    }
                    pos = etxIdx + 1;
                    continue;
                }

                // Stray bytes outside frames.
                if (b == DIM_ENQ) {
                    // Receiver saw a line error while expecting ACK/NAK - answer ACK.
                    answers.add(new byte[]{DIM_ACK});
                    pos++;
                } else if (b == DIM_ACK || b == DIM_NAK || b == 0x04 /*EOT*/) {
                    pos++; // handshake echo / sender cancel - ignore
                } else {
                    pos++; // noise before STX - ignore
                }
            }

            if (pos > 0) {
                byte[] remaining = Arrays.copyOfRange(buf, pos, buf.length);
                buffer.reset();
                buffer.write(remaining, 0, remaining.length);
            }

            // Queue everything (ACK/NAK + auto answers) so the source connector
            // can write it while the instrument's 1-second timer is still running.
            lastAnswers.addAll(answers);
            return messages.toArray(new String[0]);
        }

        /**
         * Protocol answers (ACK/NAK/auto N / auto M-A) generated by the last
         * processBytes call. The SerialSourceConnector writes these to the
         * port immediately after processBytes returns.
         */
        @Override
        public byte[][] drainAnswers() {
            byte[][] out = lastAnswers.toArray(new byte[lastAnswers.size()][]);
            lastAnswers.clear();
            return out;
        }

        /** The M/A acceptance frame body: M FS A FS FS + checksum E2. */
        private byte[] buildFrame(String type, SerialTransmissionModeProperties props,
                                  SerialPortConfig config) throws Exception {
            // 'N' -> N FS, chk 6A (manual p.1-12). 'M' -> M FS A FS FS, chk E2 (p.1-16 MAE2).
            Charset cs = Charset.forName(config.getCharset());
            byte[] bodyBytes;
            if ("N".equals(type)) {
                bodyBytes = new byte[]{'N', DIM_FS};
            } else {
                bodyBytes = new byte[]{'M', DIM_FS, 'A', DIM_FS, DIM_FS};
            }
            int sum = 0;
            for (byte x : bodyBytes) sum = (sum + (x & 0xFF)) & 0xFF;
            byte[] chk = String.format("%02X", sum).getBytes(cs);
            byte[] out = new byte[1 + bodyBytes.length + chk.length + 1];
            int p2 = 0;
            out[p2++] = DIM_STX;
            System.arraycopy(bodyBytes, 0, out, p2, bodyBytes.length); p2 += bodyBytes.length;
            System.arraycopy(chk, 0, out, p2, chk.length); p2 += chk.length;
            out[p2] = DIM_ETX;
            return out;
        }

        private void noteNak(SerialPortConfig config) {
            consecutiveNaks++;
            if (consecutiveNaks >= 4) {
                logger.error("DIMENSION mode: repeated short frames on " + config.getPortName() +
                        " - aborting read cycle");
                buffer.reset();
                consecutiveNaks = 0;
            }
        }

        @Override
        public void reset() {
            buffer.reset();
            consecutiveNaks = 0;
            lastAnswers.clear();
        }

        @Override
        public boolean sendsAck() { return true; }

        @Override
        public byte[] buildAck(String payload, SerialTransmissionModeProperties props,
                               SerialPortConfig config) throws Exception {
            return new byte[]{DIM_ACK};
        }
    }

    // ===== Utility methods =====

    public static void registerAll() {
        SerialTransmissionModeRegistry.registerServerProvider(new RawProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new LineProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new FrameProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new MllpProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new AstmProvider());
        SerialTransmissionModeRegistry.registerServerProvider(new DimensionProvider());
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
