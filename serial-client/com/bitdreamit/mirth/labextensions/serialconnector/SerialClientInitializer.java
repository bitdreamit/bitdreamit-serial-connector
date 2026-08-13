package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side initializer — loaded at Mirth Administrator startup.
 *
 * PURPOSE:
 *   Fixes CannotResolveClassException: serialReceiverProperties
 *
 *   When Mirth Administrator calls getChannelSummary(), the response XML contains
 *   <serialReceiverProperties> tags. XStream needs to know these map to our classes.
 *   This class registers the mappings via THREE mechanisms:
 *     1. xstream.processAnnotations(Class) — reads @XStreamAlias
 *     2. xstream.alias(String, Class) — explicit manual mapping
 *     3. Security permission via WildcardTypePermission
 *
 *   It also tries MULTIPLE XStream instances:
 *     - ObjectXMLSerializer.getInstance().getXStream()
 *     - Any XStream fields found via reflection
 *   Because Mirth may use different XStream instances in different contexts
 *   (e.g. XmlMessageBodyReader may have its own).
 *
 * HOW IT GETS LOADED:
 *   plugin.xml <clientClasses> triggers Class.forName() → static block runs.
 *   A background retry thread handles the case where XStream isn't ready yet.
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 *           It MUST be listed in plugin.xml <clientClasses>.
 */
public class SerialClientInitializer {
    private static final Logger logger = Logger.getLogger(SerialClientInitializer.class);

    private static final Class<?>[] PLUGIN_CLASSES = {
        SerialReceiverProperties.class,
        SerialDispatcherProperties.class,
        SerialPortConfig.class,
        ProtocolLogEntry.class,
        ProtocolLogEntry.Direction.class,
        SerialStatistics.class
    };

    private static final String[] PERMISSION_PATTERNS = {
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    };

    private static final String[][] ALIASES = {
        {"serialReceiverProperties",   "com.bitdreamit.mirth.labextensions.serialconnector.SerialReceiverProperties"},
        {"serialDispatcherProperties", "com.bitdreamit.mirth.labextensions.serialconnector.SerialDispatcherProperties"},
        {"serialPortConfig",           "com.bitdreamit.mirth.labextensions.serialconnector.SerialPortConfig"},
        {"protocolLogEntry",           "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry"},
        {"serialStatistics",           "com.bitdreamit.mirth.labextensions.serialconnector.SerialStatistics"}
    };

    static {
        logger.info("SerialClientInitializer: class loaded.");
        // Try registration immediately
        boolean ok = registerAll();

        // If immediate registration failed (XStream not ready), start a background
        // retry thread that keeps trying until it succeeds or times out.
        if (!ok) {
            logger.warn("SerialClientInitializer: immediate registration failed — starting retry thread.");
            Thread retry = new Thread(() -> {
                for (int i = 1; i <= 20; i++) {  // retry for ~10 seconds
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    logger.info("SerialClientInitializer: retry #" + i);
                    if (registerAll()) {
                        logger.info("SerialClientInitializer: registration succeeded on retry #" + i);
                        return;
                    }
                }
                logger.error("SerialClientInitializer: registration FAILED after 20 retries. " +
                             "Channel list will not load.");
            }, "SerialClientInit-Retry");
            retry.setDaemon(true);
            retry.start();
        }
    }

    /**
     * Try to register with ALL discoverable XStream instances.
     * @return true if at least one registration succeeded
     */
    private static boolean registerAll() {
        List<XStream> instances = findAllXStreamInstances();
        if (instances.isEmpty()) {
            logger.warn("SerialClientInitializer: no XStream instance found yet.");
            return false;
        }

        boolean anySuccess = false;
        for (int i = 0; i < instances.size(); i++) {
            XStream xs = instances.get(i);
            try {
                registerWithXStream(xs, "instance-" + i);
                anySuccess = true;
            } catch (Throwable t) {
                logger.error("SerialClientInitializer: failed with XStream instance " + i + ": " + t.getMessage());
            }
        }

        if (anySuccess) {
            logger.info("SerialClientInitializer: registration complete with " + instances.size() + " XStream instance(s).");
        }
        return anySuccess;
    }

    private static void registerWithXStream(XStream xstream, String label) {
        // 1. Security permission
        try {
            xstream.addPermission(new WildcardTypePermission(PERMISSION_PATTERNS));
        } catch (Throwable t) {
            logger.warn("SerialClientInitializer: [" + label + "] addPermission failed: " + t.getMessage());
        }

        // 2. processAnnotations for all plugin classes
        for (Class<?> clazz : PLUGIN_CLASSES) {
            try {
                xstream.processAnnotations(clazz);
            } catch (Throwable t) {
                logger.warn("SerialClientInitializer: [" + label + "] processAnnotations(" +
                            clazz.getSimpleName() + ") failed: " + t.getMessage());
            }
        }

        // 3. Manual alias registration (bulletproof fallback)
        for (String[] entry : ALIASES) {
            String tag = entry[0];
            String className = entry[1];
            try {
                Class<?> clazz = Class.forName(className);
                xstream.alias(tag, clazz);
                logger.info("SerialClientInitializer: [" + label + "] alias('" + tag + "', " +
                            clazz.getSimpleName() + ") registered.");
            } catch (Throwable t) {
                logger.error("SerialClientInitializer: [" + label + "] alias('" + tag +
                             "') FAILED: " + t.getMessage());
            }
        }
    }

    /**
     * Find ALL XStream instances reachable from ObjectXMLSerializer.
     * Returns a list (may contain duplicates if the same instance is found
     * via multiple paths — that's fine, registration is idempotent).
     */
    private static List<XStream> findAllXStreamInstances() {
        List<XStream> result = new ArrayList<>();
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) {
                return result;
            }

            // Strategy 1: getXStream() method
            try {
                java.lang.reflect.Method m = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = m.invoke(serializer);
                if (val instanceof XStream) {
                    result.add((XStream) val);
                    logger.info("SerialClientInitializer: found XStream via getXStream()");
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                logger.warn("SerialClientInitializer: getXStream() threw: " + e.getMessage());
            }

            // Strategy 2: scan all fields of serializer and its superclasses
            collectXStreamFields(serializer, serializer.getClass(), result);

            // Strategy 3: walk one level deep into non-primitive fields
            for (Field f : serializer.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val != null && !val.getClass().isPrimitive()
                            && !val.getClass().getName().startsWith("java.lang")) {
                        collectXStreamFields(val, val.getClass(), result);
                    }
                } catch (Exception ignored) {}
            }

            // Strategy 4: superclass chain of ObjectXMLSerializer
            Class<?> sc = ObjectXMLSerializer.class.getSuperclass();
            while (sc != null && sc != Object.class) {
                collectXStreamFields(serializer, sc, result);
                sc = sc.getSuperclass();
            }

        } catch (Throwable t) {
            logger.error("SerialClientInitializer: error finding XStream: " + t.getMessage(), t);
        }
        return result;
    }

    private static void collectXStreamFields(Object target, Class<?> clazz, List<XStream> result) {
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(target);
                        if (val instanceof XStream) {
                            result.add((XStream) val);
                        }
                    } catch (Exception ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
