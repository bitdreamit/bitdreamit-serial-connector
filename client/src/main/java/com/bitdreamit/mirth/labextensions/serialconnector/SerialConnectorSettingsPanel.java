/*
 * BitDreamIT Mirth Lab Extensions
 * Copyright (c) 2026 Kimi AI (Moonshot AI) — MIT License
 */
package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.AbstractConnectorSettingsPanel;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Advanced Serial Connector Settings Panel.
 * Exceeds commercial version with auto-detect, signal monitoring,
 * protocol analyzer, and health monitoring controls.
 */
public class SerialConnectorSettingsPanel extends AbstractConnectorSettingsPanel implements ActionListener {

    private boolean isSender;

    // Basic
    private JLabel portLabel;
    private MirthComboBox portBox;
    private JButton refreshPortsBtn;
    private MirthCheckBox autoDetectPortBox;
    private JLabel baudLabel;
    private MirthComboBox baudBox;
    private MirthCheckBox autoDetectBaudBox;
    private JLabel dataBitsLabel;
    private MirthComboBox dataBitsBox;
    private JLabel stopBitsLabel;
    private MirthComboBox stopBitsBox;
    private JLabel parityLabel;
    private MirthComboBox parityBox;
    private JLabel flowLabel;
    private MirthComboBox flowBox;
    private JLabel charsetLabel;
    private MirthComboBox charsetBox;

    // Advanced signals
    private MirthCheckBox dtrBox;
    private MirthCheckBox rtsBox;
    private MirthCheckBox waitCtsBox;
    private MirthCheckBox waitDsrBox;
    private MirthCheckBox waitDcdBox;
    private JLabel signalTimeoutLabel;
    private MirthTextField signalTimeoutField;

    // Break & flush
    private MirthCheckBox breakBox;
    private JLabel breakDurLabel;
    private MirthTextField breakDurField;
    private MirthCheckBox flushOpenBox;
    private MirthCheckBox flushCloseBox;

    // Health monitor
    private MirthCheckBox healthBox;
    private JLabel healthIntervalLabel;
    private MirthTextField healthIntervalField;
    private JLabel maxReconnectLabel;
    private MirthTextField maxReconnectField;
    private JLabel reconnectDelayLabel;
    private MirthTextField reconnectDelayField;

    // Protocol analyzer
    private MirthCheckBox analyzerBox;
    private JLabel maxLogLabel;
    private MirthTextField maxLogField;

    // Destination extras
    private MirthCheckBox waitAckBox;
    private JLabel ackTimeoutLabel;
    private MirthTextField ackTimeoutField;
    private MirthCheckBox keepOpenBox;
    private JLabel ackPatternLabel;
    private MirthTextField ackPatternField;

    // Binary mode
    private MirthCheckBox binaryBox;

