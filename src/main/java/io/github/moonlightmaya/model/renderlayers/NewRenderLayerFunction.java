package io.github.moonlightmaya.model.renderlayers;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.moonlightmaya.texture.AspectTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Script-accessible function which is used to create render layer objects
 * Arguments are passed using a petpet table $[]
 *
 * Extend RenderLayer for that sweet, sweet protected member access
 *
 * It's in its own entire class because this one function needs *a lot* of logic,
 * so it's separated out
 */
public class NewRenderLayerFunction extends RenderLayer {

    private NewRenderLayerFunction(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static final JavaFunction JAVA_FUNCTION = new JavaFunction(NewRenderLayerFunction.class, "renderLayerFunction", false);

    public static RenderLayer renderLayerFunction(PetPetTable<String, Object> params) {
        //Parse params
        ShaderProgram shader = SHADER_PROGRAMS.get(params.getOrDefault("shader", ""));
        if (shader == null)
            throw new PetPetException("Unrecognized shader: " + params.get("shader"));

        Transparency transparency = TRANSPARENCIES.get(params.getOrDefault("transparency", "NONE"));
        if (transparency == null)
            throw new PetPetException("Unrecognized transparency: " + params.get("transparency"));

        DepthTest depthTest = DEPTH_TESTS.get(params.getOrDefault("depthTest", "<="));
        if (depthTest == null)
            throw new PetPetException("Unrecognized depth test: " + params.get("depthTest"));

        Layering layering = LAYERINGS.get(params.getOrDefault("layering", "NONE"));
        if (layering == null)
            throw new PetPetException("Unrecognized layering: " + params.get("layering"));

        Texturing texturing = TEXTURINGS.get(params.getOrDefault("texturing", "DEFAULT"));
        if (texturing == null)
            throw new PetPetException("Unrecognized texturing: " + params.get("texturing"));

        // Default is 1.0
        // if key is present but explicitly set to null, then it will set to optional.empty
        OptionalDouble lineWidth = OptionalDouble.of(1.0);
        if (params.containsKey("lineWidth")) {
            Object o = params.get("lineWidth");
            if (o == null)
                lineWidth = OptionalDouble.empty();
            else if (o instanceof Double d)
                lineWidth = OptionalDouble.of(d);
            else
                throw new PetPetException("lineWidth param must be a number or null");
        }

        //Parse textures
        Textures textures;
        if (params.containsKey("textures")) {
            Object o = params.get("textures");
            if (o == null) {
                textures = Textures.create().build();
            } else if (o instanceof PetPetList<?> list) {
                Textures.Builder builder = Textures.create();
                for (Object listElement : list) {
                    if (listElement instanceof PetPetList<?> innerList && innerList.size() == 3) {
                        if (
                                (innerList.get(0) instanceof String || innerList.get(0) instanceof AspectTexture) &&
                                innerList.get(1) instanceof Boolean blur &&
                                innerList.get(2) instanceof Boolean mipmap
                        ) {
                            if (innerList.get(0) instanceof String s) {
                                builder.add(new Identifier(s), blur, mipmap);
                            } else {
                                builder.add(((AspectTexture) innerList.get(0)).getIdentifier(), blur, mipmap);
                            }
                        } else {
                            throw new PetPetException("Nested lists in \"textures\" param list must contain (String or texture), bool, bool");
                        }
                    } else if (listElement instanceof String s) {
                        builder.add(new Identifier(s), false, false);
                    } else if (listElement instanceof AspectTexture tex) {
                        builder.add(tex.getIdentifier(), false, false);
                    } else {
                        throw new PetPetException("Elements of \"textures\" param list must be an aspect texture, string, or list of length 3");
                    }
                }
                textures = builder.build();
            } else {
                throw new PetPetException("textures param must be a list or null");
            }
        } else {
            textures = Textures.create().build();
        }

        boolean hasCrumbling = getBoolean(params, "hasCrumbling", false);
        boolean translucent = getBoolean(params, "translucent", false);

        boolean affectsOutline = getBoolean(params, "affectsOutline", true);
        boolean cull = getBoolean(params, "cull", true);
        boolean lightmap = getBoolean(params, "lightmap", false);
        boolean overlay = getBoolean(params, "overlay", false);
        boolean writeColor = getBoolean(params, "writeColor", true);
        boolean writeDepth = getBoolean(params, "writeDepth", true);

        MultiPhaseParameters multiPhaseParams = MultiPhaseParameters.builder()
                .program(shader)
                .texture(textures)
                .transparency(transparency)
                .depthTest(depthTest)
                .cull(cull ? ENABLE_CULLING : DISABLE_CULLING)
                .lightmap(lightmap ? ENABLE_LIGHTMAP : DISABLE_LIGHTMAP)
                .overlay(overlay ? ENABLE_OVERLAY_COLOR : DISABLE_OVERLAY_COLOR)
                .layering(layering)
                .texturing(texturing)
                .writeMaskState(new WriteMaskState(writeColor, writeDepth))
                .lineWidth(new LineWidth(lineWidth))
                .build(affectsOutline);

        return RenderLayer.of(
                "aspect_generated_renderlayer_" + UUID.randomUUID(),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                hasCrumbling,
                translucent,
                multiPhaseParams
        );
    }

    private static boolean getBoolean(PetPetTable<String, Object> params, String paramName, boolean defaultValue) {
        boolean bool;
        if (params.getOrDefault(paramName, defaultValue) instanceof Boolean b)
            bool = b;
        else throw new PetPetException("\"" + paramName + "\" parameter must be a boolean");
        return bool;
    }

    private static final Map<String, ShaderProgram> SHADER_PROGRAMS = new HashMap<>();
    private static final Map<String, Transparency> TRANSPARENCIES = new HashMap<>();
    private static final Map<String, RenderPhase.DepthTest> DEPTH_TESTS = new HashMap<>();
    private static final Map<String, Layering> LAYERINGS = new HashMap<>();
    private static final Map<String, Texturing> TEXTURINGS = new HashMap<>();



    static {
        //Conditional, this is just to generate the code for the maps
        if (false) {
            codegen();
        }

        //Depth tests
        DEPTH_TESTS.put("ALWAYS", new DepthTest("always", GL32.GL_ALWAYS));
        DEPTH_TESTS.put("NEVER", new DepthTest("never", GL32.GL_NEVER));
        DEPTH_TESTS.put("==", new DepthTest("==", GL32.GL_EQUAL));
        DEPTH_TESTS.put("!=", new DepthTest("!=", GL32.GL_NOTEQUAL));
        DEPTH_TESTS.put("<=", new DepthTest("<=", GL32.GL_LEQUAL));
        DEPTH_TESTS.put(">=", new DepthTest(">=", GL32.GL_GEQUAL));
        DEPTH_TESTS.put("<", new DepthTest("<", GL32.GL_LESS));
        DEPTH_TESTS.put(">", new DepthTest(">", GL32.GL_GREATER));

        //Generated the other maps through codegen, and slightly cleaned up
        //Cannot generate at runtime because of obfuscation
        SHADER_PROGRAMS.put("BLOCK", BLOCK_PROGRAM);
        SHADER_PROGRAMS.put("NEW_ENTITY", NEW_ENTITY_PROGRAM);
        SHADER_PROGRAMS.put("POSITION_COLOR_LIGHTMAP", POSITION_COLOR_LIGHTMAP_PROGRAM);
        SHADER_PROGRAMS.put("POSITION", POSITION_PROGRAM);
        SHADER_PROGRAMS.put("POSITION_COLOR_TEXTURE", POSITION_COLOR_TEXTURE_PROGRAM);
        SHADER_PROGRAMS.put("POSITION_TEXTURE", POSITION_TEXTURE_PROGRAM);
        SHADER_PROGRAMS.put("POSITION_COLOR_TEXTURE_LIGHTMAP", POSITION_COLOR_TEXTURE_LIGHTMAP_PROGRAM);
        SHADER_PROGRAMS.put("COLOR", COLOR_PROGRAM);
        SHADER_PROGRAMS.put("SOLID", SOLID_PROGRAM);
        SHADER_PROGRAMS.put("CUTOUT_MIPPED", CUTOUT_MIPPED_PROGRAM);
        SHADER_PROGRAMS.put("CUTOUT", CUTOUT_PROGRAM);
        SHADER_PROGRAMS.put("TRANSLUCENT", TRANSLUCENT_PROGRAM);
        SHADER_PROGRAMS.put("TRANSLUCENT_MOVING_BLOCK", TRANSLUCENT_MOVING_BLOCK_PROGRAM);
        SHADER_PROGRAMS.put("TRANSLUCENT_NO_CRUMBLING", TRANSLUCENT_NO_CRUMBLING_PROGRAM);
        SHADER_PROGRAMS.put("ARMOR_CUTOUT_NO_CULL", ARMOR_CUTOUT_NO_CULL_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_SOLID", ENTITY_SOLID_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_CUTOUT", ENTITY_CUTOUT_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_CUTOUT_NO_CULL", ENTITY_CUTOUT_NONULL_PROGRAM); //Yarn moment, "nonull"
        SHADER_PROGRAMS.put("ENTITY_CUTOUT_NO_CULL_OFFSET_Z", ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM); //Yarn moment
        SHADER_PROGRAMS.put("ITEM_ENTITY_TRANSLUCENT_CULL", ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_TRANSLUCENT_CULL", ENTITY_TRANSLUCENT_CULL_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_TRANSLUCENT", ENTITY_TRANSLUCENT_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_TRANSLUCENT_EMISSIVE", ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_SMOOTH_CUTOUT", ENTITY_SMOOTH_CUTOUT_PROGRAM);
        SHADER_PROGRAMS.put("BEACON_BEAM", BEACON_BEAM_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_DECAL", ENTITY_DECAL_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_NO_OUTLINE", ENTITY_NO_OUTLINE_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_SHADOW", ENTITY_SHADOW_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_ALPHA", ENTITY_ALPHA_PROGRAM);
        SHADER_PROGRAMS.put("EYES", EYES_PROGRAM);
        SHADER_PROGRAMS.put("ENERGY_SWIRL", ENERGY_SWIRL_PROGRAM);
        SHADER_PROGRAMS.put("LEASH", LEASH_PROGRAM);
        SHADER_PROGRAMS.put("WATER_MASK", WATER_MASK_PROGRAM);
        SHADER_PROGRAMS.put("OUTLINE", OUTLINE_PROGRAM);
        SHADER_PROGRAMS.put("ARMOR_GLINT", ARMOR_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("ARMOR_ENTITY_GLINT", ARMOR_ENTITY_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("TRANSLUCENT_GLINT", TRANSLUCENT_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("GLINT", GLINT_PROGRAM);
        SHADER_PROGRAMS.put("DIRECT_GLINT", DIRECT_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("ENTITY_GLINT", ENTITY_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("DIRECT_ENTITY_GLINT", DIRECT_ENTITY_GLINT_PROGRAM);
        SHADER_PROGRAMS.put("CRUMBLING", CRUMBLING_PROGRAM);
        SHADER_PROGRAMS.put("TEXT", TEXT_PROGRAM);
        SHADER_PROGRAMS.put("TEXT_BACKGROUND", TEXT_BACKGROUND_PROGRAM);
        SHADER_PROGRAMS.put("TEXT_INTENSITY", TEXT_INTENSITY_PROGRAM);
        SHADER_PROGRAMS.put("TRANSPARENT_TEXT", TRANSPARENT_TEXT_PROGRAM);
        SHADER_PROGRAMS.put("TRANSPARENT_TEXT_BACKGROUND", TRANSPARENT_TEXT_BACKGROUND_PROGRAM);
        SHADER_PROGRAMS.put("TRANSPARENT_TEXT_INTENSITY", TRANSPARENT_TEXT_INTENSITY_PROGRAM);
        SHADER_PROGRAMS.put("LIGHTNING", LIGHTNING_PROGRAM);
        SHADER_PROGRAMS.put("TRIPWIRE", TRIPWIRE_PROGRAM);
        SHADER_PROGRAMS.put("END_PORTAL", END_PORTAL_PROGRAM);
        SHADER_PROGRAMS.put("END_GATEWAY", END_GATEWAY_PROGRAM);
        SHADER_PROGRAMS.put("LINES", LINES_PROGRAM);

        TRANSPARENCIES.put("NONE", NO_TRANSPARENCY);
        TRANSPARENCIES.put("ADDITIVE", ADDITIVE_TRANSPARENCY);
        TRANSPARENCIES.put("LIGHTNING", LIGHTNING_TRANSPARENCY);
        TRANSPARENCIES.put("GLINT", GLINT_TRANSPARENCY);
        TRANSPARENCIES.put("CRUMBLING", CRUMBLING_TRANSPARENCY);
        TRANSPARENCIES.put("TRANSLUCENT", TRANSLUCENT_TRANSPARENCY);

        LAYERINGS.put("NONE", NO_LAYERING);
        LAYERINGS.put("POLYGON_OFFSET", POLYGON_OFFSET_LAYERING);
        LAYERINGS.put("VIEW_OFFSET_Z", VIEW_OFFSET_Z_LAYERING);

        TEXTURINGS.put("DEFAULT", DEFAULT_TEXTURING);
        TEXTURINGS.put("GLINT", GLINT_TEXTURING);
        TEXTURINGS.put("ENTITY_GLINT", ENTITY_GLINT_TEXTURING);
    }


    //Generates and prints to stdout the massive maps up above... so i don't have to do it manually
    //The output is slightly bad, so we do a little manual cleanup after for some things
    //Changing "NO" to "NONE", also correcting yarn's "no null" typo
    private static void codegen() {
        //Shaders
        StringBuilder builder = new StringBuilder();
        try {
            for (Field f : RenderPhase.class.getDeclaredFields())
                if (f.getType() == ShaderProgram.class)
                    //Remove redundant word "PROGRAM" at the end of them all
                    builder.append("SHADER_PROGRAMS.put(\"%s\", %s);\n".formatted(
                            f.getName().substring(0, f.getName().lastIndexOf('_')),
                            f.getName()
                    ));
        } catch (IllegalArgumentException shouldBeImpossible) {
            throw new IllegalStateException("Aspect failed to read shader programs");
        }

        //Transparency types
        try {
            for (Field f : RenderPhase.class.getDeclaredFields())
                if (f.getType() == Transparency.class)
                    //Remove the redundant word "TRANSPARENCY" at the end of them all
                    builder.append("TRANSPARENCIES.put(\"%s\", %s);\n".formatted(
                            f.getName().substring(0, f.getName().lastIndexOf('_')),
                            f.getName()
                    ));            //Rename "no" to "none";
        } catch (IllegalArgumentException shouldBeImpossible) {
            throw new IllegalStateException("Aspect failed to read transparencies");
        }

        //Layerings
        try {
            for (Field f : RenderPhase.class.getDeclaredFields())
                if (f.getType() == Layering.class)
                    //Remove the redundant word "LAYERING" at the end of them all
                    builder.append("LAYERINGS.put(\"%s\", %s);\n".formatted(
                            f.getName().substring(0, f.getName().lastIndexOf('_')),
                            f.getName()
                    ));
            //Rename "no" to "none"
            LAYERINGS.put("NONE", LAYERINGS.get("NO"));
            LAYERINGS.remove("NO");
        } catch (IllegalArgumentException shouldBeImpossible) {
            throw new IllegalStateException("Aspect failed to read layerings");
        }

        //Texturings
        try {
            for (Field f : RenderPhase.class.getDeclaredFields())
                if (f.getType() == Texturing.class)
                    //Remove the redundant word "TEXTURING" at the end of them all
                    builder.append("TEXTURINGS.put(\"%s\", %s);\n".formatted(
                            f.getName().substring(0, f.getName().lastIndexOf('_')),
                            f.getName()
                    ));
        } catch (IllegalArgumentException shouldBeImpossible) {
            throw new IllegalStateException("Aspect failed to read texturings");
        }

        System.out.println(builder);
    }

    //shh
    private static void setupFlintTexturing(float scale) {long l = (long)((double) Util.getMeasuringTimeMs() * MinecraftClient.getInstance().options.getGlintSpeed().getValue() * 8.0);float f = (float)(l % 110000L) / 110000.0f;float g = (float)(l % 30000L) / 30000.0f;Matrix4f matrix4f = new Matrix4f().translation(-f, g, 0.0f);matrix4f.rotateZ(0.17453292f).scale(scale);RenderSystem.setTextureMatrix(matrix4f);}
    public static final RenderLayer flint = RenderLayer.of("flint", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, RenderLayer.MultiPhaseParameters.builder().program(DIRECT_GLINT_PROGRAM).texture(new RenderPhase.Texture(new Identifier("textures/item/flint.png"), true, false)).writeMaskState(COLOR_MASK).cull(DISABLE_CULLING).depthTest(EQUAL_DEPTH_TEST).transparency(CRUMBLING_TRANSPARENCY).texturing(new Texturing("flint_texturing", () -> setupFlintTexturing(30.0f), RenderSystem::resetTextureMatrix)).build(false));
}
