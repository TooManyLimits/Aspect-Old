package io.github.moonlightmaya.mixin.render.vanilla.sort;

import io.github.moonlightmaya.script.vanilla.EntityRendererMaps;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRendererFactory.Context.class)
public class EntityRendererFactoryContextMixin {

    /**
     * Take the part that was created here, and give it to
     * the entity renderer maps to process and save.
     * For more details on this process, see
     * EntityRenderersMixin.
     */
    @Inject(method = "getPart", at = @At("RETURN"))
    public void maybeSaveCreatedPart(EntityModelLayer layer, CallbackInfoReturnable<ModelPart> cir) {
        EntityRendererMaps.onPartCreated(cir.getReturnValue());
    }

}
