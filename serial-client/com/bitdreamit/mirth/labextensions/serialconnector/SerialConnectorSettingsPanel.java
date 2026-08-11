/*
 * BitDreamIT Mirth Lab Extensions
 */
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

    // Advanced signals
    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JTextField signalTimeoutField;

    // Break & flush
    private MirthCheckBox breakBox;
    private JTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;

    // Health monitor
    private MirthCheckBox healthBox;
    private JTextField healthIntervalField;
    private JTextField maxReconnectField;
    private JTextField reconnectDelayField;

    // Protocol analyzer
    private MirthCheckBox analyzerBox;
    private JTextField maxLogField;

    // Destination extras
    private MirthCheckBox waitAckBox;
    private JTextField ackTimeoutField;
    private MirthCheckBox keepOpenBox;
    private JTextField ackPatternField;

    // Binary mode
    private MirthCheckBox binaryBox;

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

    private JPanel createSignalPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new TitledBorder("Signal Control (Extra Feature)"));
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
        p.setBorder(new TitledBorder("Health Monitor (Extra Feature)"));
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
        p.setBorder(new TitledBorder("Protocol Analyzer (Extra Feature)"));
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
        waitAckBox = new MirthCheckBox("Wait for ACK After Write");
        waitAckBox.setBackground(Color.WHITE);
        p.add(waitAckBox, g);

        g.gridy = 1; g.gridx = 0; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel("ACK Timeout (ms):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        ackTimeoutField = new JTextField("1000", 8);
        p.add(ackTimeoutField, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        p.add(new JLabel("ACK Pattern (hex):"), g);
        g.gridx = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        ackPatternField = new JTextField("06", 8);
        p.add(ackPatternField, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 4; g.fill = GridBagConstraints.NONE;
        keepOpenBox = new MirthCheckBox("Keep Connection Open");
        keepOpenBox.setBackground(Color.WHITE);
        p.add(keepOpenBox, g);

        return p;
    }

    private void refreshPortList() {
        portBox.removeAllItems();
        portBox.addItem("");
        String[] commonPorts = {"COM1", "COM2", "COM3", "COM4", "COM5", "COM6",
                "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyACM0"};
        for (String port : commonPorts) {
            portBox.addItem(port);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        }
    }

    @Override
    public ConnectorProperties getProperties() {
        if (isSender) {
            SerialDispatcherProperties p = new SerialDispatcherProperties();
            fillConfig(p.getPortConfig());
            p.setWaitForAckAfterWrite(waitAckBox.isSelected());
            try { p.setAckTimeout(Integer.parseInt(ackTimeoutField.getText())); } catch (Exception ignored) {}
            try { p.setAckPattern(hexToBytes(ackPatternField.getText())); } catch (Exception ignored) {}
            p.setKeepConnectionOpen(keepOpenBox.isSelected());
            return p;
        } else {
            SerialReceiverProperties p = new SerialReceiverProperties();
            fillConfig(p.getPortConfig());
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
            waitAckBox.setSelected(p.isWaitForAckAfterWrite());
            ackTimeoutField.setText(String.valueOf(p.getAckTimeout()));
            ackPatternField.setText(bytesToHex(p.getAckPattern()));
            keepOpenBox.setSelected(p.isKeepConnectionOpen());
        } else if (!isSender && properties instanceof SerialReceiverProperties) {
            SerialReceiverProperties p = (SerialReceiverProperties) properties;
            loadConfig(p.getPortConfig());
        }
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
        return true;
    }

    @Override
    public void resetInvalidProperties() {}

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}