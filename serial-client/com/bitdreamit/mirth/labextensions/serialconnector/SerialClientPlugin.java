package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ClientPlugin;
import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client-side plugin — extends Mirth's ClientPlugin abstract class.
 *
 * FIXES: CannotResolveClassException: serialReceiverProperties
 *
 * ROOT CAUSE:
 *   Mirth 4.5.2's ObjectXMLSerializer has TWO XStream instances:
 *     1. getXStream() — the main instance
 *     2. instanceWithReferences — used by deserializeList() for channel summaries
 *
 *   When we call xstream.processAnnotations() or xstream.alias() directly,
 *   it only registers on instance #1. But deserializeList() uses instance #2,
 *   so the alias is missing → CannotResolveClassException.
 *
 * SOLUTION:
 *   Call serializer.processAnnotations() and serializer.allowTypes() instead
 *   of calling methods directly on the XStream instance. These serializer-level
 *   methods register on BOTH XStream instances.
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
        SerialStatistics.class,
        SerialTransmissionModeProperties.class
    };

    /** Fully-qualified class names for allowTypes (exact matches). */
    private static final List<String> EXACT_TYPES = Arrays.asList(
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialReceiverProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialDispatcherProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialPortConfig",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry$Direction",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialStatistics",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialTransmissionModeProperties"
    );

    /** Wildcard patterns for allowTypesByWildcard. */
    private static final List<String> WILDCARD_TYPES = Arrays.asList(
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    );

    public SerialClientPlugin(String pluginName) {
        super(pluginName);
        logger.info("SerialClientPlugin: constructor called for '" + pluginName + "' — registering on BOTH XStream instances.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // 1. CRITICAL: Call serializer.processAnnotations() — NOT xstream.processAnnotations()!
            //    This registers @XStreamAlias on BOTH getXStream() AND instanceWithReferences.
            //    Without this, deserializeList() fails with CannotResolveClassException.
            serializer.processAnnotations(PLUGIN_CLASSES);
            logger.info("SerialClientPlugin: processAnnotations() called on serializer (registers on BOTH XStream instances).");

            // 2. CRITICAL: Call serializer.allowTypes() — NOT xstream.allowTypesByWildcard()!
            //    This registers security permissions on BOTH XStream instances.
            serializer.allowTypes(EXACT_TYPES, WILDCARD_TYPES, Collections.<String>emptyList());
            logger.info("SerialClientPlugin: allowTypes() called on serializer (registers on BOTH XStream instances).");

            // 3. Also register manual aliases on the main XStream instance (belt and suspenders)
            XStream xstream = serializer.getXStream();
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            xstream.alias("serialTransmissionModeProperties", SerialTransmissionModeProperties.class);
            logger.info("SerialClientPlugin: manual aliases registered on main XStream instance.");

            // 4. PREMIUM: Register built-in transmission mode client providers
            //    This populates the Transmission Mode dropdown dynamically.
            //    New modes added as separate plugins will auto-register here.
            SerialBuiltinModeClientProviders.registerAll();
            logger.info("SerialClientPlugin: registered " +
                        SerialTransmissionModeRegistry.getClientProviders().size() +
                        " client transmission mode providers: " +
                        Arrays.toString(SerialTransmissionModeRegistry.getClientProviders().keySet().toArray()));

            logger.info("SerialClientPlugin: registration complete — " +
                        PLUGIN_CLASSES.length + " classes on BOTH XStream instances.");

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