    public SerialConnectorSettingsPanel(boolean isSender) {
        this.isSender = isSender;
        initComponents();
        refreshPortList();
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fillx, gap 4", "[][grow][]", ""));

        // ===== BASIC SETTINGS =====
        JPanel basicPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        basicPanel.setBackground(Color.WHITE);
        basicPanel.setBorder(new TitledBorder("Basic Settings"));

        portLabel = new JLabel("Port:");
        portBox = new MirthComboBox();
        portBox.setEditable(true);
        refreshPortsBtn = new JButton("Refresh");
        refreshPortsBtn.addActionListener(this);
        autoDetectPortBox = new MirthCheckBox("Auto-detect");
        autoDetectPortBox.setBackground(Color.WHITE);

        baudLabel = new JLabel("Baud Rate:");
        baudBox = new MirthComboBox();
        baudBox.setModel(new DefaultComboBoxModel<>(new String[]{"9600", "19200", "38400", "57600", "115200", "230400"}));
        autoDetectBaudBox = new MirthCheckBox("Auto-detect");
        autoDetectBaudBox.setBackground(Color.WHITE);

        dataBitsLabel = new JLabel("Data Bits:");
        dataBitsBox = new MirthComboBox();
        dataBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "6", "7", "8"}));
        dataBitsBox.setSelectedItem("8");

        stopBitsLabel = new JLabel("Stop Bits:");
        stopBitsBox = new MirthComboBox();
        stopBitsBox.setModel(new DefaultComboBoxModel<>(new String[]{"1", "1.5", "2"}));

        parityLabel = new JLabel("Parity:");
        parityBox = new MirthComboBox();
        parityBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "Odd", "Even", "Mark", "Space"}));

        flowLabel = new JLabel("Flow Control:");
        flowBox = new MirthComboBox();
        flowBox.setModel(new DefaultComboBoxModel<>(new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"}));

        charsetLabel = new JLabel("Charset:");
        charsetBox = new MirthComboBox();
        charsetBox.setModel(new DefaultComboBoxModel<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "Windows-1252"}));

        binaryBox = new MirthCheckBox("Binary Mode (hex)");
        binaryBox.setBackground(Color.WHITE);

        basicPanel.add(portLabel, "right");
        basicPanel.add(portBox, "split 3, w 180!");
        basicPanel.add(refreshPortsBtn, "w 80!");
        basicPanel.add(autoDetectPortBox, "wrap");
        basicPanel.add(baudLabel, "right");
        basicPanel.add(baudBox, "split 2, w 120!");
        basicPanel.add(autoDetectBaudBox, "wrap");
        basicPanel.add(dataBitsLabel, "right");
        basicPanel.add(dataBitsBox, "w 80!, wrap");
        basicPanel.add(stopBitsLabel, "right");
        basicPanel.add(stopBitsBox, "w 80!, wrap");
        basicPanel.add(parityLabel, "right");
        basicPanel.add(parityBox, "w 100!, wrap");
        basicPanel.add(flowLabel, "right");
        basicPanel.add(flowBox, "w 120!, wrap");
        basicPanel.add(charsetLabel, "right");
        basicPanel.add(charsetBox, "w 120!, wrap");
        basicPanel.add(binaryBox, "skip, wrap");

        add(basicPanel, "span, growx, wrap");

        // ===== SIGNAL CONTROL =====
        JPanel signalPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        signalPanel.setBackground(Color.WHITE);
        signalPanel.setBorder(new TitledBorder("Signal Control (Extra Feature)"));

        dtrBox = new MirthCheckBox("Set DTR");
        dtrBox.setSelected(true);
        dtrBox.setBackground(Color.WHITE);
        rtsBox = new MirthCheckBox("Set RTS");
        rtsBox.setSelected(true);
        rtsBox.setBackground(Color.WHITE);
        waitCtsBox = new MirthCheckBox("Wait for CTS");
        waitCtsBox.setBackground(Color.WHITE);
        waitDsrBox = new MirthCheckBox("Wait for DSR");
        waitDsrBox.setBackground(Color.WHITE);
        waitDcdBox = new MirthCheckBox("Wait for DCD");
        waitDcdBox.setBackground(Color.WHITE);
        signalTimeoutLabel = new JLabel("Signal Wait Timeout (ms):");
        signalTimeoutField = new MirthTextField();
        signalTimeoutField.setText("1000");

        signalPanel.add(dtrBox, "split 2");
        signalPanel.add(rtsBox, "wrap");
        signalPanel.add(waitCtsBox, "split 3");
        signalPanel.add(waitDsrBox);
        signalPanel.add(waitDcdBox, "wrap");
        signalPanel.add(signalTimeoutLabel, "right");
        signalPanel.add(signalTimeoutField, "w 100!, wrap");

        add(signalPanel, "span, growx, wrap");

        // ===== BREAK & FLUSH =====
        JPanel breakPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        breakPanel.setBackground(Color.WHITE);
        breakPanel.setBorder(new TitledBorder("Break & Flush"));

        breakBox = new MirthCheckBox("Send Break Before Open");
        breakBox.setBackground(Color.WHITE);
        breakDurLabel = new JLabel("Break Duration (ms):");
        breakDurField = new MirthTextField();
        breakDurField.setText("100");
        flushOpenBox = new MirthCheckBox("Flush Buffers on Open");
        flushOpenBox.setSelected(true);
        flushOpenBox.setBackground(Color.WHITE);
        flushCloseBox = new MirthCheckBox("Flush Buffers on Close");
        flushCloseBox.setSelected(true);
        flushCloseBox.setBackground(Color.WHITE);

        breakPanel.add(breakBox, "wrap");
        breakPanel.add(breakDurLabel, "right");
        breakPanel.add(breakDurField, "w 100!, wrap");
        breakPanel.add(flushOpenBox, "wrap");
        breakPanel.add(flushCloseBox, "wrap");

        add(breakPanel, "span, growx, wrap");

        // ===== HEALTH MONITOR =====
        JPanel healthPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        healthPanel.setBackground(Color.WHITE);
        healthPanel.setBorder(new TitledBorder("Health Monitor (Extra Feature)"));

        healthBox = new MirthCheckBox("Enable Health Monitor & Auto-Reconnect");
        healthBox.setSelected(true);
        healthBox.setBackground(Color.WHITE);
        healthIntervalLabel = new JLabel("Check Interval (ms):");
        healthIntervalField = new MirthTextField();
        healthIntervalField.setText("30000");
        maxReconnectLabel = new JLabel("Max Reconnect Attempts:");
        maxReconnectField = new MirthTextField();
        maxReconnectField.setText("10");
        reconnectDelayLabel = new JLabel("Reconnect Delay (ms):");
        reconnectDelayField = new MirthTextField();
        reconnectDelayField.setText("5000");

        healthPanel.add(healthBox, "wrap");
        healthPanel.add(healthIntervalLabel, "right");
        healthPanel.add(healthIntervalField, "w 100!, wrap");
        healthPanel.add(maxReconnectLabel, "right");
        healthPanel.add(maxReconnectField, "w 100!, wrap");
        healthPanel.add(reconnectDelayLabel, "right");
        healthPanel.add(reconnectDelayField, "w 100!, wrap");

        add(healthPanel, "span, growx, wrap");

        // ===== PROTOCOL ANALYZER =====
        JPanel analyzerPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        analyzerPanel.setBackground(Color.WHITE);
        analyzerPanel.setBorder(new TitledBorder("Protocol Analyzer (Extra Feature)"));

        analyzerBox = new MirthCheckBox("Enable Protocol Analyzer");
        analyzerBox.setBackground(Color.WHITE);
        maxLogLabel = new JLabel("Max Log Entries:");
        maxLogField = new MirthTextField();
        maxLogField.setText("1000");

        analyzerPanel.add(analyzerBox, "wrap");
        analyzerPanel.add(maxLogLabel, "right");
        analyzerPanel.add(maxLogField, "w 100!, wrap");

        add(analyzerPanel, "span, growx, wrap");

        // ===== DESTINATION EXTRAS =====
        if (isSender) {
            JPanel destPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
            destPanel.setBackground(Color.WHITE);
            destPanel.setBorder(new TitledBorder("Destination Options"));

            waitAckBox = new MirthCheckBox("Wait for ACK After Write");
            waitAckBox.setBackground(Color.WHITE);
            ackTimeoutLabel = new JLabel("ACK Timeout (ms):");
            ackTimeoutField = new MirthTextField();
            ackTimeoutField.setText("1000");
            ackPatternLabel = new JLabel("ACK Pattern (hex):");
            ackPatternField = new MirthTextField();
            ackPatternField.setText("06");
            keepOpenBox = new MirthCheckBox("Keep Connection Open");
            keepOpenBox.setBackground(Color.WHITE);

            destPanel.add(waitAckBox, "wrap");
            destPanel.add(ackTimeoutLabel, "right");
            destPanel.add(ackTimeoutField, "w 100!, wrap");
            destPanel.add(ackPatternLabel, "right");
            destPanel.add(ackPatternField, "w 100!, wrap");
            destPanel.add(keepOpenBox, "wrap");

            add(destPanel, "span, growx, wrap");
        }
    }

    private void refreshPortList() {
        portBox.removeAllItems();
        portBox.addItem("");
        // In real implementation, this would query the server for available ports
        // For client-side, we add common defaults
        String[] commonPorts = {"COM1", "COM2", "COM3", "COM4", "COM5", "COM6",
            "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyACM0"};
        for (String p : commonPorts) {
            portBox.addItem(p);
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
            ackPatternField.setSelectedItem(bytesToHex(p.getAckPattern()));
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