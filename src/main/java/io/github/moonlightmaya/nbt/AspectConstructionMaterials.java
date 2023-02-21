package io.github.moonlightmaya.nbt;

import io.github.moonlightmaya.util.IOUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The ultimate structure in the Aspect NBT hierarchy. Contains all
 * data for an entire Aspect. One of these can be generated from a folder
 * or read from NBT data.
 * A general diagram is of data flow is as follows:
 *
 * File System
 *     |
 *     |
 *     v
 * Construction Materials / "NbtStructures" --> Aspect instance
 *     ^
 *     |
 *     v
 *  An NBT compound
 *
 * To reiterate, construction materials can be read from the file system, or
 * read from NBT. They can also be written into NBT. They can be converted into
 * an Aspect instance.
 */
public record AspectConstructionMaterials(
        NbtStructures.NbtModelPart entityRoot,
        List<NbtStructures.NbtModelPart> worldRoots
) {

    /**
     * Gathers materials for an Aspect from the given NBT data.
     * May take some time to complete - can be done on an
     * alternate thread if necessary! The NBT structure is
     * as follows:
     *
     * entity: ModelPart
     * world: List<ModelPart>
     * skull: ModelPart
     * hud: ModelPart
     * portrait: ModelPart
     * textures: List<Texture>
     * customRenderLayers: List<CustomRenderLayer>
     * scripts: List<Script>
     *
     * ModelPart: {
     *    name: string
     *    posX, posY, posZ, rotX, rotY, rotZ, pivX, pivY, pivZ: float, omitted if 0
     *    children: List<ModelPart>
     *    renderLayers: optional List<RenderLayer>
     *    cubeData: optional CubeData
     *    meshData: optional MeshData
     * }
     * Texture: {
     *    name: optional string
     *    data: ByteArray
     * }
     * RenderLayer: {
     *    factory: string or int. string for vanilla, int for index into custom
     *    texture: int <index into textures> OR textures: IntArray <indices into textures>
     * }
     * CubeData: { //Will be upgraded later to use a more compact format. For the time being, it's going to be lazy.
     *    fromX, fromY, fromZ, toX, toY, toZ: float
     *    faceData: {
     *        north: {tex: int, uv: [float x 4] u1,v1,u2,v2}
     *        south: {tex: int, uv: [float x 4] ...}
     *        ...
     *    }
     * }
     * MeshData: {
     *
     * }
     * Script: {
     *     name: string
     *     source: string
     * }
     * CustomRenderLayer: {
     *    vertexFormat: string, omitted if Aspect's vertex format
     *    vertexShader, fragmentShader: optional<string> (must have both if either)
     * }
     */
    public static AspectConstructionMaterials fromNbt(NbtCompound compound) {
        NbtStructures.NbtModelPart entityRoot = NbtStructures.NbtModelPart.read(compound.getCompound(IOUtils.shorten("entity")));
        NbtList worldRootsNbt = compound.getList(IOUtils.shorten("world"), NbtElement.COMPOUND_TYPE);
        ArrayList<NbtStructures.NbtModelPart> worldRoots = new ArrayList<>();
        for (NbtElement e : worldRootsNbt)
            worldRoots.add(NbtStructures.NbtModelPart.read((NbtCompound) e));

        return new AspectConstructionMaterials(entityRoot, worldRoots);
    }

    public NbtCompound toNbt() {
        return null;
    }

    public static AspectConstructionMaterials fromFiles(File file) {
        return null;
    }




}
