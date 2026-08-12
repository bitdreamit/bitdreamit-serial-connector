package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServerPlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

public class SerialServerPlugin implements ServerPlugin {
    private static final Logger logger = Logger.getLogger(SerialServerPlugin.class);

    @Override
    public String getPluginPointName() {
        return "BitDreamIT Serial Connector";
    }

    @Override
    public void start() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            XStream xstream = getXStream(serializer);

            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                logger.info("Serial Connector XStream permissions registered.");
            } else {
                logger.error("Could not access XStream instance from ObjectXMLSerializer");
            }
        } catch (Exception e) {
            logger.error("Failed to register Serial Connector XStream permissions", e);
        }
    }

    @Override
    public void stop() {
    }

    private XStream getXStream(ObjectXMLSerializer serializer) {
        try {
            // Try method first
            java.lang.reflect.Method m = ObjectXMLSerializer.class.getDeclaredMethod("getXStream");
            m.setAccessible(true);
            return (XStream) m.invoke(serializer);
        } catch (Exception e) {
            // Fallback to field
            try {
                java.lang.reflect.Field f = ObjectXMLSerializer.class.getDeclaredField("xstream");
                f.setAccessible(true);
                return (XStream) f.get(serializer);
            } catch (Exception ex) {
                logger.error("Could not access XStream from ObjectXMLSerializer", ex);
                return null;
            }
        }
    }
}