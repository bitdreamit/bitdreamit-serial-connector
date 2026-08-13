package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServicePlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Serial Connector Server Plugin.
 *
 * Implements Mirth's ServicePlugin interface — the OFFICIAL way to register
 * server-side XStream configuration. Based on Mirth's own TcpServicePlugin.
 *
 * The init(Properties) method is called by Mirth AFTER ObjectXMLSerializer
 * is fully initialized. This is the correct place to register XStream aliases.
 *
 * CRITICAL: This class MUST exist ONLY in serial-server.jar.
 *           It MUST be listed in plugin.xml <serverClasses>.
 */
public class SerialServerPlugin implements ServicePlugin {
    private static final Logger logger = Logger.getLogger(SerialServerPlugin.class);

    /** All classes that XStream must be able to deserialize on the server side. */
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

    @Override
    public String getPluginPointName() {
        return "Serial Connector";
    }

    /**
     * Called by Mirth AFTER ObjectXMLSerializer is initialized.
     * This is the correct place to register XStream aliases and permissions.
     */
    @Override
    public void init(Properties properties) {
        logger.info("SerialServerPlugin.init() called — registering XStream aliases.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            XStream xstream = serializer.getXStream();

            // 1. Register security permission
            xstream.addPermission(new WildcardTypePermission(PERMISSION_PATTERNS));
            logger.info("SerialServerPlugin: security permission registered.");

            // 2. Process @XStreamAlias annotations for all plugin classes
            for (Class<?> clazz : PLUGIN_CLASSES) {
                try {
                    xstream.processAnnotations(clazz);
                    logger.info("SerialServerPlugin: processAnnotations(" + clazz.getSimpleName() + ") OK");
                } catch (Throwable t) {
                    logger.warn("SerialServerPlugin: processAnnotations(" +
                                clazz.getSimpleName() + ") failed: " + t.getMessage());
                }
            }

            // 3. Manual alias registration (bulletproof fallback)
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            logger.info("SerialServerPlugin: manual aliases registered.");

            logger.info("SerialServerPlugin: registration complete — " +
                        PLUGIN_CLASSES.length + " classes processed.");

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
        // No-op — we don't have configurable plugin properties
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
