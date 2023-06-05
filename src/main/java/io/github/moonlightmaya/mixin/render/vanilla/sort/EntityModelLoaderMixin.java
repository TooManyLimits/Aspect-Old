package io.github.moonlightmaya.mixin.render.vanilla.sort;

import io.github.moonlightmaya.vanilla.EntityRendererMaps;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityModelLoader.class)
public class EntityModelLoaderMixin {

    /**
     * Take the part that was created here, and give it to
     * the entity renderer maps to process and save.
     * For more details on this process, see
     * EntityRenderersMixin.
     */
    @Inject(method = "getModelPart", at = @At("RETURN"))
    public void maybeSaveCreatedPart(EntityModelLayer layer, CallbackInfoReturnable<ModelPart> cir) {
        EntityRendererMaps.onPartCreated(cir.getReturnValue());
    }

}
