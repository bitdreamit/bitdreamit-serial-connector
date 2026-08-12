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

public class SerialConnectorSettingsPanel extends ConnectorSettingsPanel implements ActionListener {

    private boolean isSender;

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

    private JTextField readTimeoutField;
    private JTextField writeTimeoutField;
    private JTextField bufferSizeField;

    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JTextField signalTimeoutField;

    private MirthCheckBox breakBox;
    private JTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;

    private MirthCheckBox healthBox;
    private JTextField healthIntervalField;
    private JTextField maxReconnectField;
    private JTextField reconnectDelayField;

    private MirthCheckBox analyzerBox;
    private JTextField maxLogField;

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
        binaryBox = new MirthCheckBox("Binary Mode (Base64)");
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

        g.gridy = 1; g.gridx = 0; g.gridwidth = 2;
        JPanel breakRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        breakRow.setBackground(Color.WHITE);
        breakRow.add(new JLabel("Break Duration (ms):"));
        breakDurField = new JTextField("100", 8);
        breakRow.add(breakDurField);
        p.add(breakRow, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 2;
        flushOpenBox = new MirthCheckBox("Flush Buffers on Open");
        flushOpenBox.setSelected(true);
        flushOpenBox.setBackground(Color.WHITE);
        p.add(flushOpenBox, g);
        g.gridx = 2; g.gridwidth = 2;
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
        healthBox = new MirthCheckBox("Enable Auto-Reconnect");
        healthBox.setSelected(true);
        healthBox.setBackground(Color.WHITE);
        p.add(healthBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1;
        p.add(new JLabel("Check Interval (ms):"), g);
        g.gridx = 1;
        healthIntervalField = new JTextField("5000", 8);
        p.add(healthIntervalField, g);
        g.gridx = 2;
        p.add(new JLabel("Max Reconnects:"), g);
        g.gridx = 3;
        maxReconnectField = new JTextField("10", 8);
        p.add(maxReconnectField, g);

        g.gridy = 2; g.gridx = 0;
        p.add(new JLabel("Reconnect Delay (ms):"), g);
        g.gridx = 1;
        reconnectDelayField = new JTextField("5000", 8);
        p.add(reconnectDelayField, g);

        return p;
    }

    private JPanel createAnalyzerPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Protocol Analyzer"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 4;
        analyzerBox = new MirthCheckBox("Enable Protocol Logging");
        analyzerBox.setBackground(Color.WHITE);
        p.add(analyzerBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1;
        p.add(new JLabel("Max Log Entries:"), g);
        g.gridx = 1;
        maxLogField = new JTextField("1000", 8);
        p.add(maxLogField, g);

        return p;
    }

    private JPanel createDestPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Destination Options"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 2;
        waitAckBox = new MirthCheckBox("Wait for ACK after Write");
        waitAckBox.setBackground(Color.WHITE);
        p.add(waitAckBox, g);
        g.gridx = 2; g.gridwidth = 2;
        keepOpenBox = new MirthCheckBox("Keep Connection Open (Pool)");
        keepOpenBox.setBackground(Color.WHITE);
        p.add(keepOpenBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1;
        p.add(new JLabel("ACK Timeout (ms):"), g);
        g.gridx = 1;
        ackTimeoutField = new JTextField("1000", 8);
        p.add(ackTimeoutField, g);
        g.gridx = 2;
        p.add(new JLabel("ACK Pattern (hex):"), g);
        g.gridx = 3;
        ackPatternField = new JTextField("06", 8);
        p.add(ackPatternField, g);

        return p;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        }
    }

    private void refreshPortList() {
        portBox.removeAllItems();
        for (int i = 1; i <= 20; i++) {
            portBox.addItem("COM" + i);
        }
        for (int i = 1; i <= 10; i++) {
            portBox.addItem("/dev/ttyUSB" + i);
            portBox.addItem("/dev/ttyACM" + i);
        }
    }

