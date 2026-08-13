package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ClientPlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Client-side plugin — extends Mirth's ClientPlugin abstract class.
 *
 * This is the OFFICIAL way Mirth loads client-side plugin code. Mirth:
 *   1. Reads plugin.xml <clientClasses> to find the class name
 *   2. Calls Class.forName(name) to load the class
 *   3. Calls constructor.newInstance(pluginName) to INSTANTIATE it
 *   4. The constructor runs AFTER ObjectXMLSerializer is fully initialized
 *
 * This fixes CannotResolveClassException: serialReceiverProperties
 * because the constructor registers XStream aliases at the correct time.
 *
 * Based on Mirth's own TcpClientPlugin implementation pattern.
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

    /** Package wildcard permission. */
    private static final String[] PERMISSION_PATTERNS = {
        "com.bitdreamit.mirth.labextensions.serialconnector.**"
    };

    /**
     * Constructor — called by Mirth AFTER ObjectXMLSerializer is initialized.
     * This is the correct place to register XStream aliases.
     *
     * @param pluginName the plugin point name from plugin.xml
     */
    public SerialClientPlugin(String pluginName) {
        super(pluginName);
        logger.info("SerialClientPlugin: constructor called for '" + pluginName + "' — registering XStream aliases.");

        try {
            ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
            XStream xstream = serializer.getXStream();

            // 1. Register security permission
            xstream.addPermission(new WildcardTypePermission(PERMISSION_PATTERNS));
            logger.info("SerialClientPlugin: security permission registered.");

            // 2. Process @XStreamAlias annotations for all plugin classes
            for (Class<?> clazz : PLUGIN_CLASSES) {
                try {
                    xstream.processAnnotations(clazz);
                    logger.info("SerialClientPlugin: processAnnotations(" + clazz.getSimpleName() + ") OK");
                } catch (Throwable t) {
                    logger.warn("SerialClientPlugin: processAnnotations(" +
                                clazz.getSimpleName() + ") failed: " + t.getMessage());
                }
            }

            // 3. Manual alias registration (bulletproof fallback)
            // XStream auto-derives tag names from class names (SerialReceiverProperties → serialReceiverProperties)
            // but only if the class is registered. This explicit alias ensures it works even if
            // annotation processing has issues.
            xstream.alias("serialReceiverProperties", SerialReceiverProperties.class);
            xstream.alias("serialDispatcherProperties", SerialDispatcherProperties.class);
            xstream.alias("serialPortConfig", SerialPortConfig.class);
            xstream.alias("protocolLogEntry", ProtocolLogEntry.class);
            xstream.alias("serialStatistics", SerialStatistics.class);
            logger.info("SerialClientPlugin: manual aliases registered.");

            logger.info("SerialClientPlugin: registration complete — " +
                        PLUGIN_CLASSES.length + " classes processed.");

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
