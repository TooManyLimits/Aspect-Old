package io.github.moonlightmaya.vanilla;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moonlightmaya.mixin.render.entity.EntityRenderDispatcherAccessor;
import io.github.moonlightmaya.mixin.render.entity.LivingEntityRendererAccessor;
import io.github.moonlightmaya.mixin.render.vanilla.part.CuboidAccessor;
import io.github.moonlightmaya.mixin.render.vanilla.part.ModelPartAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Used to convert vanilla model objects into a
 * bbmodel file and export it to the file system
 */
public class BBModelExporter {

    /**
     * The full BBModel json
     */
    private final JsonObject bbmodel = new JsonObject();
    private final String modelName;
    private int width = -1, height = -1;

    public BBModelExporter(String modelName) {
        this.modelName = modelName;
        initialize();
    }

    /**
     * Create the initial, basic bbmodel top level fields
     */
    private void initialize() {
        //Set up the initial BBModel values

        //Create the "meta" object
        JsonObject meta = new JsonObject();
        //Version? Unsure what to put here, just copied from the bbmodel file i had
        meta.addProperty("format_version", "4.5");
        //Generic model mode
        meta.addProperty("model_format", "free");
        //Vanilla textures use box uv
        meta.addProperty("box_uv", true);
        //Add meta
        bbmodel.add("meta", meta);

        //Add other fields
        bbmodel.addProperty("name", modelName);

        //Theres a "visible_box" thing? No idea what that's for
        //Same with variable_placeholders, variable_placeholder_buttons, timeline_setups, and unhandled_root_fields

        //Resolution: Set to 0,0 for now, but will change later
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", 0);
        resolution.addProperty("height", 0);
        bbmodel.add("resolution", resolution);

        //Arrays
        bbmodel.add("elements", new JsonArray());
        bbmodel.add("outliner", new JsonArray());
        bbmodel.add("textures", new JsonArray());
        bbmodel.add("animations", new JsonArray());
    }

