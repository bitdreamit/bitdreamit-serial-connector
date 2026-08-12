package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServerPlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.log4j.Logger;

public class SerialServerPlugin implements ServerPlugin {
    private static final Logger logger = Logger.getLogger(SerialServerPlugin.class);

    @Override
    public String getPluginPointName() {
        return "Serial Connector";
    }

    @Override
    public void start() {
        try {
            XStream xstream = ObjectXMLSerializer.getInstance().getXStream();
            xstream.addPermission(new WildcardTypePermission(
                    new String[]{"com.bitdreamit.mirth.labextensions.serialconnector.**"}));
            logger.info("Serial Connector XStream permissions registered on server.");
        } catch (Exception e) {
            logger.error("Failed to register Serial Connector XStream permissions", e);
        }
    }

    @Override
    public void stop() {
    }
}