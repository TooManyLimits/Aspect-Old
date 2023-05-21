package io.github.moonlightmaya.manage.data.importing;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.util.ColorUtils;
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
            public BaseStructures.ModelPartStructure toBaseStructure(List<Integer> texMapper, Resolution resolution) throws AspectImporter.AspectImporterException {

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
                                    "split" + (split++), new Vector3f(), new Vector3f(), new Vector3f(), visibility == null ? true : visibility, List.of(),
                                    AspectModelPart.ModelPartType.MESH, null, meshData
                            ));
                        }
                        return new BaseStructures.ModelPartStructure(
                                name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility == null ? true : visibility,
                                splitMeshes, AspectModelPart.ModelPartType.GROUP, null, null
                        );
                    } else if (meshes.size() == 1) {
                        BaseStructures.MeshData meshData = meshes.values().iterator().next();
                        return new BaseStructures.ModelPartStructure(
                                name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility == null ? true : visibility,
                                List.of(), AspectModelPart.ModelPartType.MESH, null, meshData
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
                                "split" + i, new Vector3f(), new Vector3f(), new Vector3f(), visibility == null ? true : visibility, List.of(),
                                AspectModelPart.ModelPartType.CUBE, new BaseStructures.CubeData(from, to, faces.get(i)), null
                            ));
                        }
                        return new BaseStructures.ModelPartStructure(
                                name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility == null ? true : visibility,
                                splitCubes, AspectModelPart.ModelPartType.GROUP, null, null
                        );
                    }
                }

                //Otherwise, gather children
                ArrayList<BaseStructures.ModelPartStructure> baseChildren = new ArrayList<>(children == null ? 0 : children.length);
                if (children != null)
                    for (Part p : children)
                        baseChildren.add(p.toBaseStructure(texMapper, resolution));

                return new BaseStructures.ModelPartStructure(
                        name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility == null ? true : visibility,
                        baseChildren,
                        type != null ? AspectModelPart.ModelPartType.valueOf(type.toUpperCase()) : AspectModelPart.ModelPartType.GROUP,
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
        public BaseStructures.Texture toBaseStructure(String relativePath) {
            String src = source.replace("data:image/png;base64,", "");
            return new BaseStructures.Texture(relativePath + "/" + strippedName(), Base64.getDecoder().decode(src));
        }

        public String strippedName() {
            return name.endsWith(".png") ? name.substring(0, name.length()-4) : name;
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
