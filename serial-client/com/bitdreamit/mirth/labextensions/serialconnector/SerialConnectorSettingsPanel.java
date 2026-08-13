package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Serial Connector Settings Panel — redesigned to match Mirth's TCP connector UI style.
 *
 * Layout follows Mirth conventions:
 *  - Titled bordered sections (like TCP Listener/Sender panels)
 *  - Compact GridBagLayout with consistent insets
 *  - White background, Mirth blue accents
 *  - Labels right-aligned, fields left-aligned
 *  - Grouped related settings (Connection, Data Format, Advanced, etc.)
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 */
public class SerialConnectorSettingsPanel extends JPanel implements ActionListener {

    private boolean isSender;
    private ConnectorProperties properties;

    // Connection section
    private MirthComboBox portBox;
    private JButton refreshPortsBtn;
    private MirthComboBox baudBox;
    private MirthComboBox dataBitsBox;
    private MirthComboBox stopBitsBox;
    private MirthComboBox parityBox;
    private MirthComboBox flowBox;

    // Data format section
    private MirthComboBox charsetBox;
    private MirthCheckBox binaryBox;
    private MirthComboBox transmissionModeBox;
    private JButton transmissionSettingsBtn;

    // Timeouts section
    private JTextField readTimeoutField;
    private JTextField writeTimeoutField;
    private JTextField bufferSizeField;

    // Signal control section
    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JTextField signalTimeoutField;

    // Advanced section
    private MirthCheckBox breakBox;
    private JTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;
    private MirthCheckBox autoDetectPortBox;
    private MirthCheckBox autoDetectBaudBox;

    // Health monitor section
    private MirthCheckBox healthBox;
    private JTextField healthIntervalField;
    private JTextField maxReconnectField;
    private JTextField reconnectDelayField;

    // Destination-only section
    private MirthCheckBox waitAckBox;
    private JTextField ackTimeoutField;
    private MirthCheckBox keepOpenBox;
    private JTextField ackPatternField;

    // Colors matching Mirth's theme
    private static final Color SECTION_BG = new Color(248, 248, 248);
    private static final Color BORDER_COLOR = new Color(180, 180, 180);
    private static final Color LABEL_COLOR = new Color(60, 60, 60);

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

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Main scrollable content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.anchor = GridBagConstraints.NORTHWEST;
        outerGbc.fill = GridBagConstraints.HORIZONTAL;
        outerGbc.weightx = 1.0;
        outerGbc.weighty = 0.0;
        outerGbc.insets = new Insets(0, 0, 8, 0);

        int row = 0;

        // Section 1: Connection
        outerGbc.gridy = row++;
        contentPanel.add(createConnectionSection(), outerGbc);

        // Section 2: Data Format
        outerGbc.gridy = row++;
        contentPanel.add(createDataFormatSection(), outerGbc);

        // Section 3: Timeouts & Buffer
        outerGbc.gridy = row++;
        contentPanel.add(createTimeoutsSection(), outerGbc);

        // Section 4: Signal Control
        outerGbc.gridy = row++;
        contentPanel.add(createSignalSection(), outerGbc);

        // Section 5: Advanced
        outerGbc.gridy = row++;
        contentPanel.add(createAdvancedSection(), outerGbc);

        // Section 6: Health Monitor
        outerGbc.gridy = row++;
        contentPanel.add(createHealthSection(), outerGbc);

        // Section 7: Destination Options (sender only)
        if (isSender) {
            outerGbc.gridy = row++;
            contentPanel.add(createDestinationSection(), outerGbc);
        }

