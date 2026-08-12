package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

public class SerialSourceSettingsPanel extends ConnectorSettingsPanel {
    private static final Logger logger = Logger.getLogger(SerialSourceSettingsPanel.class);
    private final SerialConnectorSettingsPanel panel;

    static {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                logger.info("Serial Connector client XStream permissions registered from source panel.");
            }
        } catch (Exception e) {
            logger.error("Failed to register client XStream permissions from source panel", e);
        }
    }

    public SerialSourceSettingsPanel() {
        panel = new SerialConnectorSettingsPanel(false);
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