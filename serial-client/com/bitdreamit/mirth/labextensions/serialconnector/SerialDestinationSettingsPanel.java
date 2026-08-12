package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;

public class SerialDestinationSettingsPanel extends ConnectorSettingsPanel {
    private final SerialConnectorSettingsPanel panel;

    public SerialDestinationSettingsPanel() {
        panel = new SerialConnectorSettingsPanel(true);
        setLayout(new java.awt.BorderLayout());
        add(panel, java.awt.BorderLayout.CENTER);
    }

    @Override
    public String getConnectorName() {
        return panel.getConnectorName();
    }

    @Override
    public ConnectorProperties getProperties() {
        return panel.getProperties();
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        panel.setProperties(properties);
    }

    @Override
    public ConnectorProperties getDefaults() {
        return panel.getDefaults();
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        return panel.checkProperties(properties, highlight);
    }

    @Override
    public void resetInvalidProperties() {
        panel.resetInvalidProperties();
    }
}