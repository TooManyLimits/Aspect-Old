package io.github.moonlightmaya.manage.data;

import io.github.moonlightmaya.manage.data.importing.AspectImporter;
import io.github.moonlightmaya.manage.data.importing.JsonStructures;
import io.github.moonlightmaya.model.parts.AspectModelPart;
import io.github.moonlightmaya.model.animation.Animation;
import io.github.moonlightmaya.model.animation.Interpolation;
import io.github.moonlightmaya.util.IOUtils;
import io.github.moonlightmaya.util.MathUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import petpet.types.PetPetList;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contains the "base" structures in the data graph.
 * Base structures are records.
 * Bbmodels and other files => JsonStructures
 * JsonStructures => BaseStructures.
 * BaseStructures <=> raw bytes. (.aspect file)
 * BaseStructures => Aspect instance.
 */
public class BaseStructures {

    public record AspectStructure(
            MetadataStructure metadata,
            ModelPartStructure entityRoot,
            List<ModelPartStructure> worldRoots,
            ModelPartStructure hudRoot,
            List<TextureStructure> textures,
            List<AnimationStructure> animations,
            List<ScriptStructure> scripts
    ) {
        /**
         * Note that the metadata is written out first!!
         * This is crucial because it allows us to only read
         * the metadata of a file when opening it!
         */
        public void write(DataOutputStream out) throws IOException {
            //Metadata must be first
            metadata.write(out);

            //Animations are needed by model parts, so we must write and read them before model parts
            IOUtils.writeVarInt(out, animations.size());
            for (AnimationStructure animation : animations) animation.write(out);

            entityRoot.write(out, animations);

            IOUtils.writeVarInt(out, worldRoots.size());
            for (ModelPartStructure worldRoot : worldRoots) worldRoot.write(out, animations);

            hudRoot.write(out, animations);

            IOUtils.writeVarInt(out, textures.size());
            for (TextureStructure texture : textures) texture.write(out);

            IOUtils.writeVarInt(out, scripts.size());
            for (ScriptStructure script : scripts) script.write(out);
        }

        public static AspectStructure read(DataInputStream in) throws IOException {
            //Metadata always first, so we can read just a metadata object from a stream if needed
            MetadataStructure metadata = MetadataStructure.read(in);

            //Animations are needed by model parts, so we put them early
            int numAnimations = IOUtils.readVarInt(in);
            List<AnimationStructure> animations = numAnimations > 0 ? new ArrayList<>(numAnimations) : List.of();
            for (int i = 0; i < numAnimations; i++)
                animations.add(AnimationStructure.read(in));

            ModelPartStructure entityRoot = ModelPartStructure.read(in, animations);

            int numWorldRoots = IOUtils.readVarInt(in);
            List<ModelPartStructure> worldRoots = numWorldRoots > 0 ? new ArrayList<>(numWorldRoots) : List.of();
            for (int i = 0; i < numWorldRoots; i++)
                worldRoots.add(ModelPartStructure.read(in, animations));

            ModelPartStructure hudRoot = ModelPartStructure.read(in, animations);

            int numTextures = IOUtils.readVarInt(in);
            List<TextureStructure> textures = numTextures > 0 ? new ArrayList<>(numTextures) : List.of();
            for (int i = 0; i < numTextures; i++)
                textures.add(TextureStructure.read(in));

            int numScripts = IOUtils.readVarInt(in);
            List<ScriptStructure> scripts = numScripts > 0 ? new ArrayList<>(numScripts) : List.of();
            for (int i = 0; i < numScripts; i++)
                scripts.add(ScriptStructure.read(in));
            return new AspectStructure(
                    metadata, entityRoot, worldRoots, hudRoot, textures, animations, scripts
            );
        }
    }

    public record MetadataStructure(
            String name,
            String version,
            Vector3f color,
            List<String> authors
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(version);
            IOUtils.writeVector3f(out, color);
            IOUtils.writeVarInt(out, authors.size());
            for (String author : authors)
                out.writeUTF(author);
        }

