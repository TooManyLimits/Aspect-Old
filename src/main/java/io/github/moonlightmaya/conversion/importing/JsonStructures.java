package io.github.moonlightmaya.conversion.importing;

import com.google.gson.*;
import io.github.moonlightmaya.AspectModelPart;
import io.github.moonlightmaya.conversion.BaseStructures;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class JsonStructures {

    public static class BBModel {
        public Resolution resolution;
        public Part[] elements;
        public JsonArray outliner;
        public Part[] fixedOutliner;
        public Texture[] textures;

        public Gson getGson() {
            return new GsonBuilder()
                    .registerTypeAdapter(Vector3f.class, Vector3fDeserializer.INSTANCE)
                    .registerTypeAdapter(Vector4f.class, Vector4fDeserializer.INSTANCE)
                    .registerTypeAdapter(Part.class, new OutlinerPartDeserializer(this))
                    .create();
        }
    }

    public record Resolution(int width, int height) {}

    public record Part(String name, float color, Vector3f origin, Vector3f rotation, boolean visibility, String type,
                       String uuid, Vector3f from, Vector3f to, CubeFaces faces, JsonStructures.Part[] children) {
            public BaseStructures.ModelPartStructure toBaseStructure(List<Integer> texMapper, Resolution resolution) throws AspectImporter.AspectImporterException {
                List<BaseStructures.CubeFaces> faces = null;
                if (this.faces != null) {
                    faces = this.faces.toBaseStructure(texMapper, resolution);
                    int n = faces.size();
                    if (n > 1) {
                        if (children != null) {
                            throw new AspectImporter.AspectImporterException("Attempted to split a part with children? Invalid BBModel, notify devs");
                        }
                        List<BaseStructures.ModelPartStructure> splitCubes = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            splitCubes.add(new BaseStructures.ModelPartStructure(
                                "split" + i, new Vector3f(), new Vector3f(), new Vector3f(), visibility, null,
                                type != null ? AspectModelPart.ModelPartType.valueOf(type.toUpperCase()) : AspectModelPart.ModelPartType.GROUP,
                                new BaseStructures.CubeData(from, to, faces.get(i))
                            ));
                        }
                        return new BaseStructures.ModelPartStructure(
                                name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility,
                                splitCubes, AspectModelPart.ModelPartType.GROUP, null
                        );
                    }
                }

                ArrayList<BaseStructures.ModelPartStructure> baseChildren = children == null ? null : new ArrayList<>(children.length);
                if (baseChildren != null)
                    for (Part p : children)
                        baseChildren.add(p.toBaseStructure(texMapper, resolution));

                return new BaseStructures.ModelPartStructure(
                        name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility,
                        baseChildren,
                        type != null ? AspectModelPart.ModelPartType.valueOf(type.toUpperCase()) : AspectModelPart.ModelPartType.GROUP,
                        (faces != null && faces.size() == 1) ? new BaseStructures.CubeData(from, to, faces.get(0)) : null
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
                    uv[0]/resolution.width, uv[1]/resolution.height, uv[2]/resolution.width, uv[3]/resolution.height,
                    rotation != null ? (((rotation/90)%4)+4)%4 : 0);
        }
        public int tex() {
            return texture != null ? texture : -1;
        }
    }

    public record Texture(
             String name, String source
    ) {
        public BaseStructures.Texture toBaseStructure() {
            String src = source.replace("data:image/png;base64,", "");
            return new BaseStructures.Texture(name, Base64.getDecoder().decode(src));
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
                        ctx.deserialize(json.get("visibility"), boolean.class),
                        ctx.deserialize(json.get("type"), String.class),
                        ctx.deserialize(json.get("uuid"), String.class),
                        ctx.deserialize(json.get("from"), Vector3f.class),
                        ctx.deserialize(json.get("to"), Vector3f.class),
                        ctx.deserialize(json.get("faces"), CubeFaces.class),
                        ctx.deserialize(json.get("children"), Part[].class)
                );
            }
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
