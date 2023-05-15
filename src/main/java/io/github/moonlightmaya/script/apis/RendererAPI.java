package io.github.moonlightmaya.script.apis;

import io.github.moonlightmaya.model.renderlayers.NewRenderLayerFunction;
import io.github.moonlightmaya.texture.AspectTexture;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetTable;

import java.util.HashMap;
import java.util.function.Function;

/**
 * One global instance of this API. Designed to interface with the
 * renderer in certain ways, such as dealing with render layers
 * and (eventually) shaders.
 */
@PetPetWhitelist
public class RendererAPI {

    @PetPetWhitelist
    public RenderLayer newRenderLayer(PetPetTable<String, Object> params) {
        //Logic for this function separated out into its own class, as there's a lot of it there.
        return NewRenderLayerFunction.renderLayerFunction(params);
    }

    private static final HashMap<String, RenderLayer> zeroArgRenderLayers = new HashMap<>();
    static {
        zeroArgRenderLayers.put("END_PORTAL", RenderLayer.getEndPortal());
        zeroArgRenderLayers.put("END_GATEWAY", RenderLayer.getEndGateway());
        zeroArgRenderLayers.put("GLINT", RenderLayer.getDirectEntityGlint());
        zeroArgRenderLayers.put("GLINT2", RenderLayer.getDirectGlint());
        zeroArgRenderLayers.put("FLINT", RenderUtils.getDirectEntityFlint());
        zeroArgRenderLayers.put("LINES", RenderLayer.getLines());
        zeroArgRenderLayers.put("LINES_STRIP", RenderLayer.getLineStrip());
    }
    private static final HashMap<String, Function<Identifier, RenderLayer>> oneArgRenderLayers = new HashMap<>();
    static {
        oneArgRenderLayers.put("CUTOUT", RenderLayer::getEntityCutoutNoCull);
        oneArgRenderLayers.put("CUTOUT_CULL", RenderLayer::getEntityCutout);
        oneArgRenderLayers.put("TRANSLUCENT", RenderLayer::getEntityTranslucent);
        oneArgRenderLayers.put("TRANSLUCENT_CULL", RenderLayer::getEntityTranslucentCull);

        oneArgRenderLayers.put("EMISSIVE", RenderLayer::getEyes);
        oneArgRenderLayers.put("EYES", RenderLayer::getEyes);
        oneArgRenderLayers.put("EMISSIVE_SOLID", i -> RenderLayer.getBeaconBeam(i, false));
    }

    //Zero arg render layers
    @PetPetWhitelist
    public RenderLayer getRenderLayer_1(String name) {
        // Not every single render layer is here, only the ones particularly relevant to aspect
        RenderLayer x = zeroArgRenderLayers.get(name);
        if (x == null) {
            //Error, either expected an arg, or name not found
            //Make a nice error message for them :)
            if (oneArgRenderLayers.containsKey(name))
                throw new PetPetException("Render layer " + name + " requires a texture arg");
            else
                throw new PetPetException("Unrecognized render layer name \"" + name + "\"");
        }
        return x;
    }

    //One arg render layers
    @PetPetWhitelist
    public RenderLayer getRenderLayer_2(String name, Object param) {
        Function<Identifier, RenderLayer> x = oneArgRenderLayers.get(name);
        if (x == null) {
            //Error, either expected an arg, or name not found
            //Make a nice error message for them :)
            if (zeroArgRenderLayers.containsKey(name))
                throw new PetPetException("Render layer " + name + " does not accept an arg");
            else
                throw new PetPetException("Unrecognized render layer name \"" + name + "\"");
        }
        Identifier id;
        if (param instanceof String s)
            id = new Identifier(s);
        else if (param instanceof AspectTexture t)
            id = t.getIdentifier();
        else
            throw new PetPetException("Param to getRenderLayer must be a string or a texture object!");


        return x.apply(id);
    }



}
