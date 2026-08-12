package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SerialClientXStreamPermission {
    private static final Logger logger = Logger.getLogger(SerialClientXStreamPermission.class);

    private static final String WILDCARD_PACKAGE = "com.bitdreamit.mirth.labextensions.serialconnector.**";

    static {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                // Register permission to safely deserialize your custom serial connector properties
                xstream.addPermission(new WildcardTypePermission(new String[]{WILDCARD_PACKAGE}));
                logger.info("Serial Connector client XStream permissions registered successfully.");
            } else {
                logger.error("Could not find client XStream instance via reflection.");
            }
        } catch (Exception e) {
            logger.error("Failed to register client XStream permissions", e);
        }
    }

    /**
     * Public no-arg constructor so Mirth can safely instantiate this
     * class if referenced directly as a client hook component.
     */
    public SerialClientXStreamPermission() {
    }

    /**
     * Extracts the underlying XStream instance from ObjectXMLSerializer using optimized reflection.
     */
    private static XStream findXStream() {
        ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
        if (serializer == null) {
            return null;
        }

        // Strategy 1: Target the known superclass field ("xstream") directly for maximum speed in 4.5.x
        Class<?> clazz = serializer.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField("xstream");
                field.setAccessible(true);
                Object val = field.get(serializer);
                if (val instanceof XStream) {
                    return (XStream) val;
                }
            } catch (NoSuchFieldException e) {
                // Walk up into parent class hierarchy (e.g., XStreamSerializer)
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                logger.debug("Direct field extraction failed, falling back to scanning routines", e);
                break;
            }
        }

        // Strategy 2: Fallback deep scan across methods in case of minor internal differences
        try {
            for (Method m : ObjectXMLSerializer.class.getMethods()) {
                if (XStream.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    Object val = m.invoke(serializer);
                    if (val instanceof XStream) {
                        return (XStream) val;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fallback method scanning failed to find client XStream", e);
        }

        return null;
    }
}
