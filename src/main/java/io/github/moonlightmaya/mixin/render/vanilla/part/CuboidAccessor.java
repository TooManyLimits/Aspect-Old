package io.github.moonlightmaya.mixin.render.vanilla.part;

import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Cuboid.class)
public interface CuboidAccessor {
    @Accessor
    ModelPart.Quad[] getSides();
}
