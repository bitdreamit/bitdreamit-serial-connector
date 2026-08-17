package com.bitdreamit.mirth.labextensions.serialconnector;

import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for serial transmission mode providers.
 *
 * Holds both server-side providers (SerialTransmissionModeProvider) and
 * client-side providers (SerialTransmissionModeClientProvider).
 *
 * Modeled after Mirth's ExtensionController.getTransmissionModeProviders().
 *
 * New modes can register themselves by calling registerProvider().
 * The connector looks up the provider by name at runtime.
 */
public class SerialTransmissionModeRegistry {
    private static final Logger logger = Logger.getLogger(SerialTransmissionModeRegistry.class);

    private static final Map<String, SerialTransmissionModeProvider> serverProviders = new LinkedHashMap<>();
    private static final Map<String, SerialTransmissionModeClientProvider> clientProviders = new LinkedHashMap<>();

    // ===== Server-side providers =====

    public static void registerServerProvider(SerialTransmissionModeProvider provider) {
        String name = provider.getPluginPointName();
        serverProviders.put(name, provider);
        logger.info("Registered serial server transmission mode provider: " + name);
    }

    public static SerialTransmissionModeProvider getServerProvider(String name) {
        if (name == null) return null;
        return serverProviders.get(name.toUpperCase());
    }

    public static Map<String, SerialTransmissionModeProvider> getServerProviders() {
        return serverProviders;
    }

    // ===== Client-side providers =====

    public static void registerClientProvider(SerialTransmissionModeClientProvider provider) {
        String name = provider.getPluginPointName();
        clientProviders.put(name, provider);
        logger.info("Registered serial client transmission mode provider: " + name);
    }

    public static SerialTransmissionModeClientProvider getClientProvider(String name) {
        if (name == null) return null;
        return clientProviders.get(name.toUpperCase());
    }

    public static Map<String, SerialTransmissionModeClientProvider> getClientProviders() {
        return clientProviders;
    }

    // ===== Utility =====

    public static String[] getAvailableModes() {
        return serverProviders.keySet().toArray(new String[0]);
    }

    public static boolean isModeAvailable(String name) {
        if (name == null) return false;
        return serverProviders.containsKey(name.toUpperCase());
    }
}
