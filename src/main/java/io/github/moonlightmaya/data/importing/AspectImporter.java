package io.github.moonlightmaya.data.importing;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.util.IOUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;
import petpet.types.PetPetList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Deals with the task of importing an Aspect
 * from the file system. Calling the constructor
 * with a path will perform this operation.
 * Consider running it on a different thread.
 *
 * The ultimate goal of this class is to produce
 * a BaseStructures.AspectStructure.
 */
public class AspectImporter {

    private final Path rootPath;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Vector3f.class, JsonStructures.Vector3fDeserializer.INSTANCE)
            .registerTypeAdapter(Vector4f.class, JsonStructures.Vector4fDeserializer.INSTANCE)
            .setPrettyPrinting().create();

    private BaseStructures.MetadataStructure metadata;
    private LinkedHashMap<String, BaseStructures.Texture> textures;
    private List<BaseStructures.Script> scripts;
    private int textureOffset;

    public AspectImporter(Path aspectFolder) {
        this.rootPath = aspectFolder;
    }

    public CompletableFuture<BaseStructures.AspectStructure> doImport() {
        CompletableFuture<BaseStructures.AspectStructure> result = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                if (!Files.exists(rootPath))
                    throw new AspectImporterException("Folder " + IOUtils.trimPathStringToModFolder(rootPath) + " does not exist");

                if (!Files.exists(rootPath.resolve("aspect.json")))
                    throw new AspectImporterException("Folder " + IOUtils.trimPathStringToModFolder(rootPath) + " has no aspect.json");

                metadata = getMetadata();
                //Read the globally shared textures to here
                textures = getTextures();
                //Read scripts
                scripts = getScripts();
                //Get entity model parts:
                BaseStructures.ModelPartStructure entityRoot = getEntityRoot();
                //Get world model parts:
                List<BaseStructures.ModelPartStructure> worldRoots = getWorldRoots();
                //Get hud model parts:
                BaseStructures.ModelPartStructure hudRoot = getHudRoot();


                result.complete(new BaseStructures.AspectStructure(
                        metadata,
                        entityRoot, worldRoots, hudRoot,
                        Lists.newArrayList(textures.values()),
                        scripts
                ));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private BaseStructures.MetadataStructure getMetadata() throws IOException, AspectImporterException {
        Path p = rootPath.resolve("aspect.json");
        String json = Files.readString(p);
        if (json.isBlank()) {
            return new BaseStructures.MetadataStructure("", "", new Vector3f(1,1,1), new PetPetList<>());
        }

        try {
            JsonStructures.Metadata jsonMetadata = gson.fromJson(json, JsonStructures.Metadata.class);
            return jsonMetadata.toBaseStructure();
        } catch (Exception e) {
            throw new AspectImporterException("Failed to parse aspect.json - invalid format, not correct json");
        }
    }

    private List<BaseStructures.Script> getScripts() throws IOException {
        Path p = rootPath.resolve("scripts");
        List<File> files = IOUtils.getByExtension(p, "petpet");
        List<BaseStructures.Script> result = new ArrayList<>(files.size());
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length()-".petpet".length());
            String code = Files.readString(f.toPath());
            result.add(new BaseStructures.Script(name, code));
        }
        return result;
    }

    private LinkedHashMap<String, BaseStructures.Texture> getTextures() throws IOException {
        Path p = rootPath.resolve("textures");
        List<File> files = IOUtils.getByExtension(p, "png");
        LinkedHashMap<String, BaseStructures.Texture> texes = new LinkedHashMap<>();
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length()-".png".length()); //strip .png
            byte[] bytes = Files.readAllBytes(f.toPath());
            texes.put(name, new BaseStructures.Texture(name, bytes));
        }
        textureOffset += texes.size();
        return texes;
    }

    private List<BaseStructures.ModelPartStructure> compileModels(Path p) throws IOException, AspectImporterException {
        List<File> files = IOUtils.getByExtension(p, "bbmodel");
        List<BaseStructures.ModelPartStructure> bbmodels = new ArrayList<>(files.size());
        for (File f : files) {
            //Read to a bbmodel object, and handle it
            String str = Files.readString(f.toPath());
            JsonStructures.BBModel bbmodel = gson.fromJson(str, JsonStructures.BBModel.class);
            bbmodel.fixedOutliner = bbmodel.getGson().fromJson(bbmodel.outliner, JsonStructures.Part[].class);
            String fileName = f.getName().substring(0, f.getName().length() - ".bbmodel".length()); //remove .bbmodel
            bbmodels.add(handleBBModel(bbmodel, fileName));
        }
        return bbmodels;
    }

    private BaseStructures.ModelPartStructure getEntityRoot() throws IOException, AspectImporterException {
        Path p = rootPath.resolve("entity");
        List<BaseStructures.ModelPartStructure> bbmodels = compileModels(p);
        return new BaseStructures.ModelPartStructure(
                "entity", new Vector3f(), new Vector3f(), new Vector3f(), true,
                bbmodels, AspectModelPart.ModelPartType.GROUP, null
        );
    }

    private BaseStructures.ModelPartStructure getHudRoot() throws IOException, AspectImporterException {
        Path p = rootPath.resolve("hud");
        List<BaseStructures.ModelPartStructure> bbmodels = compileModels(p);
        return new BaseStructures.ModelPartStructure(
                "hud", new Vector3f(), new Vector3f(), new Vector3f(), true,
                bbmodels, AspectModelPart.ModelPartType.GROUP, null
        );
    }

    private List<BaseStructures.ModelPartStructure> getWorldRoots() throws IOException, AspectImporterException {
        Path p = rootPath.resolve("world");
        return compileModels(p);
    }

    /**
     * "Handles" the given bbmodel file and returns a model part.
     * May also modify the state of this class in the process, adding
     * new textures or other data.
     */
    private BaseStructures.ModelPartStructure handleBBModel(JsonStructures.BBModel model, String fileName) throws AspectImporterException {

        /*
        Mapping explanation:
        Cube faces inside blockbench have numbers like 0, 1, 2, ... etc.
        These textures refer to the blockbench textures *inside the model*. So 0 means the first texture in the
        bbmodel's list. However, we want to convert these to a global index, since we want all textures from all
        bbmodels inside one big list. A "0" in one bbmodel might refer to the 11th texture in the overall aspect,
        so we make a "mapping" from 0 -> 10 for that bbmodel.
         */

        //Process json's textures and create a mapping.
        int numNewTextures = 0;
        List<Integer> jsonToGlobalTextureMapper = new ArrayList<>();
        for (JsonStructures.Texture jsonTexture : model.textures) {
            if (textures.containsKey(jsonTexture.strippedName())) {
                //If a texture of the same name is loaded globally, create a mapping
                jsonToGlobalTextureMapper.add(indexOfKey(textures, jsonTexture.strippedName()));
            } else {
                //Otherwise, create mapping using the texture offset, and store texture in main list
                textures.put(fileName + "/ASPECT_GENERATED" + numNewTextures, jsonTexture.toBaseStructure());
                jsonToGlobalTextureMapper.add(numNewTextures + textureOffset);
                numNewTextures++;
            }
        }
        textureOffset += numNewTextures;

        //Gather children
        List<BaseStructures.ModelPartStructure> children = new ArrayList<>(model.fixedOutliner.length);
        for (JsonStructures.Part p : model.fixedOutliner)
            children.add(p.toBaseStructure(jsonToGlobalTextureMapper, model.resolution));

        //Create final model part
        return new BaseStructures.ModelPartStructure(
                fileName, new Vector3f(), new Vector3f(), new Vector3f(),
                true, children, AspectModelPart.ModelPartType.GROUP,
                null
        );
    }

    private static <K, V>  int indexOfKey(LinkedHashMap<K, V> map, K key) {
        int i = 0;
        for (K elem : map.keySet()) {
            if (elem.equals(key))
                return i;
            i++;
        }
        return -1;
    }

    public static class AspectImporterException extends Exception {
        public AspectImporterException(String message) {
            super(message);
        }
    }

}
