package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Serial Connector Settings Panel — matches Mirth's TCP connector UI style exactly.
 *
 * Layout follows Mirth TCP conventions:
 *  - Strict two-column grid: Label column (fixed width, right-aligned) + Input column (left-aligned)
 *  - All input fields start at the SAME X position (single vertical axis)
 *  - Each label+field on its OWN row (no multi-column packing)
 *  - Section headers: bold text + horizontal separator line below
 *  - Pure white background, consistent spacing
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 */
public class SerialConnectorSettingsPanel extends JPanel implements ActionListener {

    private boolean isSender;
    private ConnectorProperties properties;

    // Fixed label column width — ALL labels right-align to this width
    private static final int LABEL_WIDTH = 140;
    private static final int FIELD_GAP = 6;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 10;

    // Connection fields
    private MirthComboBox portBox;
    private JButton refreshPortsBtn;
    private MirthCheckBox autoDetectPortBox;
    private MirthComboBox baudBox;
    private MirthCheckBox autoDetectBaudBox;
    private MirthComboBox dataBitsBox;
    private MirthComboBox stopBitsBox;
    private MirthComboBox parityBox;
    private MirthComboBox flowBox;

    // Data format fields
    private MirthComboBox charsetBox;
    private MirthCheckBox binaryBox;
    private MirthComboBox transmissionModeBox;
    private JButton transmissionSettingsBtn;

    // Timeouts fields
    private JTextField readTimeoutField;
    private JTextField writeTimeoutField;
    private JTextField bufferSizeField;

    // Signal control fields
    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JTextField signalTimeoutField;

    // Advanced fields
    private MirthCheckBox breakBox;
    private JTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;

    // Health monitor fields
    private MirthCheckBox healthBox;
    private JTextField healthIntervalField;
    private JTextField maxReconnectField;
    private JTextField reconnectDelayField;

    // Premium fields
    private JTextField idleTimeoutField;
    private JTextField receiveIdleTimeoutField;
    private JTextField portAliasField;
    private JButton testConnectionBtn;

    // PREMIUM: Destination extras
    private MirthCheckBox processResponseBox;
    private JTextField responseDelimiterField;
    private JTextField responseTimeoutField;
    private MirthCheckBox useTemplateBox;
    private JTextField templateField;
    private MirthComboBox checksumAlgoBox;

    // PREMIUM: Port status indicator
    private JLabel statusLabel;
    private JLabel statsLabel;

    // Destination-only fields
    private MirthCheckBox waitAckBox;
    private JTextField ackTimeoutField;
    private MirthCheckBox keepOpenBox;
    private JTextField ackPatternField;

    private static final Color LABEL_COLOR = new Color(51, 51, 51);
    private static final Color SEP_COLOR = new Color(200, 200, 200);

    public SerialConnectorSettingsPanel() {
        this(false);
    }

    public SerialConnectorSettingsPanel(boolean isSender) {
        this.isSender = isSender;
        initComponents();
        refreshPortList();
    }

    public String getConnectorName() {
        return isSender ? "Serial Writer" : "Serial Reader";
    }

    // ===== GridBag helper — enforces strict two-column layout =====

