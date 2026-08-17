package com.bitdreamit.mirth.labextensions.serialconnector;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Client-side UI provider for serial transmission modes.
 * Each provider supplies a settings component (panel or button) that
 * the connector settings panel displays when the corresponding mode is selected.
 *
 * Modeled after Mirth's TransmissionModeClientProvider so new modes can
 * be added without modifying the SerialConnectorSettingsPanel.
 */
public abstract class SerialTransmissionModeClientProvider implements ActionListener {

    public static final String CHANGE_SAMPLE_LABEL_COMMAND = "changesamplelabel";
    public static final String CHANGE_SAMPLE_VALUE_COMMAND = "changesamplevalue";

    protected ActionListener actionListener;

    /**
     * Initialize the plugin with a connector's ActionListener.
     */
    public void initialize(ActionListener actionListener) {
        this.actionListener = actionListener;
        changeSampleLabel();
    }

    /**
     * Returns the plugin point name (e.g. "RAW", "MLLP").
     */
    public abstract String getPluginPointName();

    /**
     * Returns the current properties from the UI.
     */
    public abstract SerialTransmissionModeProperties getProperties();

    /**
     * Returns the default properties.
     */
    public abstract SerialTransmissionModeProperties getDefaultProperties();

    /**
     * Load properties into the UI.
     */
    public abstract void setProperties(SerialTransmissionModeProperties properties);

    /**
     * Validate the UI values.
     */
    public abstract boolean checkProperties(SerialTransmissionModeProperties properties, boolean highlight);

    /**
     * Reset invalid component backgrounds.
     */
    public abstract void resetInvalidProperties();

    /**
     * Returns the settings component to display.
     * Can be a full panel or a button that opens a dialog.
     */
    public abstract JComponent getSettingsComponent();

    /**
     * Returns the sample label for this mode.
     */
    public abstract String getSampleLabel();

    /**
     * Returns the sample value (e.g. "<VT><message><FS><CR>").
     */
    public abstract String getSampleValue();

    /**
     * Notifies the action listener of a sample label change.
     */
    protected void changeSampleLabel() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    CHANGE_SAMPLE_LABEL_COMMAND));
        }
    }

    /**
     * Notifies the action listener of a sample value change.
     */
    protected void changeSampleValue() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    CHANGE_SAMPLE_VALUE_COMMAND));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Default no-op; subclasses override if needed
    }
}
