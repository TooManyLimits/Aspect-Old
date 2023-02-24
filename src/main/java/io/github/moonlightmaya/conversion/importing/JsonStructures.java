package io.github.moonlightmaya.conversion.importing;

import com.google.gson.*;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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
                    .registerTypeAdapter(OutlinerPart.class, new OutlinerPartDeserializer(this))
                    .create();
        }
    }

    public record Resolution(int width, int height) {}
    public class Part {
        public String name;
        public float color;
        public Vector3f origin;
        public Vector3f rotation;
        public boolean visibility;
        public String type;
        public String uuid;

        public Vector3f from;
        public Vector3f to;
        public CubeFaces faces;
    }
    public record CubeFaces(
            CubeFace north,
            CubeFace east,
            CubeFace south,
            CubeFace west,
            CubeFace up,
            CubeFace down
    ) {}

    public record CubeFace(
            float[] uv,
            Integer texture
    ) {}

    public class OutlinerPart extends Part {
        public OutlinerPart[] children;
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
                return ctx.deserialize(element, type);
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
