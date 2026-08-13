package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServicePlugin;
import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Serial Connector Server Plugin.
 *
 * FIXES: ForbiddenClassException on the server side
 *
 * Mirth 4.5.2 uses XStream 1.4.20 with an allowlist.
 * We must use xstream.allowTypesByWildcard() — NOT addPermission().
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

    private static final String[] WILDCARD_TYPES = {
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    };

    private static final String[] EXACT_TYPES = {
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialReceiverProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialDispatcherProperties",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialPortConfig",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry",
        "com.bitdreamit.mirth.labextensions.serialconnector.ProtocolLogEntry$Direction",
        "com.bitdreamit.mirth.labextensions.serialconnector.SerialStatistics"
    };

    @Override
    public String getPluginPointName() {
        return "Serial Connector";
    }

    @Override
    public void init(Properties properties) {
        logger.info("SerialServerPlugin.init() called — registering XStream types.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            XStream xstream = serializer.getXStream();

            // 1. Allow types by wildcard (CRITICAL — Mirth 4.5.2 API)
            xstream.allowTypesByWildcard(WILDCARD_TYPES);
            logger.info("SerialServerPlugin: allowTypesByWildcard registered.");

            // 2. Allow exact types
            xstream.allowTypes(EXACT_TYPES);
            logger.info("SerialServerPlugin: allowTypes registered for " + EXACT_TYPES.length + " classes.");

            // 3. Process @XStreamAlias annotations
            for (Class<?> clazz : PLUGIN_CLASSES) {
                try {
                    xstream.processAnnotations(clazz);
                    logger.info("SerialServerPlugin: processAnnotations(" + clazz.getSimpleName() + ") OK");
                } catch (Throwable t) {
                    logger.warn("SerialServerPlugin: processAnnotations(" +
                                clazz.getSimpleName() + ") failed: " + t.getMessage());
                }
            }

            // 4. Manual alias registration
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            logger.info("SerialServerPlugin: manual aliases registered.");

            logger.info("SerialServerPlugin: registration complete.");

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
