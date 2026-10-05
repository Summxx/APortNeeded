package com.summax.apn.architecture.client.render.model.baked;

import com.summax.apn.architecture.core.ArchitectureLog;

import java.lang.reflect.Method;

/**
 * Checks if a shader pack is in use through the Iris API, if it's present.
 */
public final class ShaderPacks {

    private static final long CHECK_INTERVAL = 1_000_000_000L;

    private static boolean lookedUp;
    private static Object api;
    private static Method isShaderPackInUse;
    private static volatile boolean inUse;
    private static volatile long lastCheck;
    private static volatile boolean checked;

    private ShaderPacks() {
    }

    /**
     * @return true if a shader pack is in use, checked at most once a second.
     */
    public static boolean inUse() {
        long now = System.nanoTime();
        if (!checked || now - lastCheck > CHECK_INTERVAL) {
            inUse = query();
            lastCheck = now;
            checked = true;
        }
        return inUse;
    }

    private static synchronized boolean query() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                var apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = apiClass.getMethod("getInstance").invoke(null);
                isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                api = null;
            }
        }
        if (api == null)
            return false;
        try {
            return (boolean) isShaderPackInUse.invoke(api);
        } catch (ReflectiveOperationException e) {
            ArchitectureLog.error("Failed to query the shader pack state.", e);
            api = null;
            return false;
        }
    }
}
