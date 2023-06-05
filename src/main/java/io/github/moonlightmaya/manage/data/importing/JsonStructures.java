package io.github.moonlightmaya.manage.data.importing;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.animation.Animation;
import io.github.moonlightmaya.model.animation.Interpolation;
import io.github.moonlightmaya.util.ColorUtils;
import io.github.moonlightmaya.util.DataStructureUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import petpet.types.PetPetList;

import java.lang.reflect.Type;
import java.util.*;

public class JsonStructures {

    private static final Gson VECTOR_GSON = new GsonBuilder()
            .registerTypeAdapter(Vector2f.class, Vector2fDeserializer.INSTANCE)
            .registerTypeAdapter(Vector3f.class, Vector3fDeserializer.INSTANCE)
            .registerTypeAdapter(Vector4f.class, Vector4fDeserializer.INSTANCE)
            .create();

    public static class Metadata {
        public String name;
        public String version;
        public String color;
        public String[] authors;

        public BaseStructures.MetadataStructure toBaseStructure() throws AspectImporter.AspectImporterException {
            try {
                if (name == null) name = "";
                if (version == null) version = "";
                Vector3f parsedColor = ColorUtils.parseColor(color);
                List<String> authorsList = new PetPetList<>();
                if (authors != null)
                    authorsList.addAll(Arrays.asList(authors));
                return new BaseStructures.MetadataStructure(name, version, parsedColor, authorsList);
            } catch (IllegalArgumentException e) {
                throw new AspectImporter.AspectImporterException(e.getMessage());
            }
        }
    }

    public static class BBModel {
        public Resolution resolution;
        public Part[] elements;
        public JsonArray outliner;
        public Part[] fixedOutliner;
        public Texture[] textures;
        public JsonAnimation[] animations;

        /**
         * After the initial GSON parse, some data may not be in the right format.
         * This will resolve that.
         *
         */
        public void fix() {
            if (resolution == null) resolution = new Resolution(16, 16);
            if (elements == null) elements = new Part[0];
            if (outliner == null) outliner = new JsonArray();
            fixedOutliner = getGson().fromJson(outliner, Part[].class);
            if (textures == null) textures = new Texture[0];
            if (animations == null) animations = new JsonAnimation[0];
        }

        /**
         * A custom gson is necessary because we need the outliner part deserializer.
         * This is because the Blockbench "outliner" array contains both group model parts
         * and UUIDs of parts defined in the "elements" array previously. Using the custom
         * gson and the custom deserializer, we can simplify the process of deserializing
         * both of these types of part.
         */
        public Gson getGson() {
            return new GsonBuilder()
                    .registerTypeAdapter(Vector2f.class, Vector2fDeserializer.INSTANCE)
                    .registerTypeAdapter(Vector3f.class, Vector3fDeserializer.INSTANCE)
                    .registerTypeAdapter(Vector4f.class, Vector4fDeserializer.INSTANCE)
                    .registerTypeAdapter(Part.class, new OutlinerPartDeserializer(this))
                    .create();
        }
    }

    public record Resolution(int width, int height) {}