    public void save(Path destination) throws IOException {
        File f = destination.toFile();
        f.createNewFile();
        try(FileWriter writer = new FileWriter(f)) {
            String bbmodelStr = new Gson().toJson(this.bbmodel);
            writer.write(bbmodelStr);
        }
    }

    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
        bbmodel.getAsJsonObject("resolution").addProperty("width", width);
        bbmodel.getAsJsonObject("resolution").addProperty("height", height);
    }

    private static JsonArray arr(float... vals) {
        JsonArray res = new JsonArray();
        for (float val : vals)
            res.add(val);
        return res;
    }

    private UUID addCube(String name, JsonObject parent, ModelPart.Cuboid cuboid) {
        JsonObject part = new JsonObject();

        //Simple properties
        part.addProperty("name", name);
        part.addProperty("box_uv", true);
        part.addProperty("type", "cube");
        part.add("origin", arr(0, 0, 0));

        //Iterate the faces of the cuboid
        JsonObject faces = new JsonObject();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (ModelPart.Quad quad : ((CuboidAccessor) (Object) cuboid).getSides()) {
            float u1 = quad.vertices[1].u * width;
            float u2 = quad.vertices[0].u * width;
            float v1 = quad.vertices[0].v * height;
            float v2 = quad.vertices[2].v * height;
            for (ModelPart.Vertex vert : quad.vertices) {
                //Check max and min pos values
                if (vert.pos.x < minX) minX = vert.pos.x;
                if (vert.pos.y < minY) minY = vert.pos.y;
                if (vert.pos.z < minZ) minZ = vert.pos.z;
                if (vert.pos.x > maxX) maxX = vert.pos.x;
                if (vert.pos.y > maxY) maxY = vert.pos.y;
                if (vert.pos.z > maxZ) maxZ = vert.pos.z;
            }

            Direction dir = Direction.getFacing(quad.direction.x, quad.direction.y, quad.direction.z).getOpposite();
            String key = dir.name().toLowerCase();
            JsonObject face = new JsonObject();
            face.add("uv", arr(u1, v1, u2, v2));
            face.addProperty("texture", -1);
            faces.add(key, face);
        }

        minX = parent.getAsJsonArray("origin").get(0).getAsFloat() - minX;
        minY = parent.getAsJsonArray("origin").get(1).getAsFloat() - minY;
        minZ = parent.getAsJsonArray("origin").get(2).getAsFloat() + minZ;
        maxX = parent.getAsJsonArray("origin").get(0).getAsFloat() - maxX;
        maxY = parent.getAsJsonArray("origin").get(1).getAsFloat() - maxY;
        maxZ = parent.getAsJsonArray("origin").get(2).getAsFloat() + maxZ;

        //swap min and max x and y
        float temp = minX; minX = maxX; maxX = temp;
        temp = minY; minY = maxY; maxY = temp;

        part.add("from", arr(minX, minY, minZ));
        part.add("to", arr(maxX, maxY, maxZ));

        UUID uuid = UUID.randomUUID();
        part.addProperty("uuid", uuid.toString());

        bbmodel.getAsJsonArray("elements").add(part);
        return uuid;
    }

    private JsonObject getModelPart(String name, JsonObject parent, ModelPart modelPart) {
        //Create basic values
        JsonObject part = new JsonObject();
        part.addProperty("name", name);
        if (parent == null)
            part.add("origin", arr(
                    -modelPart.getDefaultTransform().pivotX,
                    24 - modelPart.getDefaultTransform().pivotY,
                    modelPart.getDefaultTransform().pivotZ
            ));
        else
            part.add("origin", arr(
                    parent.getAsJsonArray("origin").get(0).getAsFloat() - modelPart.getDefaultTransform().pivotX,
                    parent.getAsJsonArray("origin").get(1).getAsFloat() - modelPart.getDefaultTransform().pivotY,
                    parent.getAsJsonArray("origin").get(2).getAsFloat() + modelPart.getDefaultTransform().pivotZ
            ));
        part.add("rotation", arr(
                -modelPart.getDefaultTransform().pitch * 180 / MathHelper.PI,
                modelPart.getDefaultTransform().yaw * 180 / MathHelper.PI,
                modelPart.getDefaultTransform().roll * 180 / MathHelper.PI
        ));
        part.addProperty("visibility", true);

        //Create children list:
        JsonArray children = new JsonArray();
        //Get the cuboids and add them all:
        List<ModelPart.Cuboid> cuboids = ((ModelPartAccessor) (Object) modelPart).getCuboids();
        int i = 0;
        for (ModelPart.Cuboid cuboid : cuboids) {
            UUID uuid = addCube("cube" + ++i, part, cuboid);
            children.add(uuid.toString());
        }
        //Add the actual children
        for (Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) modelPart).getChildren().entrySet()) {
            String childName = child.getKey();
            ModelPart childPart = child.getValue();
            JsonObject childJson = getModelPart(childName, part, childPart);
            children.add(childJson);
        }

        //Add children list
        part.add("children", children);
        return part;
    }

    public static BBModelExporter entity(EntityType<?> entityType) {
        String fileName = "aspect_generated_" + Registries.ENTITY_TYPE.getId(entityType).getPath();
        //Get the entity renderer for the type
        EntityRenderer<?> renderer = ((EntityRenderDispatcherAccessor) (Object) MinecraftClient.getInstance().getEntityRenderDispatcher()).getRenderers().get(entityType);
        return fromEntityRenderer(fileName, renderer);
    }

    //Handling slim vs default
    public static BBModelExporter player(boolean slim) {
        String str = slim ? "slim" : "default";
        EntityRenderer<? extends PlayerEntity> renderer = ((EntityRenderDispatcherAccessor) (Object) MinecraftClient.getInstance().getEntityRenderDispatcher()).getModelRenderers().get(str);
        return fromEntityRenderer("aspect_generated_player_" + str, renderer);
    }

    public static BBModelExporter fromEntityRenderer(String fileName, EntityRenderer<?> renderer) {
        BBModelExporter exporter = new BBModelExporter(fileName);
        //Get all the roots
        List<ModelPart> roots = EntityRendererMaps.getEntityRoots(renderer);
        int i = 0;
        for (ModelPart root : roots) {
            String name = roots.size() == 1 ? "root" : ("root" + (i++));
            JsonObject part = exporter.getModelPart(name, null, root);
            exporter.bbmodel.getAsJsonArray("outliner").add(part);
        }

        //Get feature renderers
        if (renderer instanceof LivingEntityRenderer<?,?>) {
            List<FeatureRenderer<?, ?>> featureRenderers = ((LivingEntityRendererAccessor) renderer).getFeatures();
            int j = 0;
            for (FeatureRenderer<?, ?> featureRenderer : featureRenderers) {
                List<ModelPart> featureRoots = EntityRendererMaps.getFeatureRoots(featureRenderer);

                String name = "feature" + (j++);
                int k = 0;
                if (featureRoots.size() == 1) {
                    JsonObject part = exporter.getModelPart(name, null, featureRoots.get(0));
                    exporter.bbmodel.getAsJsonArray("outliner").add(part);
                } else {
                    //Get the many feature roots as children
                    JsonArray children = new JsonArray();
                    for (ModelPart featureRoot : featureRoots)
                        children.add(exporter.getModelPart("root" + (k++), null, featureRoot));

                    //Create feature part
                    JsonObject featurePart = new JsonObject();
                    featurePart.addProperty("name", name);
                    featurePart.add("origin", arr(0, 24, 0));
                    featurePart.addProperty("visibility", true);
                    featurePart.add("children", children);
                    exporter.bbmodel.getAsJsonArray("outliner").add(featurePart);
                }
            }
        }
        return exporter;
    }

}