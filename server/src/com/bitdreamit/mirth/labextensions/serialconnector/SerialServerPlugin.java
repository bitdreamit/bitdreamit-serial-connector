package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServicePlugin;
import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Serial Connector Server Plugin.
 *
 * FIXES: CannotResolveClassException on the server side
 *
 * Mirth 4.5.2's ObjectXMLSerializer has TWO XStream instances.
 * We MUST call serializer.processAnnotations() and serializer.allowTypes()
 * (NOT xstream.processAnnotations() / xstream.allowTypesByWildcard())
 * so that registration happens on BOTH instances.
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 *           It MUST be listed in plugin.xml <serverClasses>.
 */
public class SerialServerPlugin implements ServicePlugin {
    private static final Logger logger = Logger.getLogger(SerialServerPlugin.class);

    private static final Class<?>[] PLUGIN_CLASSES = {
        SerialReceiverProperties.class,
        SerialDispatcherProperties.class,
        SerialPortConfig.class,
        ProtocolLogEntry.class,
        ProtocolLogEntry.Direction.class,
        SerialStatistics.class
    };

    private static final List<String> EXACT_TYPES = Arrays.asList(
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialReceiverProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialDispatcherProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialPortConfig",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry$Direction",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialStatistics",
        // DYNAMIC: Allow Mirth's built-in TransmissionModeProperties and all subclasses
        "com.mirth.connect.model.transmission.TransmissionModeProperties",
        "com.mirth.connect.model.transmission.framemode.FrameModeProperties"
    );

    private static final List<String> WILDCARD_TYPES = Arrays.asList(
        "com.bitdreamit.mirth.labextensions.serialconnector.**",
        // DYNAMIC: Allow any transmission mode properties (MLLP, ASTM, custom)
        "com.mirth.connect.model.transmission.**"
    );

    @Override
    public String getPluginPointName() {
        return "Serial Connector";
    }

    @Override
    public void init(Properties properties) {
        logger.info("SerialServerPlugin.init() called — registering on BOTH XStream instances.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

            // 1. CRITICAL: serializer.processAnnotations() registers on BOTH XStream instances
            serializer.processAnnotations(PLUGIN_CLASSES);
            logger.info("SerialServerPlugin: processAnnotations() called on serializer (BOTH instances).");

            // 2. CRITICAL: serializer.allowTypes() registers on BOTH XStream instances
            serializer.allowTypes(EXACT_TYPES, WILDCARD_TYPES, Collections.<String>emptyList());
            logger.info("SerialServerPlugin: allowTypes() called on serializer (BOTH instances).");

            // 3. Manual aliases on the main XStream instance (belt and suspenders)
            XStream xstream = serializer.getXStream();
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            logger.info("SerialServerPlugin: manual aliases registered on main XStream instance.");

            // DYNAMIC: Transmission modes are loaded from Mirth's ExtensionController
            // at runtime — NO custom registry needed. Same as TCP connector.
            logger.info("SerialServerPlugin: transmission modes loaded dynamically from Mirth extension system.");

            logger.info("SerialServerPlugin: registration complete — " +
                        PLUGIN_CLASSES.length + " classes on BOTH XStream instances.");

        } catch (Throwable t) {
            logger.error("SerialServerPlugin: FATAL — init() registration failed: " +
                         t.getClass().getName() + ": " + t.getMessage(), t);
        }
    }

    @Override
    public void start() {
        logger.info("SerialServerPlugin: start() called — plugin is active.");
    }

    @Override
    public void stop() {
        logger.info("SerialServerPlugin: stop() called.");
    }

    @Override
    public void update(Properties properties) {
    }

    @Override
    public Properties getDefaultProperties() {
        return new Properties();
    }

    @Override
    public ExtensionPermission[] getExtensionPermissions() {
        return null;
    }
}
