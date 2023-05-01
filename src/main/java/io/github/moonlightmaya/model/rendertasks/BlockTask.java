package io.github.moonlightmaya.model.rendertasks;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public class BlockTask extends RenderTask {

    private BlockState state;

    public BlockTask(BlockState state) {
        this.state = state;
    }

    @Override
    protected void specializedRender(VertexConsumerProvider vcp, int light, int overlay) {
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(state, VANILLA_STACK, vcp, light, overlay);
    }

    @PetPetWhitelist
    public BlockTask block(BlockState state) {
        this.state = state;
        return this;
    }

}
