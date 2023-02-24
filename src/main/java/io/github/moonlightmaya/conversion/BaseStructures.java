package io.github.moonlightmaya.conversion;

import io.github.moonlightmaya.AspectModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    ) {}

    public record ModelPartStructure(
            String name,
            Vector3f pos, Vector3f rot, Vector3f pivot, boolean visible,
            List<ModelPartStructure> children,
            AspectModelPart.ModelPartType type,
            @Nullable CubeData cubeData) {}

    public record CubeData(
            Vector3f from, Vector3f to,
            List<CubeFace> faces
    ) {}

    public record CubeFace(
            float u1, float v1, float u2, float v2,
            int tex
    ) {}

    public record Texture(
            String name,
            byte[] data
    ) {}

    public record Script(
            String name,
            String source
    ) {}

}
