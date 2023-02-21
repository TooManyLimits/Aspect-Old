package io.github.moonlightmaya.nbt;

import io.github.moonlightmaya.util.IOUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a variety of useful data structure classes
 * that are used for dealing with Aspects' NBT states.
 * These objects can be written to NBT, when importing from the file system,
 * and they can be read from NBT, when loading an Aspect from the backend or
 * from the NBT generated from the file system.
 */
public class NbtStructures {

    public record NbtModelPart(String name, Vector3f pos, Vector3f rot, Vector3f pivot, List<NbtModelPart> children) { //, List<NBTRenderLayer> renderLayers) {
        public NbtCompound write() {
            NbtCompound out = new NbtCompound();
            out.putString(IOUtils.shorten("name"), name);
            writeVector(out, "pos", pos);
            writeVector(out, "rot", rot);
            writeVector(out, "pivot", pivot);
            if (children != null && !children.isEmpty()) {
                NbtList childrenNbt = new NbtList();
                for (NbtModelPart child : children)
                    childrenNbt.add(child.write());
                out.put(IOUtils.shorten("children"), childrenNbt);
            }
            return out;
        }
        public static NbtModelPart read(NbtCompound nbt) {
            String name = nbt.getString(IOUtils.shorten("name"));
            Vector3f pos = readVector(nbt, "pos");
            Vector3f rot = readVector(nbt, "rot");
            Vector3f pivot = readVector(nbt, "pivot");
            ArrayList<NbtModelPart> children = null;
            if (nbt.contains(IOUtils.shorten("children"))) {
                children = new ArrayList<>();
                for (NbtElement child : nbt.getList(IOUtils.shorten("children"), NbtElement.COMPOUND_TYPE))
                    children.add(read((NbtCompound) child));
            }
            return new NbtModelPart(name, pos, rot, pivot, children);
        }

        private static Vector3f readVector(NbtCompound nbt, String name) {
            return new Vector3f(
                    nbt.getFloat(IOUtils.shorten(name+"X")),
                    nbt.getFloat(IOUtils.shorten(name+"Y")),
                    nbt.getFloat(IOUtils.shorten(name+"Z"))
            );
        }

        private static void writeVector(NbtCompound nbt, String name, Vector3f vec) {
            if (vec.x != 0)
                nbt.putFloat(IOUtils.shorten(name+"X"), vec.x);
            if (vec.y != 0)
                nbt.putFloat(IOUtils.shorten(name+"Y"), vec.y);
            if (vec.z != 0)
                nbt.putFloat(IOUtils.shorten(name+"Z"), vec.z);
        }

        public record CubeData(Vector3f from, Vector3f to, FaceData north) {
            public static CubeData read(NbtCompound nbt) {
                Vector3f from = readVector(nbt, "from");
                Vector3f to = readVector(nbt, "to");
            }

            public NbtCompound write() {

            }

            public record FaceData(int texture, Vector4f uv) {

            }
        }

    }

    public record NbtTexture(String name, byte[] source) {
        public NbtCompound write() {
            NbtCompound out = new NbtCompound();
            if (name != null)
                out.putString(IOUtils.shorten("name"), name);
            out.putByteArray(IOUtils.shorten("source"), source);
            return out;
        }
        public static NbtTexture read(NbtCompound nbt) {
            String name = nbt.contains(IOUtils.shorten("name")) ? nbt.getString(IOUtils.shorten("name")) : null;
            byte[] data = nbt.getByteArray(IOUtils.shorten("source"));
            return new NbtTexture(name, data);
        }
    }


}
