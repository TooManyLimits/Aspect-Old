package io.github.moonlightmaya.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;

@Deprecated //unused lol, just leaving it here in case we want it later
public enum AspectRenderType {
    CUTOUT(RenderLayer::getEntityCutout),
    CUTOUT_NO_CULL(RenderLayer::getEntityCutoutNoCull),
    TRANSLUCENT(RenderLayer::getEntityTranslucentCull),
    TRANSLUCENT_NO_CULL(RenderLayer::getEntityTranslucent),
    END_PORTAL(RenderLayer::getEndPortal),
    END_GATEWAY(RenderLayer::getEndGateway),
    GLINT(RenderLayer::getEntityGlint);

    public final Function<Identifier, RenderLayer> function;
    AspectRenderType(Function<Identifier, RenderLayer> compatRenderLayerSupplier) {
        function = compatRenderLayerSupplier;
    }
    AspectRenderType(Supplier<RenderLayer> compatRenderLayerSupplier) {
        function = tex -> compatRenderLayerSupplier.get();
    }
}
