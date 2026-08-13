package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Serial Source Settings Panel (client-side).
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 * It must NOT contain SerialReceiverProperties or SerialDispatcherProperties.
 *
 * All diagnostic logging goes through log4j (Mirth Administrator client log).
 */
public class SerialSourceSettingsPanel extends ConnectorSettingsPanel {
    private static final Logger logger = Logger.getLogger(SerialSourceSettingsPanel.class);
    private final SerialConnectorSettingsPanel panel;

    static {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                xstream.processAnnotations(SerialReceiverProperties.class);
                xstream.processAnnotations(SerialPortConfig.class);
                logger.info("SerialSourceSettingsPanel: client XStream permissions + annotations registered.");
            } else {
                logger.error("SerialSourceSettingsPanel: XStream instance is NULL — " +
                             "client-side channel editing will fail!");
            }
        } catch (Exception e) {
            logger.error("SerialSourceSettingsPanel: failed to register client XStream permissions", e);
        }
    }

    public SerialSourceSettingsPanel() {
        panel = new SerialConnectorSettingsPanel(false);
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

    @SuppressWarnings("unchecked")
    private static XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) return null;

            try {
                java.lang.reflect.Method m = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = m.invoke(serializer);
                if (val instanceof XStream) return (XStream) val;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                logger.warn("SerialSourceSettingsPanel: getXStream() threw: " + e.getMessage());
            }

            XStream found = findXStreamField(serializer, serializer.getClass());
            if (found != null) return found;

            for (java.lang.reflect.Field f : serializer.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) {
                        XStream inner = findXStreamField(val, val.getClass());
                        if (inner != null) return inner;
                    }
                } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) {
            logger.error("SerialSourceSettingsPanel: reflection failed to find client XStream", e);
            return null;
        }
    }

    private static XStream findXStreamField(Object target, Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(target);
                        if (val instanceof XStream) return (XStream) val;
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
