package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

/**
 * Client-side initializer — loaded at Mirth Administrator startup.
 *
 * This class is declared in plugin.xml under <clientClasses> so that Mirth
 * loads it via Class.forName() at startup. The static block then:
 *   1. Registers XStream security permission for our package
 *   2. Processes @XStreamAlias annotations for ALL plugin classes
 *
 * Without this, the Mirth Administrator client gets CannotResolveClassException
 * when trying to deserialize <serialReceiverProperties> or <serialDispatcherProperties>
 * tags in the channel summary XML — because the alias mapping hasn't been registered
 * until the settings panel is loaded (which is too late: getChannelSummary() runs
 * at startup, before any channel editor is opened).
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 * It MUST be listed in plugin.xml <clientClasses>.
 */
public class SerialClientInitializer {
    private static final Logger logger = Logger.getLogger(SerialClientInitializer.class);

    /** All classes that XStream must be able to deserialize on the client side. */
    private static final Class<?>[] PLUGIN_CLASSES = {
        SerialReceiverProperties.class,
        SerialDispatcherProperties.class,
        SerialPortConfig.class,
        ProtocolLogEntry.class,
        ProtocolLogEntry.Direction.class,
        SerialStatistics.class
    };

    /** Package wildcard permission. */
    private static final String[] PERMISSION_PATTERNS = {
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    };

    static {
        registerXStreamPermission();
    }

    private static void registerXStreamPermission() {
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                // Register wildcard permission for our entire package
                xstream.addPermission(new WildcardTypePermission(PERMISSION_PATTERNS));

                // Process @XStreamAlias annotations for ALL plugin classes.
                // This is the critical step: without it, XStream doesn't know that
                // <serialReceiverProperties> maps to SerialReceiverProperties.class,
                // and channel summary deserialization fails with
                // CannotResolveClassException.
                for (Class<?> clazz : PLUGIN_CLASSES) {
                    try {
                        xstream.processAnnotations(clazz);
                    } catch (Throwable t) {
                        logger.warn("SerialClientInitializer: could not process annotations for " +
                                    clazz.getName() + ": " + t.getMessage());
                    }
                }

                logger.info("SerialClientInitializer: XStream permission registered + annotations processed for " +
                            PLUGIN_CLASSES.length + " classes.");
            } else {
                logger.error("SerialClientInitializer: XStream instance is NULL — " +
                             "channel summary deserialization will fail with CannotResolveClassException! " +
                             "ObjectXMLSerializer may not be initialized yet.");
            }
        } catch (Throwable t) {
            logger.error("SerialClientInitializer: FAILED to register XStream permission: " +
                         t.getClass().getName() + ": " + t.getMessage(), t);
        }
    }

    /**
     * Find the XStream instance used by Mirth's ObjectXMLSerializer.
     * Same multi-strategy approach as SerialServerPlugin.
     */
    @SuppressWarnings("unchecked")
    private static XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) {
                logger.error("SerialClientInitializer: ObjectXMLSerializer.getInstance() returned null");
                return null;
            }

            // Strategy 1: public getXStream() method
            try {
                java.lang.reflect.Method m = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = m.invoke(serializer);
                if (val instanceof XStream) {
                    logger.info("SerialClientInitializer: found XStream via getXStream() method");
                    return (XStream) val;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                logger.warn("SerialClientInitializer: getXStream() method threw: " + e.getMessage());
            }

            // Strategy 2: field reflection on the serializer's class hierarchy
            XStream found = findXStreamField(serializer, serializer.getClass());
            if (found != null) {
                logger.info("SerialClientInitializer: found XStream via field reflection on " +
                            serializer.getClass().getName());
                return found;
            }

            // Strategy 3: walk all fields one level deep, looking inside wrapper objects
            for (java.lang.reflect.Field f : serializer.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null) {
                        XStream inner = findXStreamField(val, val.getClass());
                        if (inner != null) {
                            logger.info("SerialClientInitializer: found XStream inside field '" +
                                        f.getName() + "' of " + serializer.getClass().getName());
                            return inner;
                        }
                    }
                } catch (Exception ignored) {}
            }

            logger.error("SerialClientInitializer: could not find XStream instance in ObjectXMLSerializer. " +
                         "Serializer class: " + serializer.getClass().getName());
            return null;

        } catch (Throwable t) {
            logger.error("SerialClientInitializer: error finding XStream: " + t.getMessage(), t);
            return null;
        }
    }

    /** Walk the class hierarchy looking for an XStream-typed field. */
    private static XStream findXStreamField(Object target, Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(target);
                        if (val instanceof XStream) {
                            return (XStream) val;
                        }
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
