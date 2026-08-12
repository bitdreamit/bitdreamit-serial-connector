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
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                logger.info("Serial Connector XStream permissions registered on server.");
            } else {
                logger.error("Could not find XStream instance to register permissions.");
            }
        } catch (Exception e) {
            logger.error("Failed to register Serial Connector XStream permissions", e);
        }
    }

    @Override
    public void stop() {
    }

    private XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // Search by field type
            for (java.lang.reflect.Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) return (XStream) val;
                }
            }

            // Search by method return type
            for (java.lang.reflect.Method m : ObjectXMLSerializer.class.getDeclaredMethods()) {
                if (XStream.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object val = m.invoke(serializer);
                    if (val != null) return (XStream) val;
                }
            }

            // Search superclass
            for (java.lang.reflect.Field f : ObjectXMLSerializer.class.getSuperclass().getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) return (XStream) val;
                }
            }

        } catch (Exception e) {
            logger.error("Reflection failed to find XStream", e);
        }
        return null;
    }
}