    public record Part(String name, float color, Vector3f origin, Vector3f rotation, Boolean visibility, String type,
                       String uuid, Vector3f from, Vector3f to, Double inflate, JsonObject vertices, JsonObject faces, JsonStructures.Part[] children) {
            public BaseStructures.ModelPartStructure toBaseStructure(List<Integer> texMapper, JsonAnimation[] animations, List<Integer> animMapper, Resolution resolution) throws AspectImporter.AspectImporterException {

                //Find animators
                List<BaseStructures.AnimatorStructure> foundAnimators = new ArrayList<>(0);
                for (int i = 0; i < animations.length; i++) {
                    //For each animation, check if it has an animator for me.
                    JsonAnimation curAnimation = animations[i];
                    JsonAnimator thisAnimator = curAnimation.animators.get(this.uuid);
                    //If it does, then create the base structure for the animator, assigning the
                    //index according to the animMapper provided
                    if (thisAnimator != null) {
                        int mappedIndex = animMapper.get(i);
                        foundAnimators.add(thisAnimator.toBaseStructure(mappedIndex));
                    }
                }

                //Mesh! parse it and return it
                if (this.vertices != null) {
                    //Get the vertices and faces as nicer data structures
                    Type vType = new TypeToken<Map<String, Vector3f>>(){}.getType();
                    Map<String, Vector3f> verticesMap = VECTOR_GSON.fromJson(this.vertices, vType);
                    Type fType = new TypeToken<Map<String, MeshFace>>(){}.getType();
                    Map<String, MeshFace> faces = VECTOR_GSON.fromJson(this.faces, fType);
                    //Store the index of each vertex
                    Map<String, Integer> indicesMap = new HashMap<>();

                    //Perform any processing on mesh data TODO: Smooth shading :pain:

                    //Begin collecting and formatting mesh data

                    //The reason for the map is because we may want to split this mesh into
                    //several objects, if different faces of the mesh use different
                    //textures. This keeps things simpler and ensures that a specific model
                    //part does not have multiple sets of vertices for different textures.
                    Map<Integer, BaseStructures.MeshData> meshes = new HashMap<>();

                    //For each face, get the appropriate mesh, and add a face to it
                    int curIndex = 0;
                    for (MeshFace face : faces.values()) {
                        int tex = texMapper.get(face.texture);
                        BaseStructures.MeshData mesh = meshes.computeIfAbsent(tex,
                                texIndex -> new BaseStructures.MeshData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), texIndex)
                        );
                        curIndex = mesh.addFace(face, resolution, verticesMap, indicesMap, curIndex);
                    }

                    if (meshes.size() > 1) {
                        if (children != null)
                            throw new AspectImporter.AspectImporterException("Attempted to split a mesh part with children? Invalid BBModel, notify devs");
                        List<BaseStructures.ModelPartStructure> splitMeshes = new ArrayList<>(meshes.size());
                        int split = 0;
                        for (BaseStructures.MeshData meshData : meshes.values()) {
                            splitMeshes.add(new BaseStructures.ModelPartStructure(
                                    "split" + (split++),
                                    new Vector3f(), new Vector3f(), visibility == null ? true : visibility,
                                    List.of(), AspectModelPart.ModelPartType.MESH, foundAnimators, null, meshData
                            ));
                        }
                        return new BaseStructures.ModelPartStructure(
                                name,
                                rotation == null ? new Vector3f() : rotation,
                                origin == null ? new Vector3f() : origin,
                                visibility == null ? true : visibility,
                                splitMeshes, AspectModelPart.ModelPartType.GROUP,
                                foundAnimators, null, null
                        );
                    } else if (meshes.size() == 1) {
                        BaseStructures.MeshData meshData = meshes.values().iterator().next();
                        return new BaseStructures.ModelPartStructure(
                                name,
                                rotation == null ? new Vector3f() : rotation,
                                origin == null ? new Vector3f() : origin,
                                visibility == null ? true : visibility,
                                List.of(), AspectModelPart.ModelPartType.MESH, foundAnimators, null, meshData
                        );
                    } else {
                        throw new AspectImporter.AspectImporterException("Failed to import mesh, bug, contact devs");
                    }
                }

                //Not a mesh below this point

                List<BaseStructures.CubeFaces> faces = null;

                //Parse inflate
                if (inflate != null && from != null && to != null) {
                    float f = inflate.floatValue();
                    to.add(f,f,f);
                    from.sub(f,f,f);
                }

