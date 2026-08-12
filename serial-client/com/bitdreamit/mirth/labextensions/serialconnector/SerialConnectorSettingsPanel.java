package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Universal Serial Connector Settings Panel.
 * Transmission-mode agnostic — works with any Mirth DataType (HL7, ASTM, Delimited, XML, etc.)
 */
public class SerialConnectorSettingsPanel extends ConnectorSettingsPanel implements ActionListener {

    private boolean isSender;

    // Transmission
    private MirthComboBox transmissionModeBox;
    private JTextField messageDelimiterField;

    // Basic
    private MirthComboBox portBox;
    private JButton refreshPortsBtn;
    private MirthCheckBox autoDetectPortBox;
    private MirthComboBox baudBox;
    private MirthCheckBox autoDetectBaudBox;
    private MirthComboBox dataBitsBox;
    private MirthComboBox stopBitsBox;
    private MirthComboBox parityBox;
    private MirthComboBox flowBox;
    private MirthComboBox charsetBox;
    private MirthCheckBox binaryBox;

    // Timeouts
    private JTextField readTimeoutField;
    private JTextField writeTimeoutField;
    private JTextField bufferSizeField;

    // Signals
    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JTextField signalTimeoutField;

    // Break & Flush
    private MirthCheckBox breakBox;
    private JTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;

    // Health Monitor
    private MirthCheckBox healthBox;
    private JTextField healthIntervalField;
    private JTextField maxReconnectField;
    private JTextField reconnectDelayField;

    // Protocol Analyzer
    private MirthCheckBox analyzerBox;
    private JTextField maxLogField;

    // Destination extras
    private MirthCheckBox waitAckBox;
    private JTextField ackTimeoutField;
    private MirthCheckBox keepOpenBox;
    private JTextField ackPatternField;

    public SerialConnectorSettingsPanel() {
        this(false);
    }

    public SerialConnectorSettingsPanel(boolean isSender) {
        this.isSender = isSender;
        initComponents();
        refreshPortList();
    }

