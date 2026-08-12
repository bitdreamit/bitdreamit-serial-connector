package com.bitdreamit.mirth.labextensions.serialconnector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SerialTransmissionModeDialog extends JDialog implements ActionListener {
    private boolean saved = false;
    private String mode;

    private JTextField startBytesField;
    private JTextField endBytesField;
    private JTextField lineDelimiterField;
    private JCheckBox useMLLPv2Box;
    private JTextField commitAckField;
    private JTextField commitNakField;
    private JTextField maxRetryField;
    private JComboBox<String> byteAbbrevBox;

    private JPanel rowStart;
    private JPanel rowEnd;
    private JPanel rowLine;
    private JPanel rowMLLPv2;
    private JPanel rowAck;
    private JPanel rowNak;
    private JPanel rowRetry;

    public SerialTransmissionModeDialog(Window parent, String mode, SerialPortConfig config) {
        super(parent, "Transmission Mode Settings", ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        setResizable(false);
        initComponents();
        loadConfig(config);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 12, 10, 12));
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Start Bytes
        rowStart = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowStart.setBackground(Color.WHITE);
        rowStart.add(new JLabel("Start of Message Bytes:"));
        startBytesField = new JTextField(10);
        startBytesField.setPreferredSize(new Dimension(90, 22));
        rowStart.add(startBytesField);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        form.add(rowStart, g);

        // Row 1: End Bytes
        rowEnd = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowEnd.setBackground(Color.WHITE);
        rowEnd.add(new JLabel("End of Message Bytes:"));
        endBytesField = new JTextField(10);
        endBytesField.setPreferredSize(new Dimension(90, 22));
        rowEnd.add(endBytesField);
        g.gridy = 1;
        form.add(rowEnd, g);

        // Row 2: Line Delimiter
        rowLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowLine.setBackground(Color.WHITE);
        rowLine.add(new JLabel("Line Delimiter:"));
        lineDelimiterField = new JTextField(12);
        lineDelimiterField.setPreferredSize(new Dimension(110, 22));
        rowLine.add(lineDelimiterField);
        JLabel lineHint = new JLabel("Use \\r\\n, \\n, \\r, or custom");
        lineHint.setFont(lineHint.getFont().deriveFont(10f));
        lineHint.setForeground(Color.GRAY);
        rowLine.add(lineHint);
        g.gridy = 2;
        form.add(rowLine, g);

        // Row 3: MLLPv2
        rowMLLPv2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowMLLPv2.setBackground(Color.WHITE);
        useMLLPv2Box = new JCheckBox("Use MLLPv2");
        useMLLPv2Box.setBackground(Color.WHITE);
        rowMLLPv2.add(useMLLPv2Box);
        g.gridy = 3;
        form.add(rowMLLPv2, g);

        // Row 4: Commit ACK
        rowAck = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowAck.setBackground(Color.WHITE);
        rowAck.add(new JLabel("Commit ACK Bytes:"));
        commitAckField = new JTextField(10);
        commitAckField.setPreferredSize(new Dimension(90, 22));
        rowAck.add(commitAckField);
        g.gridy = 4;
        form.add(rowAck, g);

        // Row 5: Commit NAK
        rowNak = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowNak.setBackground(Color.WHITE);
        rowNak.add(new JLabel("Commit NAK Bytes:"));
        commitNakField = new JTextField(10);
        commitNakField.setPreferredSize(new Dimension(90, 22));
        rowNak.add(commitNakField);
        g.gridy = 5;
        form.add(rowNak, g);

        // Row 6: Max Retry
        rowRetry = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowRetry.setBackground(Color.WHITE);
        rowRetry.add(new JLabel("Max Retry Count:"));
        maxRetryField = new JTextField(10);
        maxRetryField.setPreferredSize(new Dimension(90, 22));
        rowRetry.add(maxRetryField);
        g.gridy = 6;
        form.add(rowRetry, g);

        add(form, BorderLayout.CENTER);

        // South: Byte Abbreviations + Buttons
        JPanel south = new JPanel(new BorderLayout(4, 8));
        south.setBackground(Color.WHITE);

        JPanel abbrevPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        abbrevPanel.setBackground(Color.WHITE);
        abbrevPanel.add(new JLabel("Byte Abbreviations:"));
        byteAbbrevBox = new JComboBox<>(new String[]{
                "-- Insert --", "<NUL>", "<SOH>", "<STX>", "<ETX>", "<EOT>", "<ENQ>", "<ACK>",
                "<BEL>", "<BS>", "<TAB>", "<LF>", "<VT>", "<FF>", "<CR>", "<SO>", "<SI>",
                "<DLE>", "<DC1>", "<DC2>", "<DC3>", "<DC4>", "<NAK>", "<SYN>", "<ETB>",
                "<CAN>", "<EM>", "<SUB>", "<ESC>", "<FS>", "<GS>", "<RS>", "<US>", "<SP>", "<DEL>"
        });
        byteAbbrevBox.setPreferredSize(new Dimension(130, 22));
        byteAbbrevBox.addActionListener(this);
        abbrevPanel.add(byteAbbrevBox);
        south.add(abbrevPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> { saved = true; dispose(); });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        south.add(btnPanel, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);
    }

    private void loadConfig(SerialPortConfig config) {
        // Show/hide rows based on mode
        rowStart.setVisible(!"RAW".equals(mode) && !"LINE".equals(mode));
        rowEnd.setVisible(!"RAW".equals(mode) && !"LINE".equals(mode));
        rowLine.setVisible("LINE".equals(mode));
        rowMLLPv2.setVisible("MLLP".equals(mode));
        rowAck.setVisible("MLLP".equals(mode) || "ASTM".equals(mode));
        rowNak.setVisible("MLLP".equals(mode) || "ASTM".equals(mode));
        rowRetry.setVisible("MLLP".equals(mode));

        // Defaults per mode
        if ("MLLP".equals(mode)) {
            startBytesField.setText(config.getStartOfMessageBytes().isEmpty() ? "0B" : config.getStartOfMessageBytes());
            endBytesField.setText(config.getEndOfMessageBytes().isEmpty() ? "1C0D" : config.getEndOfMessageBytes());
        } else if ("ASTM".equals(mode)) {
            startBytesField.setText(config.getStartOfMessageBytes().isEmpty() ? "02" : config.getStartOfMessageBytes());
            endBytesField.setText(config.getEndOfMessageBytes().isEmpty() ? "03" : config.getEndOfMessageBytes());
        } else {
            startBytesField.setText(config.getStartOfMessageBytes());
            endBytesField.setText(config.getEndOfMessageBytes());
        }

        lineDelimiterField.setText(config.getLineDelimiter());
        useMLLPv2Box.setSelected(config.isUseMLLPv2());
        commitAckField.setText(config.getCommitAckBytes());
        commitNakField.setText(config.getCommitNakBytes());
        maxRetryField.setText(String.valueOf(config.getMaxRetryCount()));
    }

    public void saveToConfig(SerialPortConfig config) {
        if (!saved) return;
        config.setStartOfMessageBytes(startBytesField.getText().trim());
        config.setEndOfMessageBytes(endBytesField.getText().trim());
        config.setLineDelimiter(lineDelimiterField.getText());
        config.setUseMLLPv2(useMLLPv2Box.isSelected());
        config.setCommitAckBytes(commitAckField.getText().trim());
        config.setCommitNakBytes(commitNakField.getText().trim());
        config.setMaxRetryCount(parseInt(maxRetryField.getText(), 2));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == byteAbbrevBox) {
            String selected = (String) byteAbbrevBox.getSelectedItem();
            if (selected == null || selected.startsWith("--")) return;
            String hex = abbrevToHex(selected);
            if (hex == null) return;

            Component focusOwner = getFocusOwner();
            if (focusOwner instanceof JTextField) {
                JTextField tf = (JTextField) focusOwner;
                int pos = tf.getCaretPosition();
                try {
                    tf.getDocument().insertString(pos, hex, null);
                } catch (Exception ex) {}
            }
            byteAbbrevBox.setSelectedIndex(0);
        }
    }

    private String abbrevToHex(String abbrev) {
        switch (abbrev) {
            case "<NUL>": return "00"; case "<SOH>": return "01"; case "<STX>": return "02";
            case "<ETX>": return "03"; case "<EOT>": return "04"; case "<ENQ>": return "05";
            case "<ACK>": return "06"; case "<BEL>": return "07"; case "<BS>": return "08";
            case "<TAB>": return "09"; case "<LF>": return "0A"; case "<VT>": return "0B";
            case "<FF>": return "0C"; case "<CR>": return "0D"; case "<SO>": return "0E";
            case "<SI>": return "0F"; case "<DLE>": return "10"; case "<DC1>": return "11";
            case "<DC2>": return "12"; case "<DC3>": return "13"; case "<DC4>": return "14";
            case "<NAK>": return "15"; case "<SYN>": return "16"; case "<ETB>": return "17";
            case "<CAN>": return "18"; case "<EM>": return "19"; case "<SUB>": return "1A";
            case "<ESC>": return "1B"; case "<FS>": return "1C"; case "<GS>": return "1D";
            case "<RS>": return "1E"; case "<US>": return "1F"; case "<SP>": return "20";
            case "<DEL>": return "7F";
            default: return null;
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public boolean isSaved() {
        return saved;
    }
}