        // Add glue at bottom
        outerGbc.gridy = row++;
        outerGbc.weighty = 1.0;
        outerGbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(Box.createVerticalGlue(), outerGbc);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Creates a titled bordered section like Mirth's TCP panels.
     */
    private JPanel createSection(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.BOLD, 11),
                LABEL_COLOR
        );
        panel.setBorder(BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(4, 8, 8, 8)
        ));
        return panel;
    }

    private GridBagConstraints mkGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.EAST;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.weighty = 0;
        return g;
    }

    private JLabel mkLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(LABEL_COLOR);
        return label;
    }

    // ===== Section: Connection =====

    private JPanel createConnectionSection() {
        JPanel p = createSection("Connection");

        GridBagConstraints g = mkGbc();

        // Row 0: Port + Refresh
        g.gridy = 0;
        g.gridx = 0; p.add(mkLabel("Port:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        portBox = new MirthComboBox();
        portBox.setEditable(true);
        portBox.setPreferredSize(new Dimension(120, 22));
        p.add(portBox, g);
        g.gridx = 2;
        refreshPortsBtn = new JButton("Refresh");
        refreshPortsBtn.setToolTipText("Refresh available serial ports");
        refreshPortsBtn.setMargin(new Insets(2, 6, 2, 6));
        refreshPortsBtn.addActionListener(this);
        p.add(refreshPortsBtn, g);
        g.gridx = 3;
        autoDetectPortBox = new MirthCheckBox("Auto-detect");
        p.add(autoDetectPortBox, g);

        // Row 1: Baud Rate + Auto-detect
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Baud Rate:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        baudBox = new MirthComboBox();
        baudBox.setModel(new DefaultComboBoxModel<>(new String[]{"9600", "19200", "38400", "57600", "115200", "230400"}));
        baudBox.setPreferredSize(new Dimension(120, 22));
        p.add(baudBox, g);
        g.gridx = 2; g.gridwidth = 2;
        autoDetectBaudBox = new MirthCheckBox("Auto-detect");
        p.add(autoDetectBaudBox, g);
        g.gridwidth = 1;

        // Row 2: Data Bits + Stop Bits
        g.gridy = 2;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Data Bits:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        dataBitsBox = new MirthComboBox();
        dataBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "6", "7", "8"}));
        dataBitsBox.setSelectedItem("8");
        dataBitsBox.setPreferredSize(new Dimension(60, 22));
        p.add(dataBitsBox, g);
        g.gridx = 2; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Stop Bits:"), g);
        g.gridx = 3; g.anchor = GridBagConstraints.WEST;
        stopBitsBox = new MirthComboBox();
        stopBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"1", "1.5", "2"}));
        stopBitsBox.setPreferredSize(new Dimension(60, 22));
        p.add(stopBitsBox, g);

        // Row 3: Parity + Flow Control
        g.gridy = 3;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Parity:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        parityBox = new MirthComboBox();
        parityBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "Odd", "Even", "Mark", "Space"}));
        parityBox.setPreferredSize(new Dimension(80, 22));
        p.add(parityBox, g);
        g.gridx = 2; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Flow Control:"), g);
        g.gridx = 3; g.anchor = GridBagConstraints.WEST;
        flowBox = new MirthComboBox();
        flowBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"}));
        flowBox.setPreferredSize(new Dimension(100, 22));
        p.add(flowBox, g);

        return p;
    }

    // ===== Section: Data Format =====

    private JPanel createDataFormatSection() {
        JPanel p = createSection("Data Format");

        GridBagConstraints g = mkGbc();

        // Row 0: Charset + Binary
        g.gridy = 0;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Charset:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        charsetBox = new MirthComboBox();
        charsetBox.setModel(new DefaultComboBoxModel<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "Windows-1252"}));
        charsetBox.setPreferredSize(new Dimension(120, 22));
        p.add(charsetBox, g);
        g.gridx = 2; g.gridwidth = 2;
        binaryBox = new MirthCheckBox("Binary (Base64)");
        p.add(binaryBox, g);
        g.gridwidth = 1;

        // Row 1: Transmission Mode + Settings button
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Transmission Mode:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        transmissionModeBox = new MirthComboBox();
        transmissionModeBox.setModel(new DefaultComboBoxModel<>(new String[]{"RAW", "LINE", "FRAME", "MLLP", "ASTM"}));
        transmissionModeBox.setPreferredSize(new Dimension(120, 22));
        transmissionModeBox.addActionListener(this);
        p.add(transmissionModeBox, g);
        g.gridx = 2; g.gridwidth = 2;
        transmissionSettingsBtn = new JButton("Configure...");
        transmissionSettingsBtn.setToolTipText("Configure transmission mode framing bytes");
        transmissionSettingsBtn.setMargin(new Insets(2, 8, 2, 8));
        transmissionSettingsBtn.addActionListener(this);
        p.add(transmissionSettingsBtn, g);
        g.gridwidth = 1;

        return p;
    }

    // ===== Section: Timeouts & Buffer =====

    private JPanel createTimeoutsSection() {
        JPanel p = createSection("Timeouts & Buffer");

        GridBagConstraints g = mkGbc();

        // Row 0: Read Timeout + Write Timeout
        g.gridy = 0;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Read Timeout (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        readTimeoutField = new JTextField("1000", 8);
        readTimeoutField.setPreferredSize(new Dimension(80, 22));
        p.add(readTimeoutField, g);
        g.gridx = 2; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Write Timeout (ms):"), g);
        g.gridx = 3; g.anchor = GridBagConstraints.WEST;
        writeTimeoutField = new JTextField("1000", 8);
        writeTimeoutField.setPreferredSize(new Dimension(80, 22));
        p.add(writeTimeoutField, g);

        // Row 1: Buffer Size
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Buffer Size:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        bufferSizeField = new JTextField("4096", 8);
        bufferSizeField.setPreferredSize(new Dimension(80, 22));
        p.add(bufferSizeField, g);

        return p;
    }

    // ===== Section: Signal Control =====

    private JPanel createSignalSection() {
        JPanel p = createSection("Signal Control");

        GridBagConstraints g = mkGbc();

        // Row 0: DTR + RTS
        g.gridy = 0;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Output Signals:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        JPanel signalOutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        signalOutPanel.setBackground(Color.WHITE);
        dtrBox = new MirthCheckBox("DTR");
        dtrBox.setSelected(true);
        signalOutPanel.add(dtrBox);
        rtsBox = new MirthCheckBox("RTS");
        rtsBox.setSelected(true);
        signalOutPanel.add(rtsBox);
        p.add(signalOutPanel, g);

        // Row 1: Wait CTS/DSR/DCD
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Wait For:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        JPanel waitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        waitPanel.setBackground(Color.WHITE);
        waitCtsBox = new MirthCheckBox("CTS");
        waitPanel.add(waitCtsBox);
        waitDsrBox = new MirthCheckBox("DSR");
        waitPanel.add(waitDsrBox);
        waitDcdBox = new MirthCheckBox("DCD");
        waitPanel.add(waitDcdBox);
        p.add(waitPanel, g);

        // Row 2: Signal Timeout
        g.gridy = 2;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Signal Timeout (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        signalTimeoutField = new JTextField("1000", 8);
        signalTimeoutField.setPreferredSize(new Dimension(80, 22));
        p.add(signalTimeoutField, g);

        return p;
    }

    // ===== Section: Advanced =====

    private JPanel createAdvancedSection() {
        JPanel p = createSection("Advanced");

        GridBagConstraints g = mkGbc();

        // Row 0: Send Break
        g.gridy = 0;
        g.gridx = 0; g.gridwidth = 4; g.anchor = GridBagConstraints.WEST;
        breakBox = new MirthCheckBox("Send Break Before Open");
        p.add(breakBox, g);
        g.gridwidth = 1;

        // Row 1: Break Duration
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Break Duration (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        breakDurField = new JTextField("100", 8);
        breakDurField.setPreferredSize(new Dimension(80, 22));
        p.add(breakDurField, g);

        // Row 2: Flush options
        g.gridy = 2;
        g.gridx = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST;
        flushOpenBox = new MirthCheckBox("Flush on Open");
        flushOpenBox.setSelected(true);
        p.add(flushOpenBox, g);
        g.gridx = 2; g.gridwidth = 2;
        flushCloseBox = new MirthCheckBox("Flush on Close");
        flushCloseBox.setSelected(true);
        p.add(flushCloseBox, g);
        g.gridwidth = 1;

        return p;
    }

    // ===== Section: Health Monitor =====

    private JPanel createHealthSection() {
        JPanel p = createSection("Health Monitor");

        GridBagConstraints g = mkGbc();

        // Row 0: Enable
        g.gridy = 0;
        g.gridx = 0; g.gridwidth = 4; g.anchor = GridBagConstraints.WEST;
        healthBox = new MirthCheckBox("Enable Auto-Reconnect");
        healthBox.setSelected(true);
        p.add(healthBox, g);
        g.gridwidth = 1;

        // Row 1: Interval + Max Retry
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Interval (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        healthIntervalField = new JTextField("5000", 8);
        healthIntervalField.setPreferredSize(new Dimension(80, 22));
        p.add(healthIntervalField, g);
        g.gridx = 2; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Max Retry:"), g);
        g.gridx = 3; g.anchor = GridBagConstraints.WEST;
        maxReconnectField = new JTextField("10", 8);
        maxReconnectField.setPreferredSize(new Dimension(60, 22));
        p.add(maxReconnectField, g);

        // Row 2: Retry Delay
        g.gridy = 2;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Retry Delay (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        reconnectDelayField = new JTextField("5000", 8);
        reconnectDelayField.setPreferredSize(new Dimension(80, 22));
        p.add(reconnectDelayField, g);

        return p;
    }

    // ===== Section: Destination Options (sender only) =====

    private JPanel createDestinationSection() {
        JPanel p = createSection("Destination Options");

        GridBagConstraints g = mkGbc();

        // Row 0: Wait for ACK + Keep Connection Open
        g.gridy = 0;
        g.gridx = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST;
        waitAckBox = new MirthCheckBox("Wait for ACK");
        p.add(waitAckBox, g);
        g.gridx = 2; g.gridwidth = 2;
        keepOpenBox = new MirthCheckBox("Keep Connection Open");
        p.add(keepOpenBox, g);
        g.gridwidth = 1;

        // Row 1: ACK Timeout + ACK Pattern
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("ACK Timeout (ms):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        ackTimeoutField = new JTextField("1000", 8);
        ackTimeoutField.setPreferredSize(new Dimension(80, 22));
        p.add(ackTimeoutField, g);
        g.gridx = 2; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("ACK Pattern (hex):"), g);
        g.gridx = 3; g.anchor = GridBagConstraints.WEST;
        ackPatternField = new JTextField("06", 8);
        ackPatternField.setPreferredSize(new Dimension(60, 22));
        p.add(ackPatternField, g);

        return p;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        } else if (e.getSource() == transmissionSettingsBtn) {
            openTransmissionSettings();
        }
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
        // Static port list — jSerialComm is a SERVER-only library (type="SERVER" in plugin.xml),
        // so we can't call SerialPort.getCommPorts() on the client side.
        // The user can type a custom port name if theirs isn't listed.
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
        config.setReadTimeout(parseInt(readTimeoutField.getText(), 1000));
        config.setWriteTimeout(parseInt(writeTimeoutField.getText(), 1000));
        config.setBufferSize(parseInt(bufferSizeField.getText(), 4096));
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