    @Override
    public String getConnectorName() {
        return isSender ? "Serial Writer" : "Serial Reader";
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(createTransmissionPanel(), gbc); gbc.gridy++;
        add(createBasicPanel(), gbc); gbc.gridy++;
        add(createTimeoutPanel(), gbc); gbc.gridy++;
        add(createSignalPanel(), gbc); gbc.gridy++;
        add(createBreakPanel(), gbc); gbc.gridy++;
        add(createHealthPanel(), gbc); gbc.gridy++;
        add(createAnalyzerPanel(), gbc); gbc.gridy++;
        if (isSender) {
            add(createDestPanel(), gbc); gbc.gridy++;
        }

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createGlue(), gbc);
    }

    private JPanel createTransmissionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Transmission Mode (Transport Layer)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Mode:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        transmissionModeBox = new MirthComboBox();
        transmissionModeBox.setModel(new DefaultComboBoxModel<>(
            new String[]{"BASIC", "MLLP", "ASTM_E1381"}
        ));
        transmissionModeBox.setToolTipText(
            "<html>BASIC = Raw/delimited bytes (works with any DataType)<br>" +
            "MLLP = HL7 Minimal Lower Layer Protocol framing<br>" +
            "ASTM_E1381 = ASTM E1381 session + frame protocol</html>"
        );
        transmissionModeBox.addActionListener(this);
        p.add(transmissionModeBox, g);

        g.gridy = 1; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Delimiter (BASIC only):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        messageDelimiterField = new JTextField("\\r\\n", 10);
        messageDelimiterField.setToolTipText("Escape sequences: \\r \\n \\t");
        p.add(messageDelimiterField, g);

        return p;
    }

    private JPanel createBasicPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Basic Settings"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        int row = 0;

        g.gridy = row; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Port:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        portBox = new MirthComboBox();
        portBox.setEditable(true);
        p.add(portBox, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        refreshPortsBtn = new JButton("Refresh");
        refreshPortsBtn.setToolTipText("Shows common ports. Server auto-detect works at runtime.");
        refreshPortsBtn.addActionListener(this);
        p.add(refreshPortsBtn, g);
        g.gridx = 3;
        autoDetectPortBox = new MirthCheckBox("Auto-detect");
        autoDetectPortBox.setBackground(Color.WHITE);
        p.add(autoDetectPortBox, g);
        row++;

        g.gridy = row; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Baud Rate:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        baudBox = new MirthComboBox();
        baudBox.setModel(new DefaultComboBoxModel<>(new String[]{"9600", "19200", "38400", "57600", "115200", "230400"}));
        p.add(baudBox, g);
        g.gridx = 2; g.gridwidth = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        autoDetectBaudBox = new MirthCheckBox("Auto-detect");
        autoDetectBaudBox.setBackground(Color.WHITE);
        p.add(autoDetectBaudBox, g);
        g.gridwidth = 1;
        row++;

        g.gridy = row; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Data Bits:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        dataBitsBox = new MirthComboBox();
        dataBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "6", "7", "8"}));
        dataBitsBox.setSelectedItem("8");
        p.add(dataBitsBox, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Stop Bits:"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        stopBitsBox = new MirthComboBox();
        stopBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"1", "1.5", "2"}));
        p.add(stopBitsBox, g);
        row++;

        g.gridy = row; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Parity:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        parityBox = new MirthComboBox();
        parityBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "Odd", "Even", "Mark", "Space"}));
        p.add(parityBox, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Flow Control:"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        flowBox = new MirthComboBox();
        flowBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"}));
        p.add(flowBox, g);
        row++;

        g.gridy = row; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Charset:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        charsetBox = new MirthComboBox();
        charsetBox.setModel(new DefaultComboBoxModel<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "Windows-1252"}));
        p.add(charsetBox, g);
        g.gridx = 2; g.gridwidth = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        binaryBox = new MirthCheckBox("Binary Mode (hex)");
        binaryBox.setBackground(Color.WHITE);
        p.add(binaryBox, g);
        g.gridwidth = 1;

        return p;
    }

    private JPanel createTimeoutPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Timeouts & Buffers"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Read Timeout (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        readTimeoutField = new JTextField("1000", 8);
        p.add(readTimeoutField, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Write Timeout (ms):"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        writeTimeoutField = new JTextField("1000", 8);
        p.add(writeTimeoutField, g);

        g.gridy = 1; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Buffer Size:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        bufferSizeField = new JTextField("4096", 8);
        p.add(bufferSizeField, g);
        g.gridwidth = 1;

        return p;
    }

    private JPanel createSignalPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Signal Control"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0;
        dtrBox = new MirthCheckBox("Set DTR");
        dtrBox.setSelected(true);
        dtrBox.setBackground(Color.WHITE);
        p.add(dtrBox, g);
        g.gridx = 1;
        rtsBox = new MirthCheckBox("Set RTS");
        rtsBox.setSelected(true);
        rtsBox.setBackground(Color.WHITE);
        p.add(rtsBox, g);
        g.gridx = 2;
        waitCtsBox = new MirthCheckBox("Wait for CTS");
        waitCtsBox.setBackground(Color.WHITE);
        p.add(waitCtsBox, g);
        g.gridx = 3;
        waitDsrBox = new MirthCheckBox("Wait for DSR");
        waitDsrBox.setBackground(Color.WHITE);
        p.add(waitDsrBox, g);

        g.gridy = 1; g.gridx = 0;
        waitDcdBox = new MirthCheckBox("Wait for DCD");
        waitDcdBox.setBackground(Color.WHITE);
        p.add(waitDcdBox, g);
        g.gridx = 1; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL;
        JPanel timeoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timeoutRow.setBackground(Color.WHITE);
        timeoutRow.add(new JLabel("Signal Wait Timeout (ms):"));
        signalTimeoutField = new JTextField("1000", 8);
        timeoutRow.add(signalTimeoutField);
        p.add(timeoutRow, g);
        g.gridwidth = 1; g.fill = GridBagConstraints.NONE;

        return p;
    }

    private JPanel createBreakPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Break & Flush"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 4;
        breakBox = new MirthCheckBox("Send Break Before Open");
        breakBox.setBackground(Color.WHITE);
        p.add(breakBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel("Break Duration (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        breakDurField = new JTextField("100", 8);
        p.add(breakDurField, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        flushOpenBox = new MirthCheckBox("Flush Buffers on Open");
        flushOpenBox.setSelected(true);
        flushOpenBox.setBackground(Color.WHITE);
        p.add(flushOpenBox, g);
        g.gridx = 3;
        flushCloseBox = new MirthCheckBox("Flush Buffers on Close");
        flushCloseBox.setSelected(true);
        flushCloseBox.setBackground(Color.WHITE);
        p.add(flushCloseBox, g);

        return p;
    }

    private JPanel createHealthPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Health Monitor"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 4;
        healthBox = new MirthCheckBox("Enable Health Monitor & Auto-Reconnect");
        healthBox.setSelected(true);
        healthBox.setBackground(Color.WHITE);
        p.add(healthBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel("Check Interval (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        healthIntervalField = new JTextField("30000", 8);
        p.add(healthIntervalField, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Max Reconnect:"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        maxReconnectField = new JTextField("10", 8);
        p.add(maxReconnectField, g);

        g.gridy = 2; g.gridx = 0; g.weightx = 0;
        p.add(new JLabel("Reconnect Delay (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        reconnectDelayField = new JTextField("5000", 8);
        p.add(reconnectDelayField, g);
        g.gridwidth = 1;

        return p;
    }

    private JPanel createAnalyzerPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Protocol Analyzer"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 1;
        analyzerBox = new MirthCheckBox("Enable Protocol Analyzer");
        analyzerBox.setBackground(Color.WHITE);
        p.add(analyzerBox, g);
        g.gridx = 1; g.weightx = 0;
        p.add(new JLabel("Max Log Entries:"), g);
        g.gridx = 2; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        maxLogField = new JTextField("1000", 8);
        p.add(maxLogField, g);
        g.fill = GridBagConstraints.NONE;

        return p;
    }

    private JPanel createDestPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Destination Options"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 4;
        keepOpenBox = new MirthCheckBox("Keep Connection Open (Pool)");
        keepOpenBox.setBackground(Color.WHITE);
        p.add(keepOpenBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 4;
        waitAckBox = new MirthCheckBox("Wait for ACK After Write");
        waitAckBox.setBackground(Color.WHITE);
        p.add(waitAckBox, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel("ACK Timeout (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        ackTimeoutField = new JTextField("1000", 8);
        p.add(ackTimeoutField, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("ACK Pattern (hex):"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        ackPatternField = new JTextField("06", 8);
        p.add(ackPatternField, g);

        return p;
    }

    private void refreshPortList() {
        portBox.removeAllItems();
        portBox.addItem("");
        String[] commonPorts = {"COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8",
                "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS3",
                "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2",
                "/dev/ttyACM0", "/dev/ttyACM1"};
        for (String port : commonPorts) {
            portBox.addItem(port);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        }
        if (e.getSource() == transmissionModeBox) {
            updateTransmissionUI();
        }
    }

    private void updateTransmissionUI() {
        String mode = (String) transmissionModeBox.getSelectedItem();
        boolean isBasic = "BASIC".equals(mode);
        messageDelimiterField.setEnabled(isBasic);
    }

    @Override
    public ConnectorProperties getProperties() {
        if (isSender) {
            SerialDispatcherProperties p = new SerialDispatcherProperties();
            fillConfig(p.getPortConfig());
            p.setTransmissionMode((String) transmissionModeBox.getSelectedItem());
            p.setMessageDelimiter(messageDelimiterField.getText());
            p.setWaitForAckAfterWrite(waitAckBox.isSelected());
            try { p.setAckTimeout(Integer.parseInt(ackTimeoutField.getText())); } catch (Exception ignored) {}
            try { p.setAckPattern(hexToBytes(ackPatternField.getText())); } catch (Exception ignored) {}
            p.setKeepConnectionOpen(keepOpenBox.isSelected());
            return p;
        } else {
            SerialReceiverProperties p = new SerialReceiverProperties();
            fillConfig(p.getPortConfig());
            p.setTransmissionMode((String) transmissionModeBox.getSelectedItem());
            p.setMessageDelimiter(messageDelimiterField.getText());
            return p;
        }
    }

    private void fillConfig(SerialPortConfig c) {
        c.setPortName(portBox.getSelectedItem() != null ? portBox.getSelectedItem().toString() : "");
        c.setAutoDetectPort(autoDetectPortBox.isSelected());
        try { c.setBaudRate(Integer.parseInt((String) baudBox.getSelectedItem())); } catch (Exception ignored) {}
        c.setAutoDetectBaud(autoDetectBaudBox.isSelected());
        try { c.setDataBits(Integer.parseInt((String) dataBitsBox.getSelectedItem())); } catch (Exception ignored) {}
        c.setStopBits(stopBitsBox.getSelectedIndex() + 1);
        c.setParity(parityBox.getSelectedIndex());
        c.setFlowControl(flowBox.getSelectedIndex());
        c.setCharsetEncoding((String) charsetBox.getSelectedItem());
        c.setBinaryMode(binaryBox.isSelected());

        try { c.setReadTimeout(Integer.parseInt(readTimeoutField.getText())); } catch (Exception ignored) {}
        try { c.setWriteTimeout(Integer.parseInt(writeTimeoutField.getText())); } catch (Exception ignored) {}
        try { c.setBufferSize(Integer.parseInt(bufferSizeField.getText())); } catch (Exception ignored) {}

        c.setSetDTR(dtrBox.isSelected());
        c.setSetRTS(rtsBox.isSelected());
        c.setWaitForCTS(waitCtsBox.isSelected());
        c.setWaitForDSR(waitDsrBox.isSelected());
        c.setWaitForDCD(waitDcdBox.isSelected());
        try { c.setSignalWaitTimeout(Integer.parseInt(signalTimeoutField.getText())); } catch (Exception ignored) {}

        c.setSendBreakBeforeOpen(breakBox.isSelected());
        try { c.setBreakDuration(Integer.parseInt(breakDurField.getText())); } catch (Exception ignored) {}
        c.setFlushBuffersOnOpen(flushOpenBox.isSelected());
        c.setFlushBuffersOnClose(flushCloseBox.isSelected());

        c.setEnableHealthMonitor(healthBox.isSelected());
        try { c.setHealthCheckInterval(Integer.parseInt(healthIntervalField.getText())); } catch (Exception ignored) {}
        try { c.setMaxReconnectAttempts(Integer.parseInt(maxReconnectField.getText())); } catch (Exception ignored) {}
        try { c.setReconnectDelay(Integer.parseInt(reconnectDelayField.getText())); } catch (Exception ignored) {}

        c.setEnableProtocolAnalyzer(analyzerBox.isSelected());
        try { c.setMaxProtocolLogEntries(Integer.parseInt(maxLogField.getText())); } catch (Exception ignored) {}
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        if (isSender && properties instanceof SerialDispatcherProperties) {
            SerialDispatcherProperties p = (SerialDispatcherProperties) properties;
            loadConfig(p.getPortConfig());
            transmissionModeBox.setSelectedItem(p.getTransmissionMode());
            messageDelimiterField.setText(p.getMessageDelimiter());
            waitAckBox.setSelected(p.isWaitForAckAfterWrite());
            ackTimeoutField.setText(String.valueOf(p.getAckTimeout()));
            ackPatternField.setText(bytesToHex(p.getAckPattern()));
            keepOpenBox.setSelected(p.isKeepConnectionOpen());
        } else if (!isSender && properties instanceof SerialReceiverProperties) {
            SerialReceiverProperties p = (SerialReceiverProperties) properties;
            loadConfig(p.getPortConfig());
            transmissionModeBox.setSelectedItem(p.getTransmissionMode());
            messageDelimiterField.setText(p.getMessageDelimiter());
        }
        updateTransmissionUI();
    }

    private void loadConfig(SerialPortConfig c) {
        portBox.setSelectedItem(c.getPortName());
        autoDetectPortBox.setSelected(c.isAutoDetectPort());
        baudBox.setSelectedItem(String.valueOf(c.getBaudRate()));
        autoDetectBaudBox.setSelected(c.isAutoDetectBaud());
        dataBitsBox.setSelectedItem(String.valueOf(c.getDataBits()));
        stopBitsBox.setSelectedIndex(Math.max(0, c.getStopBits() - 1));
        parityBox.setSelectedIndex(c.getParity());
        flowBox.setSelectedIndex(c.getFlowControl());
        charsetBox.setSelectedItem(c.getCharsetEncoding());
        binaryBox.setSelected(c.isBinaryMode());

        readTimeoutField.setText(String.valueOf(c.getReadTimeout()));
        writeTimeoutField.setText(String.valueOf(c.getWriteTimeout()));
        bufferSizeField.setText(String.valueOf(c.getBufferSize()));

        dtrBox.setSelected(c.isSetDTR());
        rtsBox.setSelected(c.isSetRTS());
        waitCtsBox.setSelected(c.isWaitForCTS());
        waitDsrBox.setSelected(c.isWaitForDSR());
        waitDcdBox.setSelected(c.isWaitForDCD());
        signalTimeoutField.setText(String.valueOf(c.getSignalWaitTimeout()));

        breakBox.setSelected(c.isSendBreakBeforeOpen());
        breakDurField.setText(String.valueOf(c.getBreakDuration()));
        flushOpenBox.setSelected(c.isFlushBuffersOnOpen());
        flushCloseBox.setSelected(c.isFlushBuffersOnClose());

        healthBox.setSelected(c.isEnableHealthMonitor());
        healthIntervalField.setText(String.valueOf(c.getHealthCheckInterval()));
        maxReconnectField.setText(String.valueOf(c.getMaxReconnectAttempts()));
        reconnectDelayField.setText(String.valueOf(c.getReconnectDelay()));

        analyzerBox.setSelected(c.isEnableProtocolAnalyzer());
        maxLogField.setText(String.valueOf(c.getMaxProtocolLogEntries()));
    }

    @Override
    public ConnectorProperties getDefaults() {
        return isSender ? new SerialDispatcherProperties() : new SerialReceiverProperties();
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        boolean valid = true;
        SerialPortConfig c;
        if (isSender) {
            c = ((SerialDispatcherProperties) properties).getPortConfig();
        } else {
            c = ((SerialReceiverProperties) properties).getPortConfig();
        }

        if (c.getPortName() == null || c.getPortName().trim().isEmpty()) {
            valid = false;
            if (highlight) portBox.setBackground(Color.PINK);
        } else {
            if (highlight) portBox.setBackground(Color.WHITE);
        }

        if (c.getBaudRate() <= 0) {
            valid = false;
            if (highlight) baudBox.setBackground(Color.PINK);
        } else {
            if (highlight) baudBox.setBackground(Color.WHITE);
        }

        if (c.getDataBits() < 5 || c.getDataBits() > 8) valid = false;
        if (c.getReadTimeout() < 0 || c.getWriteTimeout() < 0) valid = false;
        if (c.getHealthCheckInterval() < 1000) valid = false;

        if (isSender) {
            SerialDispatcherProperties dp = (SerialDispatcherProperties) properties;
            if (dp.isWaitForAckAfterWrite()) {
                try {
                    hexToBytes(ackPatternField.getText());
                } catch (Exception e) {
                    valid = false;
                    if (highlight) ackPatternField.setBackground(Color.PINK);
                }
            }
        }

        return valid;
    }

    @Override
    public void resetInvalidProperties() {
        portBox.setBackground(Color.WHITE);
        baudBox.setBackground(Color.WHITE);
        ackPatternField.setBackground(Color.WHITE);
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

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
