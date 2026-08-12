package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class SerialDestinationSettingsPanel extends ConnectorSettingsPanel {
    private static final Logger logger = Logger.getLogger(SerialDestinationSettingsPanel.class);
    private final SerialConnectorSettingsPanel panel;

    static {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                logger.info("Serial Connector client XStream permissions registered from destination panel.");
            }
        } catch (Exception e) {
            logger.error("Failed to register client XStream permissions from destination panel", e);
        }
    }

    public SerialDestinationSettingsPanel() {
        panel = new SerialConnectorSettingsPanel(true);
        // Anchor to top-left (NORTHWEST) so panel stays left-aligned, not centered
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(panel, gbc);
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

    private static XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) return null;

            for (java.lang.reflect.Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) return (XStream) val;
                }
            }

            for (java.lang.reflect.Method m : ObjectXMLSerializer.class.getDeclaredMethods()) {
                if (XStream.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object val = m.invoke(serializer);
                    if (val != null) return (XStream) val;
                }
            }

            Class<?> clazz = ObjectXMLSerializer.class.getSuperclass();
            while (clazz != null && clazz != Object.class) {
                for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                    if (XStream.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(serializer);
                        if (val != null) return (XStream) val;
                    }
                }
                clazz = clazz.getSuperclass();
            }

        } catch (Exception e) {
            logger.error("Reflection failed to find client XStream", e);
        }
        return null;
    }
}