    /**
     * Creates a label that is right-aligned within a fixed-width column.
     * This ensures ALL input fields start at the same X position.
     */
    private JLabel mkLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(LABEL_COLOR);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 22));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, 22));
        return label;
    }

    /**
     * Returns GridBagConstraints for a label (column 0).
     */
    private GridBagConstraints labelGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.anchor = GridBagConstraints.NORTHEAST;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 0, ROW_GAP, FIELD_GAP);
        return g;
    }

    /**
     * Returns GridBagConstraints for an input field (column 1).
     * weightx = 1.0 so the field area stretches.
     */
    private GridBagConstraints fieldGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = row;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, ROW_GAP, 0);
        return g;
    }

    // ===== Section header with separator line =====

    /**
     * Adds a section header (bold text) with a horizontal separator line below.
     * Spans both columns.
     */
    private void addSectionHeader(JPanel parent, GridBagLayout layout, String title, int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridwidth = 2;
        g.gridx = 0;
        g.gridy = row;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(row == 0 ? 0 : SECTION_GAP, 0, 2, 0);

        // Header panel: label + separator
        JPanel headerPanel = new JPanel(new BorderLayout(0, 2));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setOpaque(true);

        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(LABEL_COLOR);
        header.setBackground(Color.WHITE);
        header.setOpaque(true);
        headerPanel.add(header, BorderLayout.NORTH);

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(SEP_COLOR);
        sep.setBackground(SEP_COLOR);
        headerPanel.add(sep, BorderLayout.SOUTH);

        layout.setConstraints(headerPanel, g);
        parent.add(headerPanel);
    }

    // ===== Row helper — adds a label + component on one row =====

    private void addRow(JPanel parent, GridBagLayout layout, String labelText, JComponent field, int row) {
        JLabel label = mkLabel(labelText);
        GridBagConstraints lg = labelGbc(row);
        GridBagConstraints fg = fieldGbc(row);

        layout.setConstraints(label, lg);
        parent.add(label);

        // Wrap field in a left-aligned panel so it doesn't stretch ugly
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setOpaque(true);
        fieldPanel.add(field);
        layout.setConstraints(fieldPanel, fg);
        parent.add(fieldPanel);
    }

    private void addRowCustom(JPanel parent, GridBagLayout layout, String labelText, JComponent field, int row) {
        // For checkboxes and complex controls — field spans the input column
        JLabel label = mkLabel(labelText);
        GridBagConstraints lg = labelGbc(row);
        GridBagConstraints fg = fieldGbc(row);

        layout.setConstraints(label, lg);
        parent.add(label);

        field.setBackground(Color.WHITE);
        field.setOpaque(true);
        layout.setConstraints(field, fg);
        parent.add(field);
    }

    // ===== Main init =====

    private void initComponents() {
        setBackground(Color.WHITE);
        setOpaque(true);
        setLayout(new BorderLayout());

        JPanel contentPanel = new ScrollablePanel(new GridBagLayout());
        GridBagLayout layout = (GridBagLayout) contentPanel.getLayout();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setOpaque(true);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        int row = 0;

        // ===== Section: Connection =====
        addSectionHeader(contentPanel, layout, "Connection", row++);

        // Port row
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        portPanel.setBackground(Color.WHITE);
        portPanel.setOpaque(true);
        portBox = new MirthComboBox();
        portBox.setEditable(true);
        portBox.setPreferredSize(new Dimension(100, 22));
        portPanel.add(portBox);
        refreshPortsBtn = new JButton("Refresh");
        refreshPortsBtn.setMargin(new Insets(2, 6, 2, 6));
        refreshPortsBtn.addActionListener(this);
        portPanel.add(refreshPortsBtn);
        autoDetectPortBox = new MirthCheckBox("Auto-detect");
        portPanel.add(autoDetectPortBox);
        addRowCustom(contentPanel, layout, "Port:", portPanel, row++);

        // Port Alias (Premium)
        portAliasField = new JTextField(20);
        portAliasField.setPreferredSize(new Dimension(140, 22));
        addRow(contentPanel, layout, "Port Alias:", portAliasField, row++);

        // Baud Rate row
        JPanel baudPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        baudPanel.setBackground(Color.WHITE);
        baudPanel.setOpaque(true);
        baudBox = new MirthComboBox();
        baudBox.setModel(new DefaultComboBoxModel<>(new String[]{"9600", "19200", "38400", "57600", "115200", "230400"}));
        baudBox.setPreferredSize(new Dimension(100, 22));
        baudPanel.add(baudBox);
        autoDetectBaudBox = new MirthCheckBox("Auto-detect");
        baudPanel.add(autoDetectBaudBox);
        addRowCustom(contentPanel, layout, "Baud Rate:", baudPanel, row++);

        // Data Bits
        dataBitsBox = new MirthComboBox();
        dataBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "6", "7", "8"}));
        dataBitsBox.setSelectedItem("8");
        dataBitsBox.setPreferredSize(new Dimension(60, 22));
        addRow(contentPanel, layout, "Data Bits:", dataBitsBox, row++);

        // Stop Bits
        stopBitsBox = new MirthComboBox();
        stopBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"1", "1.5", "2"}));
        stopBitsBox.setPreferredSize(new Dimension(60, 22));
        addRow(contentPanel, layout, "Stop Bits:", stopBitsBox, row++);

        // Parity
        parityBox = new MirthComboBox();
        parityBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "Odd", "Even", "Mark", "Space"}));
        parityBox.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Parity:", parityBox, row++);

        // Flow Control
        flowBox = new MirthComboBox();
        flowBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"}));
        flowBox.setPreferredSize(new Dimension(100, 22));
        addRow(contentPanel, layout, "Flow Control:", flowBox, row++);

        // ===== Section: Data Format =====
        addSectionHeader(contentPanel, layout, "Data Format", row++);

        // Charset
        charsetBox = new MirthComboBox();
        charsetBox.setModel(new DefaultComboBoxModel<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "Windows-1252"}));
        charsetBox.setPreferredSize(new Dimension(120, 22));
        addRow(contentPanel, layout, "Charset:", charsetBox, row++);

        // Binary
        binaryBox = new MirthCheckBox("Binary (Base64)");
        addRowCustom(contentPanel, layout, "Binary Mode:", binaryBox, row++);

        // Transmission Mode — populated DYNAMICALLY from registry (like TCP connector)
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modePanel.setBackground(Color.WHITE);
        modePanel.setOpaque(true);
        transmissionModeBox = new MirthComboBox();
        // DYNAMIC: Populate modes from Mirth's LoadedExtensions — SAME API as TCP connector.
        // When a new mode extension is installed (e.g. ASTM E1381), it automatically
        // appears in this dropdown without any code changes.
        try {
            java.util.Map<String, ?> plugins =
                com.mirth.connect.client.ui.LoadedExtensions.getInstance().getTransmissionModePlugins();
            if (plugins != null && !plugins.isEmpty()) {
                transmissionModeBox.setModel(new DefaultComboBoxModel<>(
                    plugins.keySet().toArray(new String[0])));
            } else {
                // Fallback if no transmission mode plugins loaded
                transmissionModeBox.setModel(new DefaultComboBoxModel<>(
                    new String[]{"RAW", "LINE", "FRAME", "MLLP", "ASTM"}));
            }
        } catch (Throwable t) {
            // Fallback if LoadedExtensions API not available
            transmissionModeBox.setModel(new DefaultComboBoxModel<>(
                new String[]{"RAW", "LINE", "FRAME", "MLLP", "ASTM"}));
        }
        transmissionModeBox.setPreferredSize(new Dimension(100, 22));
        transmissionModeBox.addActionListener(this);
        modePanel.add(transmissionModeBox);
        transmissionSettingsBtn = new JButton("Configure...");
        transmissionSettingsBtn.setMargin(new Insets(2, 8, 2, 8));
        transmissionSettingsBtn.addActionListener(this);
        modePanel.add(transmissionSettingsBtn);
        addRowCustom(contentPanel, layout, "Transmission Mode:", modePanel, row++);

        // ===== Section: Timeouts =====
        addSectionHeader(contentPanel, layout, "Timeouts", row++);

        readTimeoutField = new JTextField("1000", 8);
        readTimeoutField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Read Timeout (ms):", readTimeoutField, row++);

        writeTimeoutField = new JTextField("1000", 8);
        writeTimeoutField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Write Timeout (ms):", writeTimeoutField, row++);

        bufferSizeField = new JTextField("4096", 8);
        bufferSizeField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Buffer Size:", bufferSizeField, row++);

        // ===== Section: Signal Control =====
        addSectionHeader(contentPanel, layout, "Signal Control", row++);

        // Output signals (DTR + RTS)
        JPanel outSignalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        outSignalPanel.setBackground(Color.WHITE);
        outSignalPanel.setOpaque(true);
        dtrBox = new MirthCheckBox("DTR");
        dtrBox.setSelected(true);
        outSignalPanel.add(dtrBox);
        rtsBox = new MirthCheckBox("RTS");
        rtsBox.setSelected(true);
        outSignalPanel.add(rtsBox);
        addRowCustom(contentPanel, layout, "Output Signals:", outSignalPanel, row++);

        // Wait for (CTS + DSR + DCD)
        JPanel waitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        waitPanel.setBackground(Color.WHITE);
        waitPanel.setOpaque(true);
        waitCtsBox = new MirthCheckBox("CTS");
        waitPanel.add(waitCtsBox);
        waitDsrBox = new MirthCheckBox("DSR");
        waitPanel.add(waitDsrBox);
        waitDcdBox = new MirthCheckBox("DCD");
        waitPanel.add(waitDcdBox);
        addRowCustom(contentPanel, layout, "Wait For:", waitPanel, row++);

        signalTimeoutField = new JTextField("1000", 8);
        signalTimeoutField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Signal Timeout (ms):", signalTimeoutField, row++);

        // ===== Section: Advanced =====
        addSectionHeader(contentPanel, layout, "Advanced", row++);

        breakBox = new MirthCheckBox("Send break before opening port");
        addRowCustom(contentPanel, layout, "Send Break:", breakBox, row++);

        breakDurField = new JTextField("100", 8);
        breakDurField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Break Duration (ms):", breakDurField, row++);

        flushOpenBox = new MirthCheckBox("Flush I/O buffers on open");
        flushOpenBox.setSelected(true);
        addRowCustom(contentPanel, layout, "Flush on Open:", flushOpenBox, row++);

        flushCloseBox = new MirthCheckBox("Flush I/O buffers on close");
        flushCloseBox.setSelected(true);
        addRowCustom(contentPanel, layout, "Flush on Close:", flushCloseBox, row++);

        // ===== Section: Timeouts (Premium) =====
        addSectionHeader(contentPanel, layout, "Idle & Receive Timeouts", row++);

        idleTimeoutField = new JTextField("0", 8);
        idleTimeoutField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Idle Timeout (ms):", idleTimeoutField, row++);

        receiveIdleTimeoutField = new JTextField("0", 8);
        receiveIdleTimeoutField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Receive Idle (ms):", receiveIdleTimeoutField, row++);

        // ===== Section: Health Monitor =====
        addSectionHeader(contentPanel, layout, "Health Monitor", row++);

        healthBox = new MirthCheckBox("Enable auto-reconnect");
        healthBox.setSelected(true);
        addRowCustom(contentPanel, layout, "Auto-Reconnect:", healthBox, row++);

        healthIntervalField = new JTextField("5000", 8);
        healthIntervalField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Check Interval (ms):", healthIntervalField, row++);

        maxReconnectField = new JTextField("10", 8);
        maxReconnectField.setPreferredSize(new Dimension(60, 22));
        addRow(contentPanel, layout, "Max Retry:", maxReconnectField, row++);

        reconnectDelayField = new JTextField("5000", 8);
        reconnectDelayField.setPreferredSize(new Dimension(80, 22));
        addRow(contentPanel, layout, "Retry Delay (ms):", reconnectDelayField, row++);

        // ===== Section: Destination Options (sender only) =====
        if (isSender) {
            addSectionHeader(contentPanel, layout, "Destination Options", row++);

            keepOpenBox = new MirthCheckBox("Keep connection open between messages");
            addRowCustom(contentPanel, layout, "Connection:", keepOpenBox, row++);

            waitAckBox = new MirthCheckBox("Wait for ACK after write");
            addRowCustom(contentPanel, layout, "ACK:", waitAckBox, row++);

            ackTimeoutField = new JTextField("1000", 8);
            ackTimeoutField.setPreferredSize(new Dimension(80, 22));
            addRow(contentPanel, layout, "ACK Timeout (ms):", ackTimeoutField, row++);

            ackPatternField = new JTextField("06", 8);
            ackPatternField.setPreferredSize(new Dimension(60, 22));
            addRow(contentPanel, layout, "ACK Pattern (hex):", ackPatternField, row++);

            // PREMIUM: Response processing
            processResponseBox = new MirthCheckBox("Read response after write");
            addRowCustom(contentPanel, layout, "Process Response:", processResponseBox, row++);

            responseDelimiterField = new JTextField("\\r\\n", 10);
            responseDelimiterField.setPreferredSize(new Dimension(80, 22));
            addRow(contentPanel, layout, "Response Delimiter:", responseDelimiterField, row++);

            responseTimeoutField = new JTextField("5000", 8);
            responseTimeoutField.setPreferredSize(new Dimension(80, 22));
            addRow(contentPanel, layout, "Response Timeout (ms):", responseTimeoutField, row++);

            // PREMIUM: NextGen-style template
            useTemplateBox = new MirthCheckBox("Apply message template");
            addRowCustom(contentPanel, layout, "Use Template:", useTemplateBox, row++);

            templateField = new JTextField(30);
            templateField.setPreferredSize(new Dimension(200, 22));
            addRow(contentPanel, layout, "Template:", templateField, row++);

            // PREMIUM: Custom checksum algorithm
            checksumAlgoBox = new MirthComboBox();
            checksumAlgoBox.setModel(new DefaultComboBoxModel<>(
                    new String[]{"ASTM_STANDARD", "MOD256", "XOR", "NONE"}));
            checksumAlgoBox.setPreferredSize(new Dimension(120, 22));
            addRow(contentPanel, layout, "Checksum Algorithm:", checksumAlgoBox, row++);
        }

        // Glue at bottom
        GridBagConstraints glueGbc = new GridBagConstraints();
        glueGbc.gridx = 0;
        glueGbc.gridy = row;
        glueGbc.gridwidth = 2;
        glueGbc.fill = GridBagConstraints.BOTH;
        glueGbc.weighty = 1.0;
        Component glue = Box.createVerticalGlue();
        layout.setConstraints(glue, glueGbc);
        contentPanel.add(glue);

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setOpaque(true);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // PREMIUM: Port status indicator at the bottom
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusPanel.setBackground(new Color(240, 240, 240));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        statusLabel = new JLabel("● Port Status: Unknown");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setForeground(new Color(120, 120, 120));
        statusPanel.add(statusLabel);
        statsLabel = new JLabel("");
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statsLabel.setForeground(new Color(120, 120, 120));
        statusPanel.add(Box.createHorizontalStrut(16));
        statusPanel.add(statsLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Content panel that implements Scrollable so it tracks the viewport width.
     * This prevents the gray area on the right side — the panel always fills
     * the full width of the scroll pane.
     */
    private static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        /**
         * Return true so the panel ALWAYS fills the full viewport width.
         * This eliminates the gray area on the right side.
         */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        } else if (e.getSource() == transmissionSettingsBtn) {
            openTransmissionSettings();
        }
    }

    /**
     * Test Connection — attempts to open the serial port with current settings.
     * Shows a dialog with the result.
     */
    private void testConnection() {
        String portName = String.valueOf(portBox.getSelectedItem());
        int baud = parseInt(String.valueOf(baudBox.getSelectedItem()), 9600);

        if (portName == null || portName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select a port first.",
                "Test Connection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Test Connection feature requires the server-side port check.\n" +
            "Port: " + portName + "\nBaud: " + baud + "\n\n" +
            "Save the channel and deploy to verify the port opens successfully.",
            "Test Connection",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void openTransmissionSettings() {
        String mode = (String) transmissionModeBox.getSelectedItem();
        SerialPortConfig config = getConfigFromFields();
        SerialTransmissionModeDialog dialog = new SerialTransmissionModeDialog(
                SwingUtilities.getWindowAncestor(this), mode, config);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            dialog.saveToConfig(config);
            if (properties == null) {
                properties = isSender ? new SerialDispatcherProperties() : new SerialReceiverProperties();
            }
            if (isSender) {
                ((SerialDispatcherProperties) properties).setPortConfig(config);
            } else {
                ((SerialReceiverProperties) properties).setPortConfig(config);
            }
        }
    }

    private SerialPortConfig getConfigFromFields() {
        if (properties != null) {
            SerialPortConfig existing = isSender
                    ? ((SerialDispatcherProperties) properties).getPortConfig()
                    : ((SerialReceiverProperties) properties).getPortConfig();
            if (existing != null) {
                return existing.clone();
            }
        }
        return new SerialPortConfig();
    }

    private void refreshPortList() {
        portBox.removeAllItems();
        for (int i = 1; i <= 20; i++) portBox.addItem("COM" + i);
        for (int i = 1; i <= 10; i++) {
            portBox.addItem("/dev/ttyUSB" + i);
            portBox.addItem("/dev/ttyACM" + i);
        }
        for (int i = 0; i <= 9; i++) {
            portBox.addItem("/dev/ttyS" + i);
        }
    }

    public ConnectorProperties getProperties() {
        if (properties == null) {
            properties = isSender ? new SerialDispatcherProperties() : new SerialReceiverProperties();
        }
        SerialPortConfig config;
        if (isSender) {
            SerialDispatcherProperties props = (SerialDispatcherProperties) properties;
            config = props.getPortConfig();
            props.setWaitForAckAfterWrite(waitAckBox.isSelected());
            props.setAckTimeout(parseInt(ackTimeoutField.getText(), 1000));
            props.setAckPattern(parseHex(ackPatternField.getText(), new byte[]{0x06}));
            props.setKeepConnectionOpen(keepOpenBox.isSelected());
        } else {
            SerialReceiverProperties props = (SerialReceiverProperties) properties;
            config = props.getPortConfig();
        }
        fillPortConfig(config);
        return properties;
    }

    public void setProperties(ConnectorProperties properties) {
        this.properties = properties;
        SerialPortConfig config = null;
        if (isSender) {
            SerialDispatcherProperties props = (SerialDispatcherProperties) properties;
            if (props != null) {
                config = props.getPortConfig();
                waitAckBox.setSelected(props.isWaitForAckAfterWrite());
                ackTimeoutField.setText(String.valueOf(props.getAckTimeout()));
                ackPatternField.setText(bytesToHex(props.getAckPattern()));
                keepOpenBox.setSelected(props.isKeepConnectionOpen());
            }
        } else {
            SerialReceiverProperties props = (SerialReceiverProperties) properties;
            if (props != null) {
                config = props.getPortConfig();
            }
        }
        if (config == null) {
            config = new SerialPortConfig();
        }

        portBox.setSelectedItem(config.getPortName());
        baudBox.setSelectedItem(String.valueOf(config.getBaudRate()));
        dataBitsBox.setSelectedItem(String.valueOf(config.getDataBits()));
        stopBitsBox.setSelectedItem(mapStopBitsToString(config.getStopBits()));
        parityBox.setSelectedIndex(config.getParity());
        flowBox.setSelectedIndex(config.getFlowControl());
        charsetBox.setSelectedItem(config.getCharset());
        binaryBox.setSelected(config.isBinaryMode());
        readTimeoutField.setText(String.valueOf(config.getReadTimeout()));
        writeTimeoutField.setText(String.valueOf(config.getWriteTimeout()));
        bufferSizeField.setText(String.valueOf(config.getBufferSize()));
        dtrBox.setSelected(config.isSetDtr());
        rtsBox.setSelected(config.isSetRts());
        waitCtsBox.setSelected(config.isWaitCts());
        waitDsrBox.setSelected(config.isWaitDsr());
        waitDcdBox.setSelected(config.isWaitDcd());
        signalTimeoutField.setText(String.valueOf(config.getSignalTimeout()));
        breakBox.setSelected(config.isSendBreak());
        breakDurField.setText(String.valueOf(config.getBreakDuration()));
        flushOpenBox.setSelected(config.isFlushOnOpen());
        flushCloseBox.setSelected(config.isFlushOnClose());
        autoDetectPortBox.setSelected(config.isAutoDetectPort());
        autoDetectBaudBox.setSelected(config.isAutoDetectBaud());

        transmissionModeBox.setSelectedItem(config.getTransmissionMode());

        healthBox.setSelected(config.isHealthMonitorEnabled());
        healthIntervalField.setText(String.valueOf(config.getHealthInterval()));
        maxReconnectField.setText(String.valueOf(config.getMaxReconnects()));
        reconnectDelayField.setText(String.valueOf(config.getReconnectDelay()));

        // Premium fields
        portAliasField.setText(config.getPortAlias() != null ? config.getPortAlias() : "");
        idleTimeoutField.setText(String.valueOf(config.getIdleTimeout()));
        receiveIdleTimeoutField.setText(String.valueOf(config.getReceiveIdleTimeout()));

        // PREMIUM: Destination extras (sender only)
        if (isSender) {
            processResponseBox.setSelected(config.isProcessResponse());
            responseDelimiterField.setText(config.getResponseDelimiter() != null ? config.getResponseDelimiter() : "\\r\\n");
            responseTimeoutField.setText(String.valueOf(config.getResponseTimeout()));
            useTemplateBox.setSelected(config.isUseTemplate());
            templateField.setText(config.getMessageTemplate() != null ? config.getMessageTemplate() : "");
            checksumAlgoBox.setSelectedItem(config.getChecksumAlgorithm() != null ? config.getChecksumAlgorithm() : "ASTM_STANDARD");
        }
    }

    private void fillPortConfig(SerialPortConfig config) {
        config.setPortName(String.valueOf(portBox.getSelectedItem()));
        config.setBaudRate(parseInt(String.valueOf(baudBox.getSelectedItem()), 9600));
        config.setDataBits(parseInt(String.valueOf(dataBitsBox.getSelectedItem()), 8));
        config.setStopBits(mapStopBitsFromString(String.valueOf(stopBitsBox.getSelectedItem())));
        config.setParity(parityBox.getSelectedIndex());
        config.setFlowControl(flowBox.getSelectedIndex());
        config.setCharset(String.valueOf(charsetBox.getSelectedItem()));
        config.setBinaryMode(binaryBox.isSelected());
        config.setPortAlias(portAliasField.getText());
        config.setReadTimeout(parseInt(readTimeoutField.getText(), 1000));
        config.setWriteTimeout(parseInt(writeTimeoutField.getText(), 1000));
        config.setBufferSize(parseInt(bufferSizeField.getText(), 4096));
        config.setIdleTimeout(parseInt(idleTimeoutField.getText(), 0));
        config.setReceiveIdleTimeout(parseInt(receiveIdleTimeoutField.getText(), 0));
        config.setSetDtr(dtrBox.isSelected());
        config.setSetRts(rtsBox.isSelected());
        config.setWaitCts(waitCtsBox.isSelected());
        config.setWaitDsr(waitDsrBox.isSelected());
        config.setWaitDcd(waitDcdBox.isSelected());
        config.setSignalTimeout(parseInt(signalTimeoutField.getText(), 1000));
        config.setSendBreak(breakBox.isSelected());
        config.setBreakDuration(parseInt(breakDurField.getText(), 100));
        config.setFlushOnOpen(flushOpenBox.isSelected());
        config.setFlushOnClose(flushCloseBox.isSelected());
        config.setAutoDetectPort(autoDetectPortBox.isSelected());
        config.setAutoDetectBaud(autoDetectBaudBox.isSelected());

        config.setTransmissionMode((String) transmissionModeBox.getSelectedItem());

        config.setHealthMonitorEnabled(healthBox.isSelected());
        config.setHealthInterval(parseInt(healthIntervalField.getText(), 5000));
        config.setMaxReconnects(parseInt(maxReconnectField.getText(), 10));
        config.setReconnectDelay(parseInt(reconnectDelayField.getText(), 5000));

        // PREMIUM: Destination extras (sender only)
        if (isSender) {
            config.setProcessResponse(processResponseBox.isSelected());
            config.setResponseDelimiter(responseDelimiterField.getText());
            config.setResponseTimeout(parseInt(responseTimeoutField.getText(), 5000));
            config.setUseTemplate(useTemplateBox.isSelected());
            config.setMessageTemplate(templateField.getText());
            config.setChecksumAlgorithm((String) checksumAlgoBox.getSelectedItem());
        }
    }

    private int mapStopBitsFromString(String s) {
        if ("1.5".equals(s)) return 3;
        return (int) parseDouble(s, 1);
    }

    private String mapStopBitsToString(int stopBits) {
        if (stopBits == 3) return "1.5";
        return String.valueOf(stopBits);
    }

    public ConnectorProperties getDefaults() {
        return isSender ? new SerialDispatcherProperties() : new SerialReceiverProperties();
    }

    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        SerialPortConfig config = null;
        if (isSender && properties instanceof SerialDispatcherProperties) {
            config = ((SerialDispatcherProperties) properties).getPortConfig();
        } else if (!isSender && properties instanceof SerialReceiverProperties) {
            config = ((SerialReceiverProperties) properties).getPortConfig();
        }
        if (config == null) {
            return false;
        }

        boolean valid = true;
        if (config.getPortName() == null || config.getPortName().trim().isEmpty()) {
            valid = false;
            if (highlight) portBox.setBackground(Color.PINK);
        } else {
            if (highlight) portBox.setBackground(Color.WHITE);
        }
        if (config.getBaudRate() <= 0) {
            valid = false;
            if (highlight) baudBox.setBackground(Color.PINK);
        } else {
            if (highlight) baudBox.setBackground(Color.WHITE);
        }
        if (config.getDataBits() < 5 || config.getDataBits() > 8) valid = false;
        if (config.getReadTimeout() < 0 || config.getWriteTimeout() < 0) valid = false;
        if (isSender && properties instanceof SerialDispatcherProperties) {
            SerialDispatcherProperties dp = (SerialDispatcherProperties) properties;
            if (dp.isWaitForAckAfterWrite() && (dp.getAckPattern() == null || dp.getAckPattern().length == 0)) {
                valid = false;
            }
        }
        return valid;
    }

    public void resetInvalidProperties() {
        portBox.setBackground(Color.WHITE);
        baudBox.setBackground(Color.WHITE);
        dataBitsBox.setBackground(Color.WHITE);
        stopBitsBox.setBackground(Color.WHITE);
        parityBox.setBackground(Color.WHITE);
        flowBox.setBackground(Color.WHITE);
        charsetBox.setBackground(Color.WHITE);
        readTimeoutField.setBackground(Color.WHITE);
        writeTimeoutField.setBackground(Color.WHITE);
        bufferSizeField.setBackground(Color.WHITE);
        signalTimeoutField.setBackground(Color.WHITE);
        breakDurField.setBackground(Color.WHITE);
        healthIntervalField.setBackground(Color.WHITE);
        maxReconnectField.setBackground(Color.WHITE);
        reconnectDelayField.setBackground(Color.WHITE);
        if (isSender) {
            ackTimeoutField.setBackground(Color.WHITE);
            ackPatternField.setBackground(Color.WHITE);
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private byte[] parseHex(String s, byte[] def) {
        if (s == null || s.trim().isEmpty()) return def;
        try {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) { return def; }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
