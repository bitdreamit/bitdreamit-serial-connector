package com.bitdreamit.mirth.labextensions.serialconnector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/**
 * Built-in client-side serial transmission mode providers.
 *
 * Each provider supplies a settings component (button that opens the
 * SerialTransmissionModeDialog) and the sample label/value for display.
 *
 * New modes can register their own client provider by calling
 * SerialTransmissionModeRegistry.registerClientProvider().
 */
public class SerialBuiltinModeClientProviders {

    public static void registerAll() {
        SerialTransmissionModeRegistry.registerClientProvider(new RawClientProvider());
        SerialTransmissionModeRegistry.registerClientProvider(new LineClientProvider());
        SerialTransmissionModeRegistry.registerClientProvider(new FrameClientProvider());
        SerialTransmissionModeRegistry.registerClientProvider(new MllpClientProvider());
        SerialTransmissionModeRegistry.registerClientProvider(new AstmClientProvider());
    }

    // ===== RAW =====

    public static class RawClientProvider extends SerialTransmissionModeClientProvider {
        public static final String NAME = "RAW";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public void setProperties(SerialTransmissionModeProperties properties) {}

        @Override
        public boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight) {
            return true;
        }

        @Override
        public void resetInvalidProperties() {}

        @Override
        public JComponent getSettingsComponent() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.add(new JButton("No settings for RAW mode"));
            return panel;
        }

        @Override
        public String getSampleLabel() { return "Raw bytes"; }

        @Override
        public String getSampleValue() { return "<raw bytes>"; }
    }

    // ===== LINE =====

    public static class LineClientProvider extends SerialTransmissionModeClientProvider {
        public static final String NAME = "LINE";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public void setProperties(SerialTransmissionModeProperties properties) {}

        @Override
        public boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight) {
            return true;
        }

        @Override
        public void resetInvalidProperties() {}

        @Override
        public JComponent getSettingsComponent() {
            return new JButton("Configure Line Delimiter...");
        }

        @Override
        public String getSampleLabel() { return "Line-delimited"; }

        @Override
        public String getSampleValue() { return "<message>\\r\\n"; }
    }

    // ===== FRAME =====

    public static class FrameClientProvider extends SerialTransmissionModeClientProvider {
        public static final String NAME = "FRAME";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public void setProperties(SerialTransmissionModeProperties properties) {}

        @Override
        public boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight) {
            return true;
        }

        @Override
        public void resetInvalidProperties() {}

        @Override
        public JComponent getSettingsComponent() {
            return new JButton("Configure Frame Bytes...");
        }

        @Override
        public String getSampleLabel() { return "Frame mode"; }

        @Override
        public String getSampleValue() { return "<start><message><end>"; }
    }

    // ===== MLLP =====

    public static class MllpClientProvider extends SerialTransmissionModeClientProvider {
        public static final String NAME = "MLLP";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public void setProperties(SerialTransmissionModeProperties properties) {}

        @Override
        public boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight) {
            return true;
        }

        @Override
        public void resetInvalidProperties() {}

        @Override
        public JComponent getSettingsComponent() {
            return new JButton("Configure MLLP...");
        }

        @Override
        public String getSampleLabel() { return "MLLP (HL7)"; }

        @Override
        public String getSampleValue() { return "<VT><message><FS><CR>"; }
    }

    // ===== ASTM =====

    public static class AstmClientProvider extends SerialTransmissionModeClientProvider {
        public static final String NAME = "ASTM";

        @Override
        public String getPluginPointName() { return NAME; }

        @Override
        public SerialTransmissionModeProperties getProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public SerialTransmissionModeProperties getDefaultProperties() {
            return new SerialTransmissionModeProperties(NAME);
        }

        @Override
        public void setProperties(SerialTransmissionModeProperties properties) {}

        @Override
        public boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight) {
            return true;
        }

        @Override
        public void resetInvalidProperties() {}

        @Override
        public JComponent getSettingsComponent() {
            return new JButton("Configure ASTM...");
        }

        @Override
        public String getSampleLabel() { return "ASTM E1381"; }

        @Override
        public String getSampleValue() { return "<STX><message><ETX><chk><CR><LF>"; }
    }
}
