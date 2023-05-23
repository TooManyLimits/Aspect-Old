package io.github.moonlightmaya.mixin.world;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    /**
     * Inject just after the call to getEntityVertexConsumers().
     * If we inject at the beginning of the render() method, then
     * everything we draw will just be overridden by the skybox and
     * other things which are rendered. By this point, though, the
     * state is such that entities should be rendered. We render all
     * the Aspect world parts now, including ones that aren't attached
     * to any entities that we can see (unlike Figura)
     */
    @Inject(method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;",
                    shift = At.Shift.AFTER
            ))
    public void renderWorldParts(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        AspectMatrixStack aspectMatrices = new AspectMatrixStack(matrices);
        Vec3d cameraPos = camera.getPos();
        aspectMatrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexConsumerProvider.Immediate vertexConsumers = this.bufferBuilders.getEntityVertexConsumers();
        AspectManager.renderWorld(vertexConsumers, tickDelta, aspectMatrices);
    }

    /**
     * Mix into the call to render (specifically the call
     * done by the WorldRenderer, not any other methods such as iris)
     * and set the current render context accordingly. On future render
     * operations before this one, the render context will be different,
     * since aspect.renderEntity will reset the context to OTHER.
     *
     * This doesn't actually modify the arg, it just looks at it and
     * does some processing.
     */
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"))
    private Entity onRenderEntity(Entity entity) {
        Aspect aspect = AspectManager.getAspect(entity.getUuid());
        if (aspect != null)
            aspect.renderContext = Aspect.RenderContexts.WORLD;
        return entity;
    }

}
