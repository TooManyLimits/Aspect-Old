package io.github.moonlightmaya.conversion.importing;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.moonlightmaya.AspectModelPart;
import io.github.moonlightmaya.conversion.BaseStructures;
import io.github.moonlightmaya.util.IOUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
    private Throwable error = null;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Vector3f.class, JsonStructures.Vector3fDeserializer.INSTANCE)
            .registerTypeAdapter(Vector4f.class, JsonStructures.Vector4fDeserializer.INSTANCE)
            .setPrettyPrinting().create();

    private LinkedHashMap<String, BaseStructures.Texture> textures;
    private int textureOffset;

    public AspectImporter(Path aspectFolder) {
        this.rootPath = aspectFolder;
    }

    public CompletableFuture<BaseStructures.AspectStructure> doImport() {
        CompletableFuture<BaseStructures.AspectStructure> result = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                if (!Files.exists(rootPath))
                    throw new AspectImporterException("Folder " + rootPath + " does not exist");

                if (!Files.exists(rootPath.resolve("aspect.json")))
                    throw new AspectImporterException("Folder " + rootPath + " has no aspect.json");

                //Read the globally shared textures to here
                textures = getTextures();
                //Read scripts
                //scripts = getScripts();
                //Get entity model parts:
                BaseStructures.ModelPartStructure entityRoot = getEntityModels();

                result.complete(new BaseStructures.AspectStructure(
                        entityRoot, List.of(),
                        Lists.newArrayList(textures.values()),
                        new ArrayList<>()
                ));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private LinkedHashMap<String, BaseStructures.Texture> getTextures() throws IOException {
        Path p = rootPath.resolve("textures");
        List<File> files = IOUtils.getByExtension(p, "png");
        LinkedHashMap<String, BaseStructures.Texture> texes = new LinkedHashMap<>();
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length()-4); //strip .png
            byte[] bytes = Files.readAllBytes(f.toPath());
            texes.put(name, new BaseStructures.Texture(name, bytes));
        }
        textureOffset += texes.size();
        return texes;
    }

    private BaseStructures.ModelPartStructure getEntityModels() throws IOException, AspectImporterException {
        Path p = rootPath.resolve("entity");
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
        return new BaseStructures.ModelPartStructure(
                "entity", new Vector3f(), new Vector3f(), new Vector3f(), true,
                bbmodels, AspectModelPart.ModelPartType.GROUP, null
        );
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
            if (textures.containsKey(jsonTexture.name())) {
                //If a texture of the same name is loaded globally, create a mapping
                jsonToGlobalTextureMapper.add(indexOfKey(textures, jsonTexture.name()));
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
