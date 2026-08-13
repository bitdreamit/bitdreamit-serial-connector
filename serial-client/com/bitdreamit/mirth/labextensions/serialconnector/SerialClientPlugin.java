package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ClientPlugin;
import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

/**
 * Client-side plugin — extends Mirth's ClientPlugin abstract class.
 *
 * FIXES: CannotResolveClassException: serialReceiverProperties
 *
 * ROOT CAUSE (Mirth bug #6348):
 *   Mirth 4.4.1+ uses XStream 1.4.20 with an allowlist (not denylist).
 *   The SERVER side reads xstream.allowtypes from mirth.properties.
 *   The CLIENT side does NOT read this property — it's a known Mirth bug.
 *   So custom plugin classes are BLOCKED by XStream security on the client.
 *
 * SOLUTION (confirmed by Mirth developers in issue #6348):
 *   "Also by the developer of the plugin, by explicitly whitelisting the
 *    relevant classes/packages in the code."
 *
 *   We must call xstream.allowTypes() and xstream.allowTypesByWildcard()
 *   DIRECTLY on the XStream instance — NOT addPermission().
 *
 *   Mirth 4.5.2's ObjectXMLSerializer has TWO XStream instances:
 *     1. getXStream() — the main instance
 *     2. instanceWithReferences — used for deserializing objects with references
 *   We must register on BOTH.
 *
 * CRITICAL: This class MUST exist ONLY in serial-client.jar.
 *           It MUST be listed in plugin.xml <clientClasses>.
 */
public class SerialClientPlugin extends ClientPlugin {
    private static final Logger logger = Logger.getLogger(SerialClientPlugin.class);

    /** All classes that XStream must be able to deserialize on the client side. */
    private static final Class<?>[] PLUGIN_CLASSES = {
        SerialReceiverProperties.class,
        SerialDispatcherProperties.class,
        SerialPortConfig.class,
        ProtocolLogEntry.class,
        ProtocolLogEntry.Direction.class,
        SerialStatistics.class
    };

    /** Fully-qualified class names for allowTypesByWildcard. */
    private static final String[] WILDCARD_TYPES = {
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    };

    /** Fully-qualified class names for allowTypes (exact matches). */
    private static final String[] EXACT_TYPES = {
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialReceiverProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialDispatcherProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialPortConfig",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry$Direction",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialStatistics"
    };

    public SerialClientPlugin(String pluginName) {
        super(pluginName);
        logger.info("SerialClientPlugin: constructor called for '" + pluginName + "' — registering XStream types.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            XStream xstream = serializer.getXStream();

            // 1. Allow types by wildcard (CRITICAL — this is the Mirth 4.5.2 API)
            xstream.allowTypesByWildcard(WILDCARD_TYPES);
            logger.info("SerialClientPlugin: allowTypesByWildcard registered.");

            // 2. Allow exact types (belt and suspenders)
            xstream.allowTypes(EXACT_TYPES);
            logger.info("SerialClientPlugin: allowTypes registered for " + EXACT_TYPES.length + " classes.");

            // 3. Process @XStreamAlias annotations
            for (Class<?> clazz : PLUGIN_CLASSES) {
                try {
                    xstream.processAnnotations(clazz);
                    logger.info("SerialClientPlugin: processAnnotations(" + clazz.getSimpleName() + ") OK");
                } catch (Throwable t) {
                    logger.warn("SerialClientPlugin: processAnnotations(" +
                                clazz.getSimpleName() + ") failed: " + t.getMessage());
                }
            }

            // 4. Manual alias registration (bulletproof)
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            logger.info("SerialClientPlugin: manual aliases registered.");

            logger.info("SerialClientPlugin: registration complete.");

        } catch (Throwable t) {
            logger.error("SerialClientPlugin: FATAL — registration failed: " +
                         t.getClass().getName() + ": " + t.getMessage(), t);
        }
    }

    @Override
    public String getPluginPointName() {
        return "Serial Connector Client";
    }

    @Override
    public void start() {
        logger.info("SerialClientPlugin: start() called.");
    }

    @Override
    public void stop() {
        logger.info("SerialClientPlugin: stop() called.");
    }

    @Override
    public void reset() {
        logger.info("SerialClientPlugin: reset() called.");
    }
}
