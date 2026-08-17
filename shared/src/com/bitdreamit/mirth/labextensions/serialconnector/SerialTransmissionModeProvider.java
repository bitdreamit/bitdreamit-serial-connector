package com.bitdreamit.mirth.labextensions.serialconnector;

/**
 * Server-side provider for serial transmission modes.
 * Each provider knows how to:
 *   - Read incoming bytes and assemble them into messages
 *   - Frame outgoing messages into bytes
 *
 * Modeled after Mirth's TransmissionModeProvider so new modes can be
 * added as separate plugins without modifying the connector classes.
 *
 * Providers are registered in SerialServerPlugin.init() and looked up
 * by SerialSourceConnector / SerialDestinationConnector at runtime.
 */
public abstract class SerialTransmissionModeProvider {

    /**
     * Returns the plugin point name (e.g. "RAW", "MLLP", "ASTM").
     */
    public abstract String getPluginPointName();

    /**
     * Returns the default properties for this mode.
     */
    public abstract SerialTransmissionModeProperties getDefaultProperties();

    /**
     * Frame an outgoing message into bytes.
     *
     * @param payload  the message payload
     * @param props    the mode-specific properties
     * @param config   the port config (charset, binary mode, etc.)
     * @return framed bytes ready to write to the serial port
     */
    public abstract byte[] frameMessage(String payload, SerialTransmissionModeProperties props,
                                         SerialPortConfig config) throws Exception;

    /**
     * Called when a chunk of bytes is read from the serial port.
     * The provider should buffer and return complete messages.
     *
     * @param data     the chunk of bytes just read
     * @param props    the mode-specific properties
     * @param config   the port config
     * @return array of complete message payloads (may be empty if buffering)
     */
    public abstract String[] processBytes(byte[] data, SerialTransmissionModeProperties props,
                                           SerialPortConfig config) throws Exception;

    /**
     * Reset internal buffer state (called on stop/reconnect).
     */
    public abstract void reset();

    /**
     * Whether this mode sends an ACK/NAK back on the source side.
     */
    public boolean sendsAck() {
        return false;
    }

    /**
     * Build the ACK bytes to send for the given received message.
     * Only called if sendsAck() returns true.
     */
    public byte[] buildAck(String payload, SerialTransmissionModeProperties props,
                           SerialPortConfig config) throws Exception {
        return new byte[0];
    }
}
