package com.bitdreamit.mirth.labextensions.serialconnector;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal frame assembler for Serial Transport.
 * Modes: BASIC (delimiter-based), MLLP, ASTM_E1381
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 */
public class SerialFrameAssembler {

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte ETB = 0x17;
    private static final byte ENQ = 0x05;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;
    private static final byte EOT = 0x04;
    private static final byte CR  = 0x0D;
    private static final byte LF  = 0x0A;
    private static final byte VT  = 0x0B;
    private static final byte FS  = 0x1C;

    private final String mode;
    private final byte[] delimiter;
    private final Charset charset;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean inFrame = false;

    public SerialFrameAssembler(String mode, String delimiterStr, String charsetName) {
        this.mode = mode != null ? mode : "BASIC";
        this.charset = Charset.forName(charsetName != null ? charsetName : "UTF-8");
        if (delimiterStr != null && !delimiterStr.isEmpty()) {
            String d = delimiterStr.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
            this.delimiter = d.getBytes(this.charset);
        } else {
            this.delimiter = new byte[]{CR, LF};
        }
    }

    public synchronized List<Frame> process(byte[] chunk) {
        List<Frame> frames = new ArrayList<>();
        if (chunk == null || chunk.length == 0) return frames;

        switch (mode) {
            case "ASTM_E1381":
                processASTM(chunk, frames);
                break;
            case "MLLP":
                processMLLP(chunk, frames);
                break;
            default:
                processBasic(chunk, frames);
                break;
        }
        return frames;
    }

    private void processASTM(byte[] chunk, List<Frame> frames) {
        for (byte b : chunk) {
            if (b == ENQ || b == ACK || b == NAK || b == EOT) {
                if (inFrame && buffer.size() > 0) {
                    buffer.reset();
                    inFrame = false;
                }
                frames.add(new Frame(Frame.Type.CONTROL, new byte[]{b}));
                continue;
            }
            if (b == STX) {
                buffer.reset();
                buffer.write(b);
                inFrame = true;
                continue;
            }
            if (inFrame) {
                buffer.write(b);
                int size = buffer.size();
                if (size >= 7) {
                    byte[] data = buffer.toByteArray();
                    int end = size - 1;
                    if (data[end] == LF && data[end - 1] == CR) {
                        byte term = data[end - 4];
                        if (term == ETX || term == ETB) {
                            frames.add(new Frame(Frame.Type.DATA, data));
                            buffer.reset();
                            inFrame = false;
                        }
                    }
                }
            }
        }
    }

    private void processMLLP(byte[] chunk, List<Frame> frames) {
        for (byte b : chunk) {
            if (b == VT) {
                buffer.reset();
                buffer.write(b);
                inFrame = true;
            } else if (inFrame) {
                buffer.write(b);
                int size = buffer.size();
                if (size >= 3) {
                    byte[] data = buffer.toByteArray();
                    if (data[size - 2] == FS && data[size - 1] == CR) {
                        frames.add(new Frame(Frame.Type.DATA, data));
                        buffer.reset();
                        inFrame = false;
                    }
                }
            }
        }
    }

    private void processBasic(byte[] chunk, List<Frame> frames) {
        if (delimiter.length == 0) {
            frames.add(new Frame(Frame.Type.DATA, chunk.clone()));
            return;
        }
        buffer.write(chunk, 0, chunk.length);
        byte[] data = buffer.toByteArray();
        int start = 0;
        for (int i = 0; i <= data.length - delimiter.length; i++) {
            boolean match = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (data[i + j] != delimiter[j]) { match = false; break; }
            }
            if (match) {
                int len = i - start;
                if (len > 0) {
                    byte[] frame = new byte[len];
                    System.arraycopy(data, start, frame, 0, len);
                    frames.add(new Frame(Frame.Type.DATA, frame));
                }
                start = i + delimiter.length;
                i = start - 1;
            }
        }
        int remaining = data.length - start;
        buffer.reset();
        if (remaining > 0) {
            buffer.write(data, start, remaining);
        }
    }

    public static class Frame {
        public enum Type { DATA, CONTROL }
        public final Type type;
        public final byte[] bytes;
        public Frame(Type type, byte[] bytes) {
            this.type = type;
            this.bytes = bytes != null ? bytes.clone() : new byte[0];
        }
    }
}
