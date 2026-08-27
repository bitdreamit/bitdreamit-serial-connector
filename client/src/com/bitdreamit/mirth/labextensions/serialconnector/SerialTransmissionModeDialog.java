package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Transmission Mode Settings Dialog — matches Mirth's TCP Transmission Mode UI style.
 *
 * Strict two-column grid: Label column (fixed width, right-aligned) + Input column (left-aligned).
 * All input fields start at the SAME X position. Section headers have separator lines.
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 */
public class SerialTransmissionModeDialog extends JDialog implements ActionListener {
    private boolean saved = false;
    private String mode;

    private MirthTextField startBytesField;
    private MirthTextField endBytesField;
    private MirthTextField lineDelimiterField;
    private MirthCheckBox useMLLPv2Box;
    private MirthTextField commitAckField;
    private MirthTextField commitNakField;
    private MirthTextField maxRetryField;
    private MirthComboBox<String> byteAbbrevBox;

    private JPanel framingSection;
    private JPanel lineModeSection;
    private JPanel mllpSection;
    private JPanel ackNakSection;
    private JPanel retrySection;
    private JLabel framingHeader;
    private JLabel lineHeader;
    private JLabel mllpHeader;
    private JLabel ackNakHeader;
    private JLabel retryHeader;
    private JLabel descriptionLabel;
    private JLabel sampleFrameLabel;

    private static final int LABEL_WIDTH = 160;
    private static final int FIELD_GAP = 6;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 8;
    private static final Color LABEL_COLOR = new Color(51, 51, 51);
    private static final Color SEP_COLOR = new Color(200, 200, 200);
    private static final Color DESC_BG = new Color(245, 248, 252);

