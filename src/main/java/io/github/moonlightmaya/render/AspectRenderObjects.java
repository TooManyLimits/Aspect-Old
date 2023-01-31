package io.github.moonlightmaya.render;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

/**
 * Contains useful instances of Minecraft's rendering classes, adapted for
 * Aspect's needs.
 */
public class AspectRenderObjects {

    /**
     * This should be one int, but Minecraft's stuff is annoyingly hard coded.
     * So we're splitting it into 2 shorts, which we'll recombine into an int in the shader.
     * Since there is no light or overlay in any shader with this element, we give this the UV Index 1, normally reserved for the Overlay UV.
     * As such, we will interact with this element through the overlay attribute on vertex consumers.
     */
    public static final VertexFormatElement PART_INDEX_ELEMENT = new VertexFormatElement(1, VertexFormatElement.ComponentType.SHORT, VertexFormatElement.Type.UV, 2);

    /**
     * Overlay, color, and light are not present, because that information will instead be part of each model part's customization.
     */
    public static final VertexFormat ASPECT_VERTEX_FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
            .put("Position", VertexFormats.POSITION_ELEMENT)
            .put("Texture", VertexFormats.TEXTURE_ELEMENT)
            .put("Normal", VertexFormats.NORMAL_ELEMENT)
            .put("PartIndex", AspectRenderObjects.PART_INDEX_ELEMENT)
            .build());
    public static final int ASPECT_VERTEX_BYTES = ASPECT_VERTEX_FORMAT.getVertexSizeByte();


    /**
     * Custom render layers that utilize the new format, otherwise copying vanilla render layers.
     */
    public static final RenderLayer ASPECT_CUTOUT = null;


}
