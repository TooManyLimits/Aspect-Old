package io.github.moonlightmaya.mixin.render.models;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Evil mixin
 *
 * JUSTIFICATION:
 * The "default" position as given in the getTexturedModelData method is inaccurate. It lists the default position
 * as 2.5 for the slim skin, and 2.0 for the non-slim skin. However, in BipedEntityModel, the pivot is unconditionally
 * set to exactly 2 in all cases, no matter the skin type.
 *
 * Additionally, the Blockbench preset for the player model has the pivot point as 2 in both skin types, meaning
 * some kind of workaround would be necessary if I wish to support the "Minecraft Skin" preset from Blockbench.
 * I chose this as it was the easiest and keeps Aspect's code the cleanest that I could think of.
 *
 * If anyone else's mod depends on these incorrect default transforms provided by Minecraft, then I apologize.
 */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {

    private static float adjust(float origPivot) {
        assert origPivot == 2.5f; //just checking i did this mixin right
        return 2f;
    }

    @ModifyArg(method = "getTexturedModelData", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelTransform;pivot(FFF)Lnet/minecraft/client/model/ModelTransform;",
            ordinal = 1
    ), index = 1)
    private static float adjustSlimPivot1(float pivot) {
        return adjust(pivot);
    }

    @ModifyArg(method = "getTexturedModelData", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelTransform;pivot(FFF)Lnet/minecraft/client/model/ModelTransform;",
            ordinal = 2
    ), index = 1)
    private static float adjustSlimPivot2(float pivot) {
        return adjust(pivot);
    }

    @ModifyArg(method = "getTexturedModelData", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelTransform;pivot(FFF)Lnet/minecraft/client/model/ModelTransform;",
            ordinal = 3
    ), index = 1)
    private static float adjustSlimPivot3(float pivot) {
        return adjust(pivot);
    }

    @ModifyArg(method = "getTexturedModelData", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelTransform;pivot(FFF)Lnet/minecraft/client/model/ModelTransform;",
            ordinal = 4
    ), index = 1)
    private static float adjustSlimPivot4(float pivot) {
        return adjust(pivot);
    }
}