    @Override
    public ConnectorProperties getProperties() {
        if (isSender) {
            SerialDispatcherProperties props = new SerialDispatcherProperties();
            fillPortConfig(props.getPortConfig());
            props.setWaitForAckAfterWrite(waitAckBox.isSelected());
            props.setAckTimeout(parseInt(ackTimeoutField.getText(), 1000));
            props.setAckPattern(parseHex(ackPatternField.getText(), new byte[]{0x06}));
            props.setKeepConnectionOpen(keepOpenBox.isSelected());
            return props;
        } else {
            SerialReceiverProperties props = new SerialReceiverProperties();
            fillPortConfig(props.getPortConfig());
            return props;
        }
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        SerialPortConfig config;
        if (isSender) {
            SerialDispatcherProperties props = (SerialDispatcherProperties) properties;
            config = props.getPortConfig();
            waitAckBox.setSelected(props.isWaitForAckAfterWrite());
            ackTimeoutField.setText(String.valueOf(props.getAckTimeout()));
            ackPatternField.setText(bytesToHex(props.getAckPattern()));
            keepOpenBox.setSelected(props.isKeepConnectionOpen());
        } else {
            SerialReceiverProperties props = (SerialReceiverProperties) properties;
            config = props.getPortConfig();
        }
        portBox.setSelectedItem(config.getPortName());
        baudBox.setSelectedItem(String.valueOf(config.getBaudRate()));
        dataBitsBox.setSelectedItem(String.valueOf(config.getDataBits()));
        stopBitsBox.setSelectedItem(String.valueOf(config.getStopBits()));
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
        healthBox.setSelected(config.isHealthMonitorEnabled());
        healthIntervalField.setText(String.valueOf(config.getHealthInterval()));
        maxReconnectField.setText(String.valueOf(config.getMaxReconnects()));
        reconnectDelayField.setText(String.valueOf(config.getReconnectDelay()));
        analyzerBox.setSelected(config.isProtocolLoggingEnabled());
        maxLogField.setText(String.valueOf(config.getMaxLogEntries()));
    }

    private void fillPortConfig(SerialPortConfig config) {
        config.setPortName(String.valueOf(portBox.getSelectedItem()));
        config.setBaudRate(parseInt(String.valueOf(baudBox.getSelectedItem()), 9600));
        config.setDataBits(parseInt(String.valueOf(dataBitsBox.getSelectedItem()), 8));
        config.setStopBits((int) parseDouble(String.valueOf(stopBitsBox.getSelectedItem()), 1));
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
        config.setHealthMonitorEnabled(healthBox.isSelected());
        config.setHealthInterval(parseInt(healthIntervalField.getText(), 5000));
        config.setMaxReconnects(parseInt(maxReconnectField.getText(), 10));
        config.setReconnectDelay(parseInt(reconnectDelayField.getText(), 5000));
        config.setProtocolLoggingEnabled(analyzerBox.isSelected());
        config.setMaxLogEntries(parseInt(maxLogField.getText(), 1000));
    }

    @Override
    public ConnectorProperties getDefaults() {
        return isSender ? new SerialDispatcherProperties() : new SerialReceiverProperties();
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        SerialPortConfig config;
        if (isSender) {
            config = ((SerialDispatcherProperties) properties).getPortConfig();
        } else {
            config = ((SerialReceiverProperties) properties).getPortConfig();
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
        if (config.getDataBits() < 5 || config.getDataBits() > 8) {
            valid = false;
        }
        if (config.getReadTimeout() < 0 || config.getWriteTimeout() < 0) {
            valid = false;
        }
        if (isSender) {
            SerialDispatcherProperties dp = (SerialDispatcherProperties) properties;
            if (dp.isWaitForAckAfterWrite() && (dp.getAckPattern() == null || dp.getAckPattern().length == 0)) {
                valid = false;
            }
        }
        return valid;
    }

    @Override
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
        maxLogField.setBackground(Color.WHITE);
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