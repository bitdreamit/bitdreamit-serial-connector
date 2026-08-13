package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.plugins.ServerPlugin;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SerialServerPlugin implements ServerPlugin {
    private static final Logger logger = Logger.getLogger(SerialServerPlugin.class);

    @Override
    public String getPluginPointName() {
        return "Serial Connector";
    }

    @Override
    public void start() {
        logger.info("SerialServerPlugin: start() called.");
        try {
            XStream xstream = findXStream();
            if (xstream != null) {
                xstream.addPermission(new WildcardTypePermission(
                        new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));

                // Also process annotations explicitly (registers aliases + implicit permissions)
                xstream.processAnnotations(SerialReceiverProperties.class);
                xstream.processAnnotations(SerialDispatcherProperties.class);
                xstream.processAnnotations(SerialPortConfig.class);

                logger.info("SerialServerPlugin: XStream permission + annotations registered successfully.");
            } else {
                logger.error("SerialServerPlugin: findXStream() returned NULL. Dumping ObjectXMLSerializer structure for diagnosis...");
                dumpSerializerStructure();
            }
        } catch (Throwable t) {
            logger.error("SerialServerPlugin: Throwable during start()", t);
        }
    }

    @Override
    public void stop() {
    }

    /**
     * Exhaustive XStream finder — checks declared type AND runtime type,
     * unwraps ThreadLocal/AtomicReference/Collections/Maps.
     */
    private XStream findXStream() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            if (serializer == null) {
                logger.error("ObjectXMLSerializer.getInstance() returned null");
                return null;
            }

            // Search strategy 1: public getter (Mirth 4.6+)
            try {
                Method m = ObjectXMLSerializer.class.getMethod("getXStream");
                Object val = m.invoke(serializer);
                if (val instanceof XStream) return (XStream) val;
            } catch (NoSuchMethodException ignored) {}

            // Search strategy 2: any method returning XStream (even if declared as Object)
            for (Method m : ObjectXMLSerializer.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    try {
                        Object val = m.invoke(serializer);
                        if (val instanceof XStream) return (XStream) val;
                    } catch (Exception ignored) {}
                }
            }

            // Search strategy 3: declared fields by declared type
            for (Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                if (XStream.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    if (val instanceof XStream) return (XStream) val;
                }
            }

            // Search strategy 4: declared fields by RUNTIME type (catches Object-typed fields)
            for (Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(serializer);
                XStream found = deepSearchForXStream(val);
                if (found != null) return found;
            }

            // Search strategy 5: superclass fields
            Class<?> clazz = ObjectXMLSerializer.class.getSuperclass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    XStream found = deepSearchForXStream(val);
                    if (found != null) return found;
                }
                clazz = clazz.getSuperclass();
            }

        } catch (Exception e) {
            logger.error("SerialServerPlugin: findXStream() reflection failed", e);
        }
        return null;
    }

    /**
     * Deep search inside wrappers: ThreadLocal, AtomicReference, Collections, Maps, arrays.
     */
    private XStream deepSearchForXStream(Object obj) {
        if (obj == null) return null;
        if (obj instanceof XStream) return (XStream) obj;

        // Unwrap ThreadLocal
        if (obj instanceof ThreadLocal) {
            return deepSearchForXStream(((ThreadLocal<?>) obj).get());
        }

        // Unwrap AtomicReference
        if (obj instanceof AtomicReference) {
            return deepSearchForXStream(((AtomicReference<?>) obj).get());
        }

        // Search inside Collections
        if (obj instanceof Collection) {
            for (Object item : (Collection<?>) obj) {
                XStream found = deepSearchForXStream(item);
                if (found != null) return found;
            }
        }

        // Search inside Maps
        if (obj instanceof Map) {
            for (Object item : ((Map<?, ?>) obj).values()) {
                XStream found = deepSearchForXStream(item);
                if (found != null) return found;
            }
        }

        // Search inside arrays
        if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                XStream found = deepSearchForXStream(java.lang.reflect.Array.get(obj, i));
                if (found != null) return found;
            }
        }

        return null;
    }

    /**
     * Diagnostic dump: logs every field name, declared type, and runtime type
     * so we can see where Mirth 4.5.2 actually stores XStream.
     */
    private void dumpSerializerStructure() {
        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== ObjectXMLSerializer field dump ===\n");

            for (Field f : ObjectXMLSerializer.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(serializer);
                sb.append(String.format("  Field: %s | Declared: %s | Runtime: %s | Value: %s\n",
                        f.getName(),
                        f.getType().getSimpleName(),
                        val != null ? val.getClass().getSimpleName() : "null",
                        val != null ? val.toString().substring(0, Math.min(50, val.toString().length())) : "null"));
            }

            Class<?> clazz = ObjectXMLSerializer.class.getSuperclass();
            while (clazz != null && clazz != Object.class) {
                sb.append("  --- Superclass: ").append(clazz.getSimpleName()).append(" ---\n");
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(serializer);
                    sb.append(String.format("  Field: %s | Declared: %s | Runtime: %s | Value: %s\n",
                            f.getName(),
                            f.getType().getSimpleName(),
                            val != null ? val.getClass().getSimpleName() : "null",
                            val != null ? val.toString().substring(0, Math.min(50, val.toString().length())) : "null"));
                }
                clazz = clazz.getSuperclass();
            }
            sb.append("=== End dump ===");
            logger.error(sb.toString());
        } catch (Exception e) {
            logger.error("Failed to dump serializer structure", e);
        }
    }
}