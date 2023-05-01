package io.github.moonlightmaya.model.rendertasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import petpet.external.PetPetWhitelist;

@PetPetWhitelist
public class TextTask extends RenderTask {

    private String text;

    public TextTask(String text) {
        this.text = text;
    }

    @Override
    protected void specializedRender(VertexConsumerProvider vcp, int light, int overlay) {
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        renderer.draw(VANILLA_STACK, text, 0, 0, 0xFFFFFFFF);
    }

    @PetPetWhitelist
    public TextTask text(String text) {
        this.text = text;
        return this;
    }
}