    public SerialTransmissionModeDialog(Window parent, String mode, SerialPortConfig config) {
        super(parent, "Transmission Mode Settings", ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        initComponents();
        loadConfig(config);
        updateModeDisplay();
        pack();
        setLocationRelativeTo(parent);
    }

    private JLabel mkLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(LABEL_COLOR);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 22));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, 22));
        return label;
    }

    private GridBagConstraints labelGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = row;
        g.anchor = GridBagConstraints.NORTHEAST;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 0, ROW_GAP, FIELD_GAP);
        return g;
    }

    private GridBagConstraints fieldGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1; g.gridy = row;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, ROW_GAP, 0);
        return g;
    }

    private void addRow(JPanel parent, GridBagLayout layout, String labelText, JComponent field, int row) {
        JLabel label = mkLabel(labelText);
        GridBagConstraints lg = labelGbc(row);
        GridBagConstraints fg = fieldGbc(row);
        layout.setConstraints(label, lg);
        parent.add(label);

        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setOpaque(true);
        fieldPanel.add(field);
        layout.setConstraints(fieldPanel, fg);
        parent.add(fieldPanel);
    }

    private JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(LABEL_COLOR);
        header.setBackground(Color.WHITE);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(SECTION_GAP, 0, 2, 0));
        return header;
    }

    private void initComponents() {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout(8, 10));

        // ===== Top: Description panel =====
        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.setBackground(DESC_BG);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEP_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel modeTitleLabel = new JLabel("Mode: " + (mode != null ? mode : "RAW"));
        modeTitleLabel.setFont(modeTitleLabel.getFont().deriveFont(Font.BOLD, 13f));
        modeTitleLabel.setForeground(new Color(10, 104, 245));
        topPanel.add(modeTitleLabel, BorderLayout.NORTH);

        descriptionLabel = new JLabel(" ");
        descriptionLabel.setForeground(LABEL_COLOR);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        topPanel.add(descriptionLabel, BorderLayout.CENTER);

        JPanel samplePanel = new JPanel(new BorderLayout(4, 2));
        samplePanel.setBackground(new Color(250, 250, 245));
        samplePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 180), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        JLabel sampleTitle = new JLabel("Sample Frame:");
        sampleTitle.setFont(sampleTitle.getFont().deriveFont(Font.BOLD, 10f));
        sampleTitle.setForeground(LABEL_COLOR);
        samplePanel.add(sampleTitle, BorderLayout.NORTH);
        sampleFrameLabel = new JLabel(" ");
        sampleFrameLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        sampleFrameLabel.setForeground(new Color(80, 80, 80));
        samplePanel.add(sampleFrameLabel, BorderLayout.CENTER);
        topPanel.add(samplePanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // ===== Center: Settings (strict two-column grid) =====
        JPanel centerPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        centerPanel.setLayout(layout);
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setOpaque(true);

        int row = 0;

        // --- Framing section ---
        GridBagConstraints hg = new GridBagConstraints();
        hg.gridwidth = 2; hg.gridx = 0; hg.gridy = row++;
        hg.fill = GridBagConstraints.HORIZONTAL; hg.weightx = 1.0;
        framingHeader = createSectionHeader("Message Framing");
        layout.setConstraints(framingHeader, hg);
        centerPanel.add(framingHeader);
        // Separator
        JSeparator sep1 = new JSeparator(SwingConstants.HORIZONTAL);
        sep1.setForeground(SEP_COLOR);
        GridBagConstraints sep1g = new GridBagConstraints();
        sep1g.gridwidth = 2; sep1g.gridx = 0; sep1g.gridy = row++;
        sep1g.fill = GridBagConstraints.HORIZONTAL; sep1g.weightx = 1.0;
        layout.setConstraints(sep1, sep1g);
        centerPanel.add(sep1);

        startBytesField = new MirthTextField();

        startBytesField.setPreferredSize(new Dimension(112, 22));
        startBytesField.setPreferredSize(new Dimension(140, 22));
        addRow(centerPanel, layout, "Start of Message (hex):", startBytesField, row++);

        endBytesField = new MirthTextField();

        endBytesField.setPreferredSize(new Dimension(112, 22));
        endBytesField.setPreferredSize(new Dimension(140, 22));
        addRow(centerPanel, layout, "End of Message (hex):", endBytesField, row++);

        // --- Line delimiter section ---
        hg = new GridBagConstraints();
        hg.gridwidth = 2; hg.gridx = 0; hg.gridy = row++;
        hg.fill = GridBagConstraints.HORIZONTAL; hg.weightx = 1.0;
        lineHeader = createSectionHeader("Line Delimiter");
        layout.setConstraints(lineHeader, hg);
        centerPanel.add(lineHeader);
        JSeparator sep2 = new JSeparator(SwingConstants.HORIZONTAL);
        sep2.setForeground(SEP_COLOR);
        GridBagConstraints sep2g = new GridBagConstraints();
        sep2g.gridwidth = 2; sep2g.gridx = 0; sep2g.gridy = row++;
        sep2g.fill = GridBagConstraints.HORIZONTAL; sep2g.weightx = 1.0;
        layout.setConstraints(sep2, sep2g);
        centerPanel.add(sep2);

        lineDelimiterField = new MirthTextField();

        lineDelimiterField.setPreferredSize(new Dimension(112, 22));
        lineDelimiterField.setPreferredSize(new Dimension(140, 22));
        addRow(centerPanel, layout, "Delimiter:", lineDelimiterField, row++);

        // --- MLLP section ---
        hg = new GridBagConstraints();
        hg.gridwidth = 2; hg.gridx = 0; hg.gridy = row++;
        hg.fill = GridBagConstraints.HORIZONTAL; hg.weightx = 1.0;
        mllpHeader = createSectionHeader("MLLP Options");
        layout.setConstraints(mllpHeader, hg);
        centerPanel.add(mllpHeader);
        JSeparator sep3 = new JSeparator(SwingConstants.HORIZONTAL);
        sep3.setForeground(SEP_COLOR);
        GridBagConstraints sep3g = new GridBagConstraints();
        sep3g.gridwidth = 2; sep3g.gridx = 0; sep3g.gridy = row++;
        sep3g.fill = GridBagConstraints.HORIZONTAL; sep3g.weightx = 1.0;
        layout.setConstraints(sep3, sep3g);
        centerPanel.add(sep3);

        useMLLPv2Box = new MirthCheckBox("Use MLLPv2 (send commit ACK)");
        useMLLPv2Box.setBackground(Color.WHITE);
        addRow(centerPanel, layout, "MLLP Version:", useMLLPv2Box, row++);

        // --- ACK/NAK section ---
        hg = new GridBagConstraints();
        hg.gridwidth = 2; hg.gridx = 0; hg.gridy = row++;
        hg.fill = GridBagConstraints.HORIZONTAL; hg.weightx = 1.0;
        ackNakHeader = createSectionHeader("ACK / NAK Bytes");
        layout.setConstraints(ackNakHeader, hg);
        centerPanel.add(ackNakHeader);
        JSeparator sep4 = new JSeparator(SwingConstants.HORIZONTAL);
        sep4.setForeground(SEP_COLOR);
        GridBagConstraints sep4g = new GridBagConstraints();
        sep4g.gridwidth = 2; sep4g.gridx = 0; sep4g.gridy = row++;
        sep4g.fill = GridBagConstraints.HORIZONTAL; sep4g.weightx = 1.0;
        layout.setConstraints(sep4, sep4g);
        centerPanel.add(sep4);

        commitAckField = new MirthTextField();

        commitAckField.setPreferredSize(new Dimension(80, 22));
        commitAckField.setPreferredSize(new Dimension(100, 22));
        addRow(centerPanel, layout, "Commit ACK (hex):", commitAckField, row++);

        commitNakField = new MirthTextField();

        commitNakField.setPreferredSize(new Dimension(80, 22));
        commitNakField.setPreferredSize(new Dimension(100, 22));
        addRow(centerPanel, layout, "Commit NAK (hex):", commitNakField, row++);

        // --- Retry section ---
        hg = new GridBagConstraints();
        hg.gridwidth = 2; hg.gridx = 0; hg.gridy = row++;
        hg.fill = GridBagConstraints.HORIZONTAL; hg.weightx = 1.0;
        retryHeader = createSectionHeader("Retry Options");
        layout.setConstraints(retryHeader, hg);
        centerPanel.add(retryHeader);
        JSeparator sep5 = new JSeparator(SwingConstants.HORIZONTAL);
        sep5.setForeground(SEP_COLOR);
        GridBagConstraints sep5g = new GridBagConstraints();
        sep5g.gridwidth = 2; sep5g.gridx = 0; sep5g.gridy = row++;
        sep5g.fill = GridBagConstraints.HORIZONTAL; sep5g.weightx = 1.0;
        layout.setConstraints(sep5, sep5g);
        centerPanel.add(sep5);

        maxRetryField = new MirthTextField();

        maxRetryField.setPreferredSize(new Dimension(64, 22));
        maxRetryField.setPreferredSize(new Dimension(60, 22));
        addRow(centerPanel, layout, "Max Retry Count:", maxRetryField, row++);

        add(centerPanel, BorderLayout.CENTER);

        // ===== Bottom: Byte abbreviations + buttons =====
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(Color.WHITE);

        JPanel abbrevPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        abbrevPanel.setBackground(Color.WHITE);
        JLabel abbrevLabel = new JLabel("Insert Byte:");
        abbrevLabel.setForeground(LABEL_COLOR);
        abbrevPanel.add(abbrevLabel);
        byteAbbrevBox = new MirthComboBox<>();
        byteAbbrevBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "-- Select --", "<NUL>", "<SOH>", "<STX>", "<ETX>", "<EOT>", "<ENQ>", "<ACK>",
                "<BEL>", "<BS>", "<TAB>", "<LF>", "<VT>", "<FF>", "<CR>", "<SO>", "<SI>",
                "<DLE>", "<DC1>", "<DC2>", "<DC3>", "<DC4>", "<NAK>", "<SYN>", "<ETB>",
                "<CAN>", "<EM>", "<SUB>", "<ESC>", "<FS>", "<GS>", "<RS>", "<US>", "<SP>", "<DEL>"
        }));
        byteAbbrevBox.setPreferredSize(new Dimension(120, 22));
        byteAbbrevBox.addActionListener(this);
        abbrevPanel.add(byteAbbrevBox);
        bottomPanel.add(abbrevPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setBackground(Color.WHITE);
        JButton okBtn = new JButton("OK");
        okBtn.setPreferredSize(new Dimension(75, 25));
        okBtn.addActionListener(e -> { saved = true; dispose(); });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(75, 25));
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateModeDisplay() {
        boolean showFraming = !"RAW".equals(mode) && !"LINE".equals(mode);
        boolean showLine = "LINE".equals(mode);
        boolean showMllp = "MLLP".equals(mode);
        boolean showAckNak = "MLLP".equals(mode) || "ASTM".equals(mode);
        boolean showRetry = "MLLP".equals(mode);

        framingHeader.setVisible(showFraming);
        framingSection = null; // sections are inline now, we toggle header visibility
        lineHeader.setVisible(showLine);
        mllpHeader.setVisible(showMllp);
        ackNakHeader.setVisible(showAckNak);
        retryHeader.setVisible(showRetry);

        String desc = "";
        String sample = "";
        switch (mode) {
            case "RAW":
                desc = "Raw mode: bytes are passed through without any framing.";
                sample = "<raw bytes>";
                break;
            case "LINE":
                desc = "Line mode: messages are delimited by a character sequence.";
                sample = "<message>\\r\\n<message>\\r\\n...";
                break;
            case "FRAME":
                desc = "Frame mode: messages are wrapped between Start and End byte sequences.";
                sample = "<start><message><end>";
                break;
            case "MLLP":
                desc = "MLLP mode: Minimal Lower Layer Protocol for HL7.";
                sample = "<VT><message><FS><CR>";
                break;
            case "ASTM":
                desc = "ASTM E1381 mode: for medical devices.";
                sample = "<STX><message><ETX><checksum><CR><LF>";
                break;
        }
        descriptionLabel.setText("<html><p style='width:400px'>" + desc + "</p></html>");
        sampleFrameLabel.setText(sample);
    }

    private void loadConfig(SerialPortConfig config) {
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
            String abbrev = selected.substring(1, selected.length() - 1);
            String hex = abbrevToHex(abbrev);
            if (hex == null) return;

            Component focusOwner = getFocusOwner();
            if (focusOwner instanceof MirthTextField) {
                MirthTextField tf = (MirthTextField) focusOwner;
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
            case "NUL": return "00"; case "SOH": return "01"; case "STX": return "02";
            case "ETX": return "03"; case "EOT": return "04"; case "ENQ": return "05";
            case "ACK": return "06"; case "BEL": return "07"; case "BS": return "08";
            case "TAB": return "09"; case "LF": return "0A"; case "VT": return "0B";
            case "FF": return "0C"; case "CR": return "0D"; case "SO": return "0E";
            case "SI": return "0F"; case "DLE": return "10"; case "DC1": return "11";
            case "DC2": return "12"; case "DC3": return "13"; case "DC4": return "14";
            case "NAK": return "15"; case "SYN": return "16"; case "ETB": return "17";
            case "CAN": return "18"; case "EM": return "19"; case "SUB": return "1A";
            case "ESC": return "1B"; case "FS": return "1C"; case "GS": return "1D";
            case "RS": return "1E"; case "US": return "1F"; case "SP": return "20";
            case "DEL": return "7F";
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
