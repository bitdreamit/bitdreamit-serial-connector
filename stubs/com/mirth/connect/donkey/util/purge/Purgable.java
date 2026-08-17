package com.mirth.connect.donkey.util.purge;

import java.util.Map;

/**
 * STUB — DO NOT DEPLOY. Compile-time fallback for Purgable.
 * Add donkey-server.jar to classpath instead. See stubs/README.md.
 */
public interface Purgable {
    String getPluginPointName();
    Map<String, Object> getPurgedProperties();
}
