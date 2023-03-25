package io.github.moonlightmaya.data;

import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.util.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the "base" structures in the data graph.
 * Base structures are records.
 * Bbmodels => JsonStructures
 * JsonStructures => BaseStructures.
 * BaseStructures <=> raw bytes. This step uses reflection.
 * BaseStructures => Aspect instance.
 */
public class BaseStructures {

    public record AspectStructure(
            ModelPartStructure entityRoot,
            List<ModelPartStructure> worldRoots,
            List<Texture> textures,
            List<Script> scripts
    ) {
        public void write(DataOutputStream out) throws IOException {
            entityRoot.write(out);

            out.writeInt(worldRoots.size());
            for (ModelPartStructure worldRoot : worldRoots) worldRoot.write(out);

            out.writeInt(textures.size());
            for (Texture texture : textures) texture.write(out);

            out.writeInt(scripts.size());
            for (Script script : scripts) script.write(out);
        }

        public static AspectStructure read(DataInputStream in) throws IOUtils.AspectIOException {
            try {
                ModelPartStructure entityRoot = ModelPartStructure.read(in);

                int numWorldRoots = in.readInt();
                List<ModelPartStructure> worldRoots = numWorldRoots > 0 ? new ArrayList<>(numWorldRoots) : List.of();
                for (int i = 0; i < numWorldRoots; i++)
                    worldRoots.add(ModelPartStructure.read(in));

                int numTextures = in.readInt();
                List<Texture> textures = numTextures > 0 ? new ArrayList<>(numTextures) : List.of();
                for (int i = 0; i < numTextures; i++)
                    textures.add(Texture.read(in));

                int numScripts = in.readInt();
                List<Script> scripts = numScripts > 0 ? new ArrayList<>(numScripts) : List.of();
                for (int i = 0; i < numScripts; i++)
                    scripts.add(Script.read(in));
                return new AspectStructure(
                        entityRoot, worldRoots, textures, scripts
                );
            } catch (IOException e) {
                throw new IOUtils.AspectIOException(e);
            }
        }
    }

    public record ModelPartStructure(
            String name,
            Vector3f pos, Vector3f rot, Vector3f pivot, boolean visible,
            List<ModelPartStructure> children,
            AspectModelPart.ModelPartType type,
            @Nullable CubeData cubeData
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            IOUtils.writeVector3f(out, pos);
            IOUtils.writeVector3f(out, rot);
            IOUtils.writeVector3f(out, pivot);
            out.write((visible ? 1 : 0) | (cubeData != null ? 2 : 0));
            out.writeInt(children.size());
            for (ModelPartStructure child : children)
                child.write(out);
            out.write(type.ordinal());
            if (cubeData != null)
                cubeData.write(out);
        }

        public static ModelPartStructure read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            Vector3f pos = IOUtils.readVector3f(in);
            Vector3f rot = IOUtils.readVector3f(in);
            Vector3f pivot = IOUtils.readVector3f(in);
            int visibleAndHasCubeData = in.read();
            boolean visible = (visibleAndHasCubeData & 1) > 0;
            boolean hasCubeData = (visibleAndHasCubeData & 2) > 0;
            int numChildren = in.readInt();
            List<ModelPartStructure> children = numChildren > 0 ? new ArrayList<>(numChildren) : List.of();
            for (int i = 0; i < numChildren; i++)
                children.add(ModelPartStructure.read(in));
            AspectModelPart.ModelPartType type = AspectModelPart.ModelPartType.values()[in.read()];
            CubeData cubeData = hasCubeData ? CubeData.read(in) : null;
            return new ModelPartStructure(
                    name, pos, rot, pivot, visible, children, type, cubeData
            );
        }
    }

    public record CubeData(
            Vector3f from, Vector3f to,
            CubeFaces faces
    ) {
        public void write(DataOutputStream out) throws IOException {
            IOUtils.writeVector3f(out, from);
            IOUtils.writeVector3f(out, to);
            faces.write(out);
        }

        public static CubeData read(DataInputStream in) throws IOException {
            Vector3f from = IOUtils.readVector3f(in);
            Vector3f to = IOUtils.readVector3f(in);
            CubeFaces faces = CubeFaces.read(in);
            return new CubeData(from, to, faces);
        }
    }

    public record CubeFaces(
        byte presentFaces,
        List<CubeFace> faces, //n, e, s, w, u, d
        int tex
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(presentFaces);
            int face = 0;
            for (int i = 0; i < 6; i++) {
                boolean present = (presentFaces & (1 << i)) > 0;
                if (!present) continue;
                faces.get(face++).write(out);
            }
            if (presentFaces != 0)
                out.writeInt(tex);
        }

        public static CubeFaces read(DataInputStream in) throws IOException {
            byte presentFaces = in.readByte();
            if (presentFaces == 0)
                return new CubeFaces(presentFaces, List.of(), -1);

            List<CubeFace> faces = new ArrayList<>(6);
            for (int i = 0; i < 6; i++) {
                boolean present = (presentFaces & (1 << i)) > 0;
                if (present)
                    faces.add(CubeFace.read(in));
            }
            int tex = in.readInt();
            return new CubeFaces(presentFaces, faces, tex);
        }
    }

    public record CubeFace(
            Vector4f uvs, //u1 v1 u2 v2
            int rot //0 1 2 3
    ) {
        public void write(DataOutputStream out) throws IOException {
            IOUtils.writeVector4f(out, uvs);
            out.write(rot);
        }

        public static CubeFace read(DataInputStream in) throws IOException {
            Vector4f uvs = IOUtils.readVector4f(in);
            int rot = in.read();
            return new CubeFace(uvs, rot);
        }
    }

    public record Texture(
            String name,
            byte[] data
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            out.writeInt(data.length);
            out.write(data);
        }

        public static Texture read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            int dataLen = in.readInt();
            byte[] data = new byte[dataLen];
            in.readNBytes(data, 0, dataLen);
            return new Texture(name, data);
        }
    }

    public record Script(
            String name,
            String source
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(source);
        }

        public static Script read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            String source = in.readUTF();
            return new Script(name, source);
        }
    }

}
