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
        return "Serial Connector";
    }

    @Override
    public void start() {
        logger.info("SerialServerPlugin.start() called.");
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
                logger.info("Serial Connector XStream permissions registered on server.");
            } else {
                logger.error("SerialServerPlugin: XStream instance is null. Permissions NOT registered.");
            }
        } catch (Exception e) {
            logger.error("SerialServerPlugin: Failed to register XStream permissions", e);
        }
    }

    @Override
    public void stop() {
    }


    private XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) {
                logger.error("ObjectXMLSerializer.getInstance() returned null");
                return null;
            }

            // Try public getter first (Mirth 4.6+)
            try {
                java.lang.reflect.Method getter = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = getter.invoke(serializer);
                if (val != null) {
                    logger.debug("Found XStream via public getXStream()");
                    return (XStream) val;
                }
            } catch (NoSuchMethodException e) {
                logger.debug("Public getXStream() not found, using reflection.");
            }

            // Reflect on declared fields
            for (java.lang.reflect.Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) {
                        logger.debug("Found XStream via field: " + f.getName());
                        return (XStream) val;
                    }
                }
            }

            // Reflect on superclass
            Class<?> clazz = ObjectXMLSerializer.class.getSuperclass();
            while (clazz != null && clazz != Object.class) {
                for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                    if (XStream.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(serializer);
                        if (val != null) {
                            logger.debug("Found XStream via superclass field: " + f.getName());
                            return (XStream) val;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }

        } catch (Exception e) {
            logger.error("SerialServerPlugin: Reflection failed to find XStream", e);
        }
        return null;
    }
}