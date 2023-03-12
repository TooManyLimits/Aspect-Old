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

    public class BBModel {
        public Resolution resolution;
        public Part[] elements;
        public JsonArray outliner;
        public Part[] fixedOutliner;

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
            public BaseStructures.ModelPartStructure toBaseStructure() {
                return new BaseStructures.ModelPartStructure(
                        name, new Vector3f(), rotation == null ? new Vector3f() : rotation, origin, visibility,
                        children == null ? null :
                                Arrays.stream(children).map(Part::toBaseStructure).collect(Collectors.toList()),
                        type != null ? AspectModelPart.ModelPartType.valueOf(type.toUpperCase()) : AspectModelPart.ModelPartType.GROUP,
                        faces != null ? new BaseStructures.CubeData(from, to, faces.toBaseStructure()) : null
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
        public List<BaseStructures.CubeFace> toBaseStructure() {
            ArrayList<BaseStructures.CubeFace> faces = new ArrayList<>();
            faces.add(north.toBaseStructure());
            faces.add(east.toBaseStructure());
            faces.add(south.toBaseStructure());
            faces.add(west.toBaseStructure());
            faces.add(up.toBaseStructure());
            faces.add(down.toBaseStructure());
            return faces;
        }
    }

    public record CubeFace(
            float[] uv,
            Integer texture
    ) {
        public BaseStructures.CubeFace toBaseStructure() {
            return new BaseStructures.CubeFace(uv[0], uv[1], uv[2], uv[3], texture != null ? texture : -1);
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
