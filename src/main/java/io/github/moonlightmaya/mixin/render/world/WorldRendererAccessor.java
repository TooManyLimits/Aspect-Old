package io.github.moonlightmaya.mixin.render.world;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor
    int getRegularEntityCount();
    @Accessor
    int getBlockEntityCount();
}