        public static MetadataStructure read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            String version = in.readUTF();
            Vector3f color = IOUtils.readVector3f(in);
            int authorCount = IOUtils.readVarInt(in);
            List<String> authors = new PetPetList<>(authorCount);
            for (int i = 0; i < authorCount; i++)
                authors.add(in.readUTF());
            return new MetadataStructure(
                    name, version, color, authors
            );
        }
    }

    public record ModelPartStructure(
            String name,
            Vector3f rot, Vector3f pivot, boolean visible,
            List<ModelPartStructure> children,
            AspectModelPart.ModelPartType type,
            List<AnimatorStructure> animators,
            @Nullable CubeData cubeData,
            @Nullable MeshData meshData
    ) {
        public void write(DataOutputStream out, List<AnimationStructure> animations) throws IOException {
            out.writeUTF(name);
            IOUtils.writeVector3f(out, rot);
            IOUtils.writeVector3f(out, pivot);

            out.write(
                    (visible ? 1 : 0) | //Visible flag
                    (cubeData != null ? 2 : 0) | //Has cube data
                    (meshData != null ? 4 : 0) | //Has mesh data
                    (animators.size() > 0 ? 8 : 0)); //Has at least 1 animator

            if (children != null) {
                IOUtils.writeVarInt(out, children.size());
                for (ModelPartStructure child : children)
                    child.write(out, animations);
            } else {
                IOUtils.writeVarInt(out, 0);
            }

            out.write(type.ordinal());

            if (animators.size() > 0) {
                IOUtils.writeVarInt(out, animators.size());
                for (AnimatorStructure animator : animators)
                    animator.write(out, animations);
            }

            if (cubeData != null)
                cubeData.write(out);
            if (meshData != null)
                meshData.write(out);
        }

        public static ModelPartStructure read(DataInputStream in, List<AnimationStructure> animations) throws IOException {
            String name = in.readUTF();
            Vector3f rot = IOUtils.readVector3f(in);
            Vector3f pivot = IOUtils.readVector3f(in);

            int flags = in.read();
            boolean visible = (flags & 1) > 0;
            boolean hasCubeData = (flags & 2) > 0;
            boolean hasMeshData = (flags & 4) > 0;
            boolean hasAnimations = (flags & 8) > 0;

            int numChildren = IOUtils.readVarInt(in);
            List<ModelPartStructure> children = numChildren > 0 ? new ArrayList<>(numChildren) : List.of();
            for (int i = 0; i < numChildren; i++)
                children.add(ModelPartStructure.read(in, animations));

            AspectModelPart.ModelPartType type = AspectModelPart.ModelPartType.values()[in.read()];

            List<AnimatorStructure> animators = new ArrayList<>();
            if (hasAnimations) {
                int count = IOUtils.readVarInt(in);
                for (int i = 0; i < count; i++)
                    animators.add(AnimatorStructure.read(in, animations));
            }

            CubeData cubeData = hasCubeData ? CubeData.read(in) : null;
            MeshData meshData = hasMeshData ? MeshData.read(in) : null;

            return new ModelPartStructure(
                    name, rot, pivot, visible, children, type, animators, cubeData, meshData
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
                IOUtils.writeVarInt(out, tex);
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
            int tex = IOUtils.readVarInt(in);
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

    //A mesh needs the following data:
    //- a texture index (a single mesh may have only one texture)
    //- all vertices' positions (x, y, z)
    //- all uvs for faces (list is 3-4x longer than the faces list)
    //- all faces (lists of 3 or 4 indices). if last value in vec4i is -1 then its a triangle
    public record MeshData(
            List<Vector3f> vertexPositions,
            List<Vector2f> uvs,
            List<Vector4i> faces,
            int tex
    ) {
        //Add a face to the mesh data while creating it.
        public int addFace(JsonStructures.MeshFace jsonFace, JsonStructures.Resolution resolution, Map<String, Vector3f> verticesMap, Map<String, Integer> indicesMap, int curIndex) throws AspectImporter.AspectImporterException {
            String[] verts = jsonFace.vertices();

            //Skip faces with fewer than 3 vertices or more than 4 vertices
            //Only accept tris and quads here
            if (verts.length < 3 || verts.length > 4)
                return curIndex;

            //Gather all the vertex positions
            Vector3f[] vertexPositions = new Vector3f[verts.length];
            int[] vertexIndices = new int[verts.length];
            Vector2f[] uvs = new Vector2f[verts.length];

            for (int i = 0; i < verts.length; i++) {
                vertexPositions[i] = verticesMap.get(verts[i]);
                uvs[i] = jsonFace.uv().get(verts[i]);

                //If we've already given a vertex an index, reuse it, otherwise get the next index
                if (indicesMap.containsKey(verts[i])) {
                    vertexIndices[i] = indicesMap.get(verts[i]);
                } else {
                    //This vertex hasn't been added yet, so add it and store index
                    this.vertexPositions.add(vertexPositions[i]);
                    indicesMap.put(verts[i], curIndex);
                    vertexIndices[i] = curIndex++;
                }
            }

            //We now have the positions and indices of these vertices, now we need to ensure there's no crossing
            //Reorder the vertices indices if necessary, this can only happen if it's a quad
            if (verts.length == 4) {
                if (testOppositeSides(vertexPositions[1], vertexPositions[2], vertexPositions[0], vertexPositions[3])) {
                    int temp1 = vertexIndices[2];
                    vertexIndices[2] = vertexIndices[1];
                    vertexIndices[1] = vertexIndices[0];
                    vertexIndices[0] = temp1;

                    Vector2f temp2 = uvs[2];
                    uvs[2] = uvs[1];
                    uvs[1] = uvs[0];
                    uvs[0] = temp2;
                } else if (testOppositeSides(vertexPositions[0], vertexPositions[1], vertexPositions[2], vertexPositions[3])) {
                    int temp = vertexIndices[2];
                    vertexIndices[2] = vertexIndices[1];
                    vertexIndices[1] = temp;

                    Vector2f temp2 = uvs[2];
                    uvs[2] = uvs[1];
                    uvs[1] = temp2;
                }
            }

            //Now that we have the indices in proper order, emit a face
            for (Vector2f uv : uvs)
                uv.div(resolution.width(), resolution.height());
            Collections.addAll(this.uvs, uvs);
            this.faces.add(new Vector4i(
                    vertexIndices[0],
                    vertexIndices[1],
                    vertexIndices[2],
                    verts.length == 3 ? -1 : vertexIndices[3]
            ));

            //Return the next unused index
            return curIndex;
        }

        //Tests if the two given points are on opposite sides of the given line
        //Sometimes blockbench likes to flip vertices, giving quad faces a weird "X" shape,
        //so this is used to un-flip them.
        private static boolean testOppositeSides(Vector3f linePoint1, Vector3f linePoint2, Vector3f point1, Vector3f point2) {
            Vector3f t1 = new Vector3f(linePoint1);
            Vector3f t2 = new Vector3f(linePoint2);
            Vector3f t3 = new Vector3f(point1);
            Vector3f t4 = new Vector3f(point2);

            t2.sub(t1);
            t3.sub(t1);
            t4.sub(t1);

            t1.set(t2);
            t1.cross(t3);
            t2.cross(t4);
            return t1.dot(t2) < 0;
        }

        public void write(DataOutputStream out) throws IOException {
            //Write all vertex positions
            int vertexCount = this.vertexPositions.size();
            IOUtils.writeVarInt(out, vertexCount);
            for (Vector3f vertexPos : this.vertexPositions)
                IOUtils.writeVector3f(out, vertexPos);

            //number of faces
            IOUtils.writeVarInt(out, this.faces.size());

            //Create bit vector for face sizes
            //0 means the face is a triangle, 1 means quad
            byte cur = 0;
            int bit = 0;
            for (Vector4i face : this.faces) {
                if (bit == 8) {
                    out.writeByte(cur);
                    bit = 0;
                    cur = 0;
                }
                if (face.w != -1)
                    cur |= (1 << bit);
                bit++;
            }
            if (bit != 0)
                out.writeByte(cur);

            //Write the actual face values
            Writer writer = DataOutputStream::writeByte;
            if (vertexCount > 255) writer = DataOutputStream::writeShort;
            if (vertexCount > 65535) writer = IOUtils::writeVarInt;

            int curUVCount = 0; //current uv may increment by 3 or 4 each time

            for (Vector4i face : this.faces) {
                writer.write(out, face.x);
                writer.write(out, face.y);
                writer.write(out, face.z);
                if (face.w != -1)
                    writer.write(out, face.w);

                Vector2f uv1 = this.uvs.get(curUVCount);
                Vector2f uv2 = this.uvs.get(curUVCount + 1);
                Vector2f uv3 = this.uvs.get(curUVCount + 2);

                if (face.w == -1) {
                    //Triangle, three UVs
                    IOUtils.writeVector3f(out, new Vector3f(uv1.x, uv2.x, uv3.x));
                    IOUtils.writeVector3f(out, new Vector3f(uv1.y, uv2.y, uv3.y));
                    curUVCount += 3;
                } else {
                    //Quad, four UVs
                    Vector2f uv4 = this.uvs.get(curUVCount + 3);
                    IOUtils.writeVector4f(out, new Vector4f(uv1.x, uv2.x, uv3.x, uv4.x));
                    IOUtils.writeVector4f(out, new Vector4f(uv1.y, uv2.y, uv3.y, uv4.y));
                    curUVCount += 4;
                }
            }

            //Tex
            IOUtils.writeVarInt(out, tex);
        }

        public static MeshData read(DataInputStream in) throws IOException {
            //Read vertex positions
            int vertexCount = IOUtils.readVarInt(in);
            List<Vector3f> vertexPositions = new ArrayList<>(vertexCount);
            for (int i = 0; i < vertexCount; i++)
                vertexPositions.add(IOUtils.readVector3f(in));

            //Get bit vector for tris/quads
            int faceCount = IOUtils.readVarInt(in);
            byte[] bitVector = in.readNBytes((int) Math.ceil(faceCount / 8.0));

            //Get reader
            Reader reader = DataInputStream::readUnsignedByte;
            if (vertexCount > 255) reader = DataInputStream::readUnsignedShort;
            if (vertexCount > 65535) reader = IOUtils::readVarInt;

            List<Vector2f> uvs = new ArrayList<>();
            List<Vector4i> faces = new ArrayList<>(faceCount);

            //Read the face data, including uvs
            for (int i = 0; i < faceCount; i++) {
                boolean isQuad = (bitVector[i / 8] & (1 << (i % 8))) > 0;

                if (isQuad) {
                    //Read 4 vertices, then the uvs
                    faces.add(new Vector4i(reader.read(in), reader.read(in), reader.read(in), reader.read(in)));
                    Vector4f us = IOUtils.readVector4f(in);
                    Vector4f vs = IOUtils.readVector4f(in);
                    uvs.add(new Vector2f(us.x, vs.x));
                    uvs.add(new Vector2f(us.y, vs.y));
                    uvs.add(new Vector2f(us.z, vs.z));
                    uvs.add(new Vector2f(us.w, vs.w));
                } else {
                    faces.add(new Vector4i(reader.read(in), reader.read(in), reader.read(in), -1));
                    Vector3f us = IOUtils.readVector3f(in);
                    Vector3f vs = IOUtils.readVector3f(in);
                    uvs.add(new Vector2f(us.x, vs.x));
                    uvs.add(new Vector2f(us.y, vs.y));
                    uvs.add(new Vector2f(us.z, vs.z));
                }
            }

            int tex = IOUtils.readVarInt(in);
            return new MeshData(vertexPositions, uvs, faces, tex);
        }

        @FunctionalInterface
        private interface Writer {
            void write(DataOutputStream dos, int i) throws IOException;
        }

        @FunctionalInterface
        private interface Reader {
            int read(DataInputStream dis) throws IOException;
        }

    }


    public record TextureStructure(
            String name,
            byte[] data
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            IOUtils.writeVarInt(out, data.length);
            out.write(data);
        }

        public static TextureStructure read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            int dataLen = IOUtils.readVarInt(in);
            byte[] data = new byte[dataLen];
            in.readNBytes(data, 0, dataLen);
            return new TextureStructure(name, data);
        }
    }

    public record AnimationStructure(
            String name, Animation.LoopMode loopMode, boolean override, float length, float snapping
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            IOUtils.writeVarInt(out, loopMode.ordinal());
            out.writeBoolean(override);
            out.writeFloat(length);
        }

        public static AnimationStructure read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            Animation.LoopMode loopMode = Animation.LoopMode.values()[IOUtils.readVarInt(in)];
            boolean override = in.readBoolean();
            float length = in.readFloat();
            float snapping = in.readFloat();
            return new AnimationStructure(name, loopMode, override, length, snapping);
        }
    }

    /**
     * Unlike in .bbmodel files, where animators are stored inside of animation objects
     * and use UUIDs to reference specific model parts, we do not want to store
     * UUIDs of model parts. So instead we store the Animator objects inside a
     * ModelPartStructure. As a result, we need some way for an animator to know
     * which animation it's part of.
     */
    public record AnimatorStructure(
            int animationIndex, //Which animation does this animator belong to?
            List<KeyframeStructure> posKeyframes,
            List<KeyframeStructure> rotKeyframes,
            List<KeyframeStructure> scaleKeyframes
    ) {
        public void write(DataOutputStream out, List<AnimationStructure> animations) throws IOException {
            AnimationStructure animation = animations.get(this.animationIndex);

            IOUtils.writeVarInt(out, animationIndex);

            IOUtils.writeVarInt(out, posKeyframes.size());
            for (KeyframeStructure kf : posKeyframes)
                kf.write(out, animation.snapping);

            IOUtils.writeVarInt(out, rotKeyframes.size());
            for (KeyframeStructure kf : rotKeyframes)
                kf.write(out, animation.snapping);

            IOUtils.writeVarInt(out, scaleKeyframes.size());
            for (KeyframeStructure kf : scaleKeyframes)
                kf.write(out, animation.snapping);
        }

        public static AnimatorStructure read(DataInputStream in, List<AnimationStructure> animations) throws IOException {
            int animationIndex = IOUtils.readVarInt(in);

            AnimationStructure animation = animations.get(animationIndex);

            int numPosKeyframes = IOUtils.readVarInt(in);
            List<KeyframeStructure> posKeyframes = new ArrayList<>(numPosKeyframes);
            for (int i = 0; i < numPosKeyframes; i++)
                posKeyframes.add(KeyframeStructure.read(in, animation.snapping));

            int numRotKeyframes = IOUtils.readVarInt(in);
            List<KeyframeStructure> rotKeyframes = new ArrayList<>(numRotKeyframes);
            for (int i = 0; i < numRotKeyframes; i++)
                rotKeyframes.add(KeyframeStructure.read(in, animation.snapping));

            int numScaleKeyframes = IOUtils.readVarInt(in);
            List<KeyframeStructure> scaleKeyframes = new ArrayList<>(numScaleKeyframes);
            for (int i = 0; i < numScaleKeyframes; i++)
                scaleKeyframes.add(KeyframeStructure.read(in, animation.snapping));

            return new AnimatorStructure(animationIndex, posKeyframes, rotKeyframes, scaleKeyframes);
        }
    }

    public record KeyframeStructure(
            float time, Interpolation.Builtin interpolation,
            Object x, Object y, Object z, //string or number
            @Nullable Vector3f bezierLeftTime,
            @Nullable Vector3f bezierLeftValue,
            @Nullable Vector3f bezierRightTime,
            @Nullable Vector3f bezierRightValue
    ) {

        /**
         * 0: The value is 0
         * 1: The value is an int
         * 2: The value is an arbitrary float
         * 3: The value is a string
         */
        private int getBitsFor(Object val) {
            if (val instanceof String)
                return 3;
            if (val instanceof Number n) {
                float v = n.floatValue();
                if (MathUtils.epsilon(v))
                    return 0;
                if (MathUtils.epsilon(v % 1))
                    return 1;
                return 2;
            }
            throw new RuntimeException("Should be impossible?");
        }

        private void writeValue(DataOutputStream out, Object val, byte flags, int index) throws IOException {
            int bits = (flags >> (index * 2)) & 0b11;
            switch (bits) {
                case 0 -> {}
                case 1 -> IOUtils.writeVarInt(out, Math.round(((Number) val).floatValue()));
                case 2 -> out.writeFloat(((Number) val).floatValue());
                case 3 -> out.writeUTF((String) val);
                default -> throw new IOException("Unexpected writing keyframe bits: " + bits + ". Should be impossible?");
            }
        }

        private static Object readValue(DataInputStream in, byte flags, int index) throws IOException {
            int bits = (flags >> (index * 2)) & 0b11;
            return switch (bits) {
                case 0 -> 0f;
                case 1 -> (float) IOUtils.readVarInt(in);
                case 2 -> in.readFloat();
                case 3 -> in.readUTF();
                default -> throw new IOException("Unexpected reading keyframe bits: " + bits + ". Should be impossible?");
            };
        }

        public void write(DataOutputStream out, float snapping) throws IOException {
            //Construct bit flags and write them
            boolean isTimeIntAfterSnapping = MathUtils.epsilon((time * snapping) % 1);
            byte bitFlags = (byte) (
                    getBitsFor(x) |
                    getBitsFor(y) << 2 |
                    getBitsFor(z) << 4 |
                    (isTimeIntAfterSnapping ? 1 : 0) << 6
            );
            out.writeByte(bitFlags);

            //Write interpolation
            IOUtils.writeVarInt(out, interpolation.ordinal());

            //Write bezier things if this is bezier
            if (interpolation == Interpolation.Builtin.BEZIER) {
                IOUtils.writeVector3f(out, this.bezierLeftTime);
                IOUtils.writeVector3f(out, this.bezierLeftValue);
                IOUtils.writeVector3f(out, this.bezierRightTime);
                IOUtils.writeVector3f(out, this.bezierRightValue);
            }

            //Write values
            writeValue(out, x, bitFlags, 0);
            writeValue(out, y, bitFlags, 1);
            writeValue(out, z, bitFlags, 2);

            //Write time
            if (isTimeIntAfterSnapping)
                IOUtils.writeVarInt(out, Math.round(time * snapping));
            else
                out.writeFloat(time);
        }

        public static KeyframeStructure read(DataInputStream in, float snapping) throws IOException {
            //Read flags
            byte flags = in.readByte();
            //Read interpolation
            Interpolation.Builtin interpolation = Interpolation.Builtin.values()[IOUtils.readVarInt(in)];
            //Read bezier things if this is bezier
            Vector3f bezierLeftTime = null, bezierLeftValue = null, bezierRightTime = null, bezierRightValue = null;
            if (interpolation == Interpolation.Builtin.BEZIER) {
                bezierLeftTime = IOUtils.readVector3f(in);
                bezierLeftValue = IOUtils.readVector3f(in);
                bezierRightTime = IOUtils.readVector3f(in);
                bezierRightValue = IOUtils.readVector3f(in);
            }
            //Read values
            Object x = readValue(in, flags, 0);
            Object y = readValue(in, flags, 1);
            Object z = readValue(in, flags, 2);
            //Read time
            boolean isTimeIntAfterSnapping = (flags & 0b1000000) > 0;
            float time = isTimeIntAfterSnapping ? IOUtils.readVarInt(in) / snapping : in.readFloat();
            //Create
            return new KeyframeStructure(time, interpolation, x, y, z, bezierLeftTime, bezierLeftValue, bezierRightTime, bezierRightValue);
        }

        //True if it's a bezier keyframe
        public boolean isBezier() {
            return this.interpolation == Interpolation.Builtin.BEZIER;
        }

        //True if this has a string element
        public boolean isFunction() {
            return x instanceof String || y instanceof String || z instanceof String;
        }
    }

    public record ScriptStructure(
            String name,
            String source
    ) {
        public void write(DataOutputStream out) throws IOException {
            out.writeUTF(name);
            out.writeUTF(source);
        }

        public static ScriptStructure read(DataInputStream in) throws IOException {
            String name = in.readUTF();
            String source = in.readUTF();
            return new ScriptStructure(name, source);
        }
    }

}