                //If the part is a cube with multiple textures, split it into multiple cubes
                if (this.faces != null) {
                    CubeFaces jsonFaces = VECTOR_GSON.fromJson(this.faces, CubeFaces.class);
                    faces = jsonFaces.toBaseStructure(texMapper, resolution);
                    int n = faces.size();
                    if (n > 1) {
                        if (children != null) {
                            throw new AspectImporter.AspectImporterException("Attempted to split a cube part with children? Invalid BBModel, notify devs");
                        }
                        List<BaseStructures.ModelPartStructure> splitCubes = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            splitCubes.add(new BaseStructures.ModelPartStructure(
                                "split" + i, new Vector3f(), new Vector3f(), visibility == null ? true : visibility, List.of(),
                                AspectModelPart.ModelPartType.CUBE, foundAnimators, new BaseStructures.CubeData(from, to, faces.get(i)), null
                            ));
                        }
                        return new BaseStructures.ModelPartStructure(
                                name,
                                rotation == null ? new Vector3f() : rotation,
                                origin == null ? new Vector3f() : origin,
                                visibility == null ? true : visibility,
                                splitCubes, AspectModelPart.ModelPartType.GROUP, foundAnimators, null, null
                        );
                    }
                }

                //Otherwise, gather children
                ArrayList<BaseStructures.ModelPartStructure> baseChildren = new ArrayList<>(children == null ? 0 : children.length);
                if (children != null)
                    for (Part p : children)
                        baseChildren.add(p.toBaseStructure(texMapper, animations, animMapper, resolution));

                //Get the bb type. If type is not specified/null, make it a group
                AspectModelPart.ModelPartType bbType = this.type == null ?
                        AspectModelPart.ModelPartType.GROUP :
                        AspectModelPart.ModelPartType.valueOf(type.toUpperCase());

                return new BaseStructures.ModelPartStructure(
                        name,
                        rotation == null ? new Vector3f() : rotation,
                        origin == null ? new Vector3f() : origin,
                        visibility == null ? true : visibility,
                        baseChildren, bbType, foundAnimators,
                        (faces != null && faces.size() == 1) ? new BaseStructures.CubeData(from, to, faces.get(0)) : null,
                        null
                );
            }
        }
    public record CubeFaces(
            CubeFace north,
            CubeFace east,
            CubeFace south,
            CubeFace west,
            CubeFace up,
            CubeFace down
    ) {
        //may also split the part into many
        public List<BaseStructures.CubeFaces> toBaseStructure(List<Integer> texMapper, Resolution resolution) {
            Map<Integer, CubeFacesIntermediate> intermediates = new LinkedHashMap<>();
            faceHelper(resolution, intermediates, north, 0);
            faceHelper(resolution, intermediates, east, 1);
            faceHelper(resolution, intermediates, south, 2);
            faceHelper(resolution, intermediates, west, 3);
            faceHelper(resolution, intermediates, up, 4);
            faceHelper(resolution, intermediates, down, 5);
            ArrayList<BaseStructures.CubeFaces> result = new ArrayList<>(intermediates.size());
            for (Map.Entry<Integer, CubeFacesIntermediate> entry : intermediates.entrySet()) {
                result.add(new BaseStructures.CubeFaces(
                        entry.getValue().present, entry.getValue().faces, texMapper.get(entry.getKey())
                ));
            }
            return result;
        }

        private void faceHelper(Resolution resolution, Map<Integer, CubeFacesIntermediate> intermediates, CubeFace face, int index) {
            if (face != null && face.tex() != -1) {
                if (!intermediates.containsKey(face.tex()))
                    intermediates.put(face.tex(), new CubeFacesIntermediate());
                CubeFacesIntermediate intermediate = intermediates.get(face.tex());
                intermediate.present |= (1 << index);
                intermediate.faces.add(face.toBaseStructure(resolution));
            }
        }

        private static class CubeFacesIntermediate {
            private final List<BaseStructures.CubeFace> faces = new ArrayList<>(6);
            private byte present;
        }
    }

    public record CubeFace(
            float[] uv, //u1 v1 u2 v2
            Integer rotation,
            Integer texture
    ) {
        public BaseStructures.CubeFace toBaseStructure(Resolution resolution) {
            return new BaseStructures.CubeFace(
                    new Vector4f(uv[0]/resolution.width, uv[1]/resolution.height, uv[2]/resolution.width, uv[3]/resolution.height),
                    rotation != null ? (((rotation/90)%4)+4)%4 : 0);
        }
        public int tex() {
            return texture != null ? texture : -1;
        }
    }

    public record MeshFace(
            Map<String, Vector2f> uv,
            String[] vertices,
            int texture
    ) {

    }

    public record Texture(
             String name, String source
    ) {
        public BaseStructures.TextureStructure toBaseStructure(String relativePath) {
            String src = source.replace("data:image/png;base64,", "");
            return new BaseStructures.TextureStructure(relativePath + "/" + strippedName(), Base64.getDecoder().decode(src));
        }

        public String strippedName() {
            return name.endsWith(".png") ? name.substring(0, name.length()-4) : name;
        }
    }

    public record JsonAnimation(
            String name, String loop, boolean override, float length, float snapping, LinkedHashMap<String, JsonAnimator> animators
    ) {
        public BaseStructures.AnimationStructure toBaseStructure() {
            //Base structure doesn't care about the animators like the json does, so we can pretty simply convert
            Animation.LoopMode loopMode = Animation.LoopMode.valueOf(this.loop.toUpperCase());
            return new BaseStructures.AnimationStructure(name, loopMode, override, length, snapping);
        }
    }

    public record JsonAnimator(
            JsonKeyframe[] keyframes //thats it lol
    ) {
        public BaseStructures.AnimatorStructure toBaseStructure(int mappedIndex) throws AspectImporter.AspectImporterException {
            List<BaseStructures.KeyframeStructure> posKeyframes = new ArrayList<>();
            List<BaseStructures.KeyframeStructure> rotKeyframes = new ArrayList<>();
            List<BaseStructures.KeyframeStructure> scaleKeyframes = new ArrayList<>();
            for (JsonKeyframe keyframe : keyframes) {
                BaseStructures.KeyframeStructure baseKeyframe = keyframe.toBaseStructure();
                switch (keyframe.channel) {
                    case "position" -> posKeyframes.add(baseKeyframe);
                    case "rotation" -> rotKeyframes.add(baseKeyframe);
                    case "scale" -> scaleKeyframes.add(baseKeyframe);
                    default -> throw new AspectImporter.AspectImporterException("Unrecognized animation channel \"" + keyframe.channel + "\"");
                }
            }
            Comparator<BaseStructures.KeyframeStructure> sortByTime = Comparator.comparing(BaseStructures.KeyframeStructure::time);
            posKeyframes.sort(sortByTime);
            rotKeyframes.sort(sortByTime);
            scaleKeyframes.sort(sortByTime);
            return new BaseStructures.AnimatorStructure(mappedIndex, posKeyframes, rotKeyframes, scaleKeyframes);
        }
    }

    public record JsonKeyframe(
            //I don't know why data_points is an array and I'm sorry
            String channel, float time, String interpolation, DataPoint[] data_points,
            //Bezier
            Vector3f bezier_left_time, Vector3f bezier_left_value, Vector3f bezier_right_time, Vector3f bezier_right_value
    ) {
        public BaseStructures.KeyframeStructure toBaseStructure() throws AspectImporter.AspectImporterException {
            //Get interpolation, default to linear if unrecognized
            Interpolation.Builtin interpolation = DataStructureUtils.getOrDefault(
                    Interpolation.Builtin::valueOf,
                    (this.interpolation != null ? this.interpolation : "linear").toUpperCase(),
                    Interpolation.Builtin.LINEAR,
                    "Unrecognized interpolation " + this.interpolation + ", defaulting to linear"
            );

            data_points[0].verify(); //check data

            return new BaseStructures.KeyframeStructure(
                    time, interpolation,
                    data_points[0].realX(), data_points[0].realY(), data_points[0].realZ(),
                    bezier_left_time, bezier_left_value, bezier_right_time, bezier_right_value
            );
        }
    }

    public record DataPoint(
            Object x, Object y, Object z //can be either strings or numbers
    ) {
        public void verify() throws AspectImporter.AspectImporterException {
            if (!(x instanceof String) && !(x instanceof Number))
                throw new AspectImporter.AspectImporterException("Failed to parse data point: expected string or number, but got " + (x == null ? "null" : x.getClass()));
            if (!(y instanceof String) && !(y instanceof Number))
                throw new AspectImporter.AspectImporterException("Failed to parse data point: expected string or number, but got " + (y == null ? "null" : y.getClass()));
            if (!(z instanceof String) && !(z instanceof Number))
                throw new AspectImporter.AspectImporterException("Failed to parse data point: expected string or number, but got " + (z == null ? "null" : z.getClass()));
        }

        public Object realX() {
            return checkNumberString(x);
        }
        public Object realY() {
            return checkNumberString(y);
        }
        public Object realZ() {
            return checkNumberString(z);
        }

        /**
         * Sometimes blockbench loves to take ordinary numbers and export them as strings
         * so these functions
         */
        private static Object checkNumberString(Object o) {
            if (o instanceof String s) {
                try {
                    return Float.parseFloat(s);
                } catch (Exception ignored) {}
            }
            return o;
        }
    }


    public static class OutlinerPartDeserializer implements JsonDeserializer<Part> {
        private final Map<String, Part> uuidPartMap = new HashMap<>();

        public OutlinerPartDeserializer(BBModel model) {
            for (Part part : model.elements) {
                uuidPartMap.put(part.uuid, part);
            }
        }

        @Override
        public Part deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            if (element.isJsonPrimitive()) {
                if (!uuidPartMap.containsKey(element.getAsString()))
                    throw new JsonParseException("Invalid bbmodel, found uuid in outliner not present in elements (" + element.getAsString() + ")");
                return uuidPartMap.get(element.getAsString());
            } else {
                JsonObject json = (JsonObject) element;
                return new Part(
                        ctx.deserialize(json.get("name"), String.class),
                        ctx.deserialize(json.get("color"), float.class),
                        ctx.deserialize(json.get("origin"), Vector3f.class),
                        ctx.deserialize(json.get("rotation"), Vector3f.class),
                        ctx.deserialize(json.get("visibility"), Boolean.class),
                        ctx.deserialize(json.get("type"), String.class),
                        ctx.deserialize(json.get("uuid"), String.class),
                        ctx.deserialize(json.get("from"), Vector3f.class),
                        ctx.deserialize(json.get("to"), Vector3f.class),
                        ctx.deserialize(json.get("inflate"), Double.class),
                        ctx.deserialize(json.get("vertices"), JsonObject.class),
                        ctx.deserialize(json.get("faces"), JsonObject.class),
                        ctx.deserialize(json.get("children"), Part[].class)
                );
            }
        }
    }

    public static class Vector2fDeserializer implements JsonDeserializer<Vector2f> {
        public static final Vector2fDeserializer INSTANCE = new Vector2fDeserializer();
        @Override
        public Vector2f deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonArray arr = element.getAsJsonArray();
            return new Vector2f(
                    arr.get(0).getAsFloat(),
                    arr.get(1).getAsFloat()
            );
        }
    }

    public static class Vector3fDeserializer implements JsonDeserializer<Vector3f> {
        public static final Vector3fDeserializer INSTANCE = new Vector3fDeserializer();
        @Override
        public Vector3f deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonArray arr = element.getAsJsonArray();
            return new Vector3f(
                    arr.get(0).getAsFloat(),
                    arr.get(1).getAsFloat(),
                    arr.get(2).getAsFloat()
            );
        }
    }

    public static class Vector4fDeserializer implements JsonDeserializer<Vector4f> {
        public static final Vector4fDeserializer INSTANCE = new Vector4fDeserializer();
        @Override
        public Vector4f deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonArray arr = element.getAsJsonArray();
            return new Vector4f(
                    arr.get(0).getAsFloat(),
                    arr.get(1).getAsFloat(),
                    arr.get(2).getAsFloat(),
                    arr.get(3).getAsFloat()
            );
        }
    }

}
