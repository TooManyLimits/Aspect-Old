package io.github.moonlightmaya.util.compat;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;

/**
 * Sodium doesn't like our custom VCP from RenderUtils,
 * so we make a workaround
 */
public class SodiumCompat {

    public static class SillyLittleSodiumVertexConsumer implements VertexConsumer, VertexBufferWriter {
        public static SillyLittleSodiumVertexConsumer instance = new SillyLittleSodiumVertexConsumer();
        @Override
        public void push(MemoryStack memoryStack, long l, int i, VertexFormatDescription vertexFormatDescription) {
            //Do nothing? I think?
        }
        @Override
        public VertexConsumer vertex(double x, double y, double z) {return this;}
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {return this;}
        @Override
        public VertexConsumer texture(float u, float v) {return this;}
        @Override
        public VertexConsumer overlay(int u, int v) {return this;}
        @Override
        public VertexConsumer light(int u, int v) {return this;}
        @Override
        public VertexConsumer normal(float x, float y, float z) {return this;}
        @Override
        public void next() {}
        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {}
        @Override
        public void unfixColor() {}
    }


}
