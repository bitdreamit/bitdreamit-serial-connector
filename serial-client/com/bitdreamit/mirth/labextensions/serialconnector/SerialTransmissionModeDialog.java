package com.bitdreamit.mirth.labextensions.serialconnector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Transmission Mode Settings Dialog — redesigned to match Mirth's TCP Transmission Mode UI style.
 *
 * Features:
 *  - Titled bordered sections (like Mirth's TCP Transmission Mode dialog)
 *  - Mode description panel at top showing what the selected mode does
 *  - Compact GridBagLayout with consistent insets
 *  - Sample frame preview showing how bytes will be framed
 *  - White background, Mirth-style layout
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 */
public class SerialTransmissionModeDialog extends JDialog implements ActionListener {
    private boolean saved = false;
    private String mode;

    // Fields
    private JTextField startBytesField;
    private JTextField endBytesField;
    private JTextField lineDelimiterField;
    private JCheckBox useMLLPv2Box;
    private JTextField commitAckField;
    private JTextField commitNakField;
    private JTextField maxRetryField;
    private JComboBox<String> byteAbbrevBox;

    // Section panels (for show/hide based on mode)
    private JPanel framingSection;
    private JPanel lineModeSection;
    private JPanel mllpSection;
    private JPanel ackNakSection;
    private JPanel retrySection;

    // Description label
    private JLabel descriptionLabel;
    private JLabel sampleFrameLabel;

    // Colors matching Mirth's theme
    private static final Color BORDER_COLOR = new Color(180, 180, 180);
    private static final Color LABEL_COLOR = new Color(60, 60, 60);
    private static final Color DESCRIPTION_BG = new Color(245, 248, 252);
    private static final Color SAMPLE_BG = new Color(250, 250, 245);

    public SerialTransmissionModeDialog(Window parent, String mode, SerialPortConfig config) {
        super(parent, "Transmission Mode Settings", ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        setResizable(false);
        initComponents();
        loadConfig(config);
        updateModeDisplay();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout(8, 10));

        // ===== Top: Description panel =====
        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.setBackground(DESCRIPTION_BG);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
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

        // Sample frame preview
        JPanel samplePanel = new JPanel(new BorderLayout(4, 2));
        samplePanel.setBackground(SAMPLE_BG);
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

        // ===== Center: Settings sections =====
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.weighty = 0.0;

        int row = 0;

        // Framing section (Start/End bytes)
        g.gridy = row++; g.insets = new Insets(6, 0, 2, 0);
        centerPanel.add(createSectionHeader("Message Framing"), g);
        framingSection = createFramingSection();
        g.gridy = row++; g.insets = new Insets(0, 0, 6, 0);
        centerPanel.add(framingSection, g);

        // Line mode section (delimiter)
        g.gridy = row++; g.insets = new Insets(6, 0, 2, 0);
        centerPanel.add(createSectionHeader("Line Delimiter"), g);
        lineModeSection = createLineModeSection();
        g.gridy = row++; g.insets = new Insets(0, 0, 6, 0);
        centerPanel.add(lineModeSection, g);

        // MLLP section
        g.gridy = row++; g.insets = new Insets(6, 0, 2, 0);
        centerPanel.add(createSectionHeader("MLLP Options"), g);
        mllpSection = createMllpSection();
        g.gridy = row++; g.insets = new Insets(0, 0, 6, 0);
        centerPanel.add(mllpSection, g);

        // ACK/NAK section
        g.gridy = row++; g.insets = new Insets(6, 0, 2, 0);
        centerPanel.add(createSectionHeader("ACK / NAK Bytes"), g);
        ackNakSection = createAckNakSection();
        g.gridy = row++; g.insets = new Insets(0, 0, 6, 0);
        centerPanel.add(ackNakSection, g);

        // Retry section
        g.gridy = row++; g.insets = new Insets(6, 0, 2, 0);
        centerPanel.add(createSectionHeader("Retry Options"), g);
        retrySection = createRetrySection();
        g.gridy = row++; g.insets = new Insets(0, 0, 6, 0);
        centerPanel.add(retrySection, g);

        add(centerPanel, BorderLayout.CENTER);

        // ===== Bottom: Byte abbreviations + buttons =====
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(Color.WHITE);

        // Byte abbreviations
        JPanel abbrevPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        abbrevPanel.setBackground(Color.WHITE);
        JLabel abbrevLabel = new JLabel("Insert Byte:");
        abbrevLabel.setForeground(LABEL_COLOR);
        abbrevPanel.add(abbrevLabel);
        byteAbbrevBox = new JComboBox<>(new String[]{
                "-- Select --", "<NUL>", "<SOH>", "<STX>", "<ETX>", "<EOT>", "<ENQ>", "<ACK>",
                "<BEL>", "<BS>", "<TAB>", "<LF>", "<VT>", "<FF>", "<CR>", "<SO>", "<SI>",
                "<DLE>", "<DC1>", "<DC2>", "<DC3>", "<DC4>", "<NAK>", "<SYN>", "<ETB>",
                "<CAN>", "<EM>", "<SUB>", "<ESC>", "<FS>", "<GS>", "<RS>", "<US>", "<SP>", "<DEL>"
        });
        byteAbbrevBox.setPreferredSize(new Dimension(120, 22));
        byteAbbrevBox.addActionListener(this);
        abbrevPanel.add(byteAbbrevBox);
        bottomPanel.add(abbrevPanel, BorderLayout.NORTH);

        // Button panel
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

    private JPanel createSection(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        // EmptyBorder with small padding — matches Mirth's TCP section style
        panel.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
        return panel;
    }

    private JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(LABEL_COLOR);
        header.setBackground(Color.WHITE);
        header.setOpaque(true);
        return header;
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

    // ===== Framing Section =====

    private JPanel createFramingSection() {
        JPanel p = createSection("Message Framing");

        GridBagConstraints g = mkGbc();

        // Row 0: Start Bytes
        g.gridy = 0;
        g.gridx = 0; p.add(mkLabel("Start of Message Bytes (hex):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        startBytesField = new JTextField(12);
        startBytesField.setPreferredSize(new Dimension(120, 22));
        p.add(startBytesField, g);
        JLabel startHint = new JLabel("e.g. 0B for <VT>");
        startHint.setFont(startHint.getFont().deriveFont(10f));
        startHint.setForeground(Color.GRAY);
        g.gridx = 2; p.add(startHint, g);

        // Row 1: End Bytes
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("End of Message Bytes (hex):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        endBytesField = new JTextField(12);
        endBytesField.setPreferredSize(new Dimension(120, 22));
        p.add(endBytesField, g);
        JLabel endHint = new JLabel("e.g. 1C0D for <FS><CR>");
        endHint.setFont(endHint.getFont().deriveFont(10f));
        endHint.setForeground(Color.GRAY);
        g.gridx = 2; p.add(endHint, g);

        return p;
    }

    // ===== Line Mode Section =====

    private JPanel createLineModeSection() {
        JPanel p = createSection("Line Delimiter");

        GridBagConstraints g = mkGbc();

        // Row 0: Delimiter
        g.gridy = 0;
        g.gridx = 0; p.add(mkLabel("Line Delimiter:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        lineDelimiterField = new JTextField(12);
        lineDelimiterField.setPreferredSize(new Dimension(120, 22));
        p.add(lineDelimiterField, g);
        JLabel hint = new JLabel("Use \\r\\n, \\n, \\r, or custom");
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setForeground(Color.GRAY);
        g.gridx = 2; p.add(hint, g);

        return p;
    }

    // ===== MLLP Section =====

    private JPanel createMllpSection() {
        JPanel p = createSection("MLLP Options");

        GridBagConstraints g = mkGbc();

        // Row 0: MLLPv2
        g.gridy = 0;
        g.gridx = 0; g.gridwidth = 3; g.anchor = GridBagConstraints.WEST;
        useMLLPv2Box = new JCheckBox("Use MLLPv2 (send commit ACK)");
        p.add(useMLLPv2Box, g);
        g.gridwidth = 1;

        return p;
    }

    // ===== ACK/NAK Section =====

    private JPanel createAckNakSection() {
        JPanel p = createSection("ACK / NAK Bytes");

        GridBagConstraints g = mkGbc();

        // Row 0: ACK
        g.gridy = 0;
        g.gridx = 0; p.add(mkLabel("Commit ACK Bytes (hex):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        commitAckField = new JTextField(12);
        commitAckField.setPreferredSize(new Dimension(100, 22));
        p.add(commitAckField, g);
        JLabel ackHint = new JLabel("default: 06 (<ACK>)");
        ackHint.setFont(ackHint.getFont().deriveFont(10f));
        ackHint.setForeground(Color.GRAY);
        g.gridx = 2; p.add(ackHint, g);

        // Row 1: NAK
        g.gridy = 1;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(mkLabel("Commit NAK Bytes (hex):"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        commitNakField = new JTextField(12);
        commitNakField.setPreferredSize(new Dimension(100, 22));
        p.add(commitNakField, g);
        JLabel nakHint = new JLabel("default: 15 (<NAK>)");
        nakHint.setFont(nakHint.getFont().deriveFont(10f));
        nakHint.setForeground(Color.GRAY);
        g.gridx = 2; p.add(nakHint, g);

        return p;
    }

    // ===== Retry Section =====

    private JPanel createRetrySection() {
        JPanel p = createSection("Retry Options");

        GridBagConstraints g = mkGbc();

        // Row 0: Max Retry
        g.gridy = 0;
        g.gridx = 0; p.add(mkLabel("Max Retry Count:"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST;
        maxRetryField = new JTextField(8);
        maxRetryField.setPreferredSize(new Dimension(80, 22));
        p.add(maxRetryField, g);

        return p;
    }

    // ===== Mode display update =====

    private void updateModeDisplay() {
        // Show/hide sections based on mode
        boolean showFraming = !"RAW".equals(mode) && !"LINE".equals(mode);
        boolean showLine = "LINE".equals(mode);
        boolean showMllp = "MLLP".equals(mode);
        boolean showAckNak = "MLLP".equals(mode) || "ASTM".equals(mode);
        boolean showRetry = "MLLP".equals(mode);

        framingSection.setVisible(showFraming);
        lineModeSection.setVisible(showLine);
        mllpSection.setVisible(showMllp);
        ackNakSection.setVisible(showAckNak);
        retrySection.setVisible(showRetry);

        // Update description
        String desc = "";
        String sample = "";
        switch (mode) {
            case "RAW":
                desc = "Raw mode: bytes are passed through without any framing. Every read is dispatched as-is.";
                sample = "<raw bytes>";
                break;
            case "LINE":
                desc = "Line mode: messages are delimited by a character sequence (e.g. \\r\\n). Useful for text protocols.";
                sample = "<message>" + escape(config().getLineDelimiter()) + "<message>" + escape(config().getLineDelimiter()) + "...";
                break;
            case "FRAME":
                desc = "Frame mode: messages are wrapped between Start and End byte sequences. Useful for custom binary protocols.";
                sample = hexToDisplay(config().getStartOfMessageBytes(), "<none>") +
                         "<message>" +
                         hexToDisplay(config().getEndOfMessageBytes(), "<none>");
                break;
            case "MLLP":
                desc = "MLLP mode: Minimal Lower Layer Protocol for HL7. Uses <VT>...<FS><CR> framing with optional ACK.";
                sample = "<VT><message><FS><CR>";
                break;
            case "ASTM":
                desc = "ASTM E1381 mode: American Society for Testing and Materials protocol for medical devices.";
                sample = "<STX><message><ETX><checksum><CR><LF>";
                break;
            default:
                desc = "Unknown mode.";
        }
        descriptionLabel.setText("<html><p style='width:400px'>" + desc + "</p></html>");
        sampleFrameLabel.setText(sample);
    }

    private SerialPortConfig config() {
        // Helper to get a temp config for display
        SerialPortConfig c = new SerialPortConfig();
        loadConfig(c);
        return c;
    }

    private String escape(String delim) {
        if (delim == null || delim.isEmpty()) return "\\r\\n";
        return delim;
    }

    private String hexToDisplay(String hex, String fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        // Convert hex to display format
        StringBuilder sb = new StringBuilder();
        String clean = hex.replaceAll("\\s", "").toUpperCase();
        if (clean.length() % 2 != 0) clean = "0" + clean;
        for (int i = 0; i < clean.length(); i += 2) {
            String byteHex = clean.substring(i, i + 2);
            String abbrev = hexToAbbrev(byteHex);
            if (abbrev != null) {
                sb.append("<").append(abbrev).append(">");
            } else {
                sb.append("0x").append(byteHex);
            }
        }
        return sb.toString();
    }

    private String hexToAbbrev(String hex) {
        switch (hex.toUpperCase()) {
            case "00": return "NUL"; case "01": return "SOH"; case "02": return "STX";
            case "03": return "ETX"; case "04": return "EOT"; case "05": return "ENQ";
            case "06": return "ACK"; case "07": return "BEL"; case "08": return "BS";
            case "09": return "TAB"; case "0A": return "LF"; case "0B": return "VT";
            case "0C": return "FF"; case "0D": return "CR"; case "0E": return "SO";
            case "0F": return "SI"; case "10": return "DLE"; case "11": return "DC1";
            case "12": return "DC2"; case "13": return "DC3"; case "14": return "DC4";
            case "15": return "NAK"; case "16": return "SYN"; case "17": return "ETB";
            case "18": return "CAN"; case "19": return "EM"; case "1A": return "SUB";
            case "1B": return "ESC"; case "1C": return "FS"; case "1D": return "GS";
            case "1E": return "RS"; case "1F": return "US"; case "20": return "SP";
            case "7F": return "DEL";
            default: return null;
        }
    }

    private void loadConfig(SerialPortConfig config) {
        // Set defaults per mode
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
            String abbrev = selected.substring(1, selected.length() - 1); // strip < >
            String hex = abbrevToHex(abbrev);
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
