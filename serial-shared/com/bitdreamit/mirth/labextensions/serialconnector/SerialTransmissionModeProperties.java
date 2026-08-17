package com.bitdreamit.mirth.labextensions.serialconnector;

import com.mirth.connect.donkey.util.purge.Purgable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for serial transmission mode properties.
 * Modeled after Mirth's TransmissionModeProperties so that new modes
 * (MLLP, ASTM, Frame, custom) can be added as separate providers without
 * modifying the core connector classes.
 *
 * Each subclass stores its own mode-specific fields.
 * The pluginPointName identifies which provider handles this mode.
 */
public class SerialTransmissionModeProperties implements Serializable, Purgable {
    private static final long serialVersionUID = 1L;

    private String pluginPointName;

    public SerialTransmissionModeProperties(String pluginPointName) {
        this.pluginPointName = pluginPointName;
    }

    public String getPluginPointName() {
        return pluginPointName;
    }

    public void setPluginPointName(String pluginPointName) {
        this.pluginPointName = pluginPointName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SerialTransmissionModeProperties) {
            SerialTransmissionModeProperties other = (SerialTransmissionModeProperties) obj;
            if (other.getPluginPointName() != null && other.getPluginPointName().equals(pluginPointName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pluginPointName != null ? pluginPointName.hashCode() : 0;
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purged = new HashMap<>();
        purged.put("pluginPointName", pluginPointName);
        return purged;
    }

    @Override
    public String toString() {
        return pluginPointName != null ? pluginPointName : "UNKNOWN";
    }
}
