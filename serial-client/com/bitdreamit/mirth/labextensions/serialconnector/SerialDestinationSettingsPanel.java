package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import org.apache.log4j.Logger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Serial Destination Settings Panel (client-side).
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 *
 * XStream registration is handled by SerialClientPlugin constructor, NOT here.
 */
public class SerialDestinationSettingsPanel extends ConnectorSettingsPanel {
    private static final Logger logger = Logger.getLogger(SerialDestinationSettingsPanel.class);
    private final SerialConnectorSettingsPanel panel;

    public SerialDestinationSettingsPanel() {
        panel = new SerialConnectorSettingsPanel(true);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;  // ← FIX: fill BOTH so panel stretches full width
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(panel, gbc);
    }

    @Override
    public String getConnectorName() { return panel.getConnectorName(); }

    @Override
    public ConnectorProperties getProperties() { return panel.getProperties(); }

    @Override
    public void setProperties(ConnectorProperties properties) { panel.setProperties(properties); }

    @Override
    public ConnectorProperties getDefaults() { return panel.getDefaults(); }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        return panel.checkProperties(properties, highlight);
    }

    @Override
    public void resetInvalidProperties() { panel.resetInvalidProperties(); }
}
