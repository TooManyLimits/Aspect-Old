package io.github.moonlightmaya.util;

import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;

public class RenderUtils {

    /**
     * Says "RenderCall", but is just a runnable
     */
    public static void executeOnRenderThread(RenderCall r) {
        if (RenderSystem.isOnRenderThreadOrInit())
            r.execute();
        else
            RenderSystem.recordRenderCall(r);
    }
}
