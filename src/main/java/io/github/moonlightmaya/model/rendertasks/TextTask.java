package io.github.moonlightmaya.model.rendertasks;

import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;

@PetPetWhitelist
public class TextTask extends RenderTask {

    //Save the "json" version so it's accessible from text_0()
    private String jsonText;
    private Text text;
    private int backgroundColor;
    TextRenderer.TextLayerType layerType = TextRenderer.TextLayerType.NORMAL;
    private boolean shadow;




    public TextTask(String text) {
        backgroundColor = 0;
        this.jsonText = text;
        this.text = DisplayUtils.tryParseJsonText(text);
    }

    @Override
    protected void specializedRender(VertexConsumerProvider vcp, int light, int overlay) {
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        VANILLA_STACK.peek().getPositionMatrix().scale(-1, -1, 1);
        renderer.draw(text, 0, 0, 0xFFFFFFFF, shadow, VANILLA_STACK.peek().getPositionMatrix(), vcp, layerType, backgroundColor, light);
    }

    @PetPetWhitelist
    public String text_0() {
        return jsonText;
    }
    @PetPetWhitelist
    public TextTask text_1(String text) {
        this.jsonText = text;
        this.text = DisplayUtils.tryParseJsonText(text);
        return this;
    }
    @PetPetWhitelist
    public double width() {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }
    @PetPetWhitelist
    public Vector4d backgroundColor_0() {
        return MathUtils.intToRGBA(backgroundColor);
    }
    @PetPetWhitelist
    public TextTask backgroundColor_1(Vector4d backgroundColor) {
        this.backgroundColor = MathUtils.RGBAToInt(backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
        return this;
    }
    @PetPetWhitelist
    public TextTask backgroundColor_4(double r, double g, double b, double a) {
        this.backgroundColor = MathUtils.RGBAToInt(r, g, b, a);
        return this;
    }
    @PetPetWhitelist
    public String layerType_0() {
        return String.valueOf(this.layerType);
    }
    @PetPetWhitelist
    public TextTask layerType_1(String s) {
        try {
            this.layerType = TextRenderer.TextLayerType.valueOf(s);
            return this;
        } catch (IllegalArgumentException e) {
            throw new PetPetException("Unrecognized text layer type \"" + s + "\"");
        }
    }
    @PetPetWhitelist
    public boolean shadow_0() {
        return this.shadow;
    }
    @PetPetWhitelist
    public TextTask shadow_1(boolean b) {
        this.shadow = b;
        return this;
    }


}
