package io.github.moonlightmaya.manage.data.importing;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.util.IOUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;
import petpet.types.PetPetList;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
                BaseStructures.ModelPartStructure entityRoot = getRootForType("entity");
                //Get world model parts:
                List<BaseStructures.ModelPartStructure> worldRoots = getWorldRoots();
                //Get hud model parts:
                BaseStructures.ModelPartStructure hudRoot = getRootForType("hud");

                //Create the base aspect structure
                BaseStructures.AspectStructure aspect = new BaseStructures.AspectStructure(
                    metadata,
                    entityRoot, worldRoots, hudRoot,
                    Lists.newArrayList(textures.values()),
                    scripts
                );

                //Save the serialized form to the file if needed
                if (true) { //Later may add a flag in metadata to disable saving this
                    String name = this.rootPath.toFile().getName() + ".aspect";
                    try (FileOutputStream out = new FileOutputStream(this.rootPath.resolve(name).toFile())) {
                        DataOutputStream dos = new DataOutputStream(out);
                        aspect.write(dos);
                    }
                }

                result.complete(aspect);
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


    private List<BaseStructures.Script> getScriptsIn(Path p, String prefix) throws IOException {
        //Get petpet files
        List<File> files = IOUtils.getByExtension(p, "petpet");
        List<BaseStructures.Script> result = new ArrayList<>(files.size());
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length()-".petpet".length());
            String code = Files.readString(f.toPath());
            result.add(new BaseStructures.Script(name, code));
        }

        //Get subfolders
        List<File> subfolders = IOUtils.getSubFolders(p);
        for (File subfolder : subfolders) {
            String subfolderPrefix = prefix + "/" + subfolder.getName();
            List<BaseStructures.Script> subfolderScripts = getScriptsIn(subfolder.toPath(), subfolderPrefix);
            result.addAll(subfolderScripts);
        }

        return result;
    }

    private List<BaseStructures.Script> getScripts() throws IOException {
        //If main.petpet exists in the root, then add it directly,
        //and don't check for a scripts directory
        Path mainPath = rootPath.resolve("main.petpet");
        if (Files.exists(mainPath) && !mainPath.toFile().isDirectory()) {
            ArrayList<BaseStructures.Script> scripts = new ArrayList<>();
            scripts.add(new BaseStructures.Script("main", Files.readString(mainPath)));
            return scripts;
        }

        //If you want multiple scripts, include main.petpet in the scripts folder
        //Get all scripts that exist in the scripts folder
        Path folderPath = rootPath.resolve("scripts");
        if (Files.exists(folderPath) && folderPath.toFile().isDirectory()) {
            return getScriptsIn(folderPath, "");
        }

        return List.of();
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

    /**
     * Gets a model part structure from a .bbmodel file
     * Assumes the given file is a .bbmodel
     */
    private BaseStructures.ModelPartStructure parseBBModel(File f) throws IOException, AspectImporterException {
        String str = Files.readString(f.toPath());
        JsonStructures.BBModel bbmodel = gson.fromJson(str, JsonStructures.BBModel.class);
        bbmodel.fixedOutliner = bbmodel.getGson().fromJson(bbmodel.outliner, JsonStructures.Part[].class);
        return handleBBModel(bbmodel, f.toPath());
    }

    private BaseStructures.ModelPartStructure compileModels(Path p, String name) throws IOException, AspectImporterException {

        List<File> bbmodelFiles = IOUtils.getByExtension(p, "bbmodel");
        List<BaseStructures.ModelPartStructure> bbmodels = new ArrayList<>(bbmodelFiles.size());
        for (File f : bbmodelFiles)
            bbmodels.add(parseBBModel(f));

        List<File> subfolders = IOUtils.getSubFolders(p);
        for (File subfolder : subfolders) {
            BaseStructures.ModelPartStructure subFolderResult = compileModels(subfolder.toPath(), subfolder.getName());
            Optional<BaseStructures.ModelPartStructure> alreadyFound = bbmodels.stream().filter(s -> s.name().equals(subfolder.getName())).findFirst();

            if (alreadyFound.isPresent()) {
                //If there's already a model with this name, then merge the children of this part into that one
                alreadyFound.get().children().addAll(subFolderResult.children());
            } else {
                //Otherwise, just append this to the bbmodels list
                bbmodels.add(subFolderResult);
            }
        }

        return new BaseStructures.ModelPartStructure(
                name, new Vector3f(), new Vector3f(), new Vector3f(), true,
                bbmodels, AspectModelPart.ModelPartType.GROUP, null, null
        );
    }

    private BaseStructures.ModelPartStructure getRootForType(String name) throws IOException, AspectImporterException {
        //Look for normal .bbmodel file
        Path p = rootPath.resolve(name + ".bbmodel");
        if (Files.exists(p) && !p.toFile().isDirectory()) {
            return parseBBModel(rootPath.resolve(name + ".bbmodel").toFile());
        }
        //That didn't exist, so expect folder format
        p = rootPath.resolve(name);
        if (Files.exists(p) && p.toFile().isDirectory()) {
            return compileModels(p, name);
        }
        //Neither exists, so return an empty part
        return new BaseStructures.ModelPartStructure(
                name, new Vector3f(), new Vector3f(), new Vector3f(), true,
                new ArrayList<>(0), AspectModelPart.ModelPartType.GROUP, null, null
        );
    }

    private List<BaseStructures.ModelPartStructure> getWorldRoots() throws IOException, AspectImporterException {
        //If world.bbmodel exists, return a list containing only that bbmodel
        Path p = rootPath.resolve("world.bbmodel");
        if (Files.exists(p) && !p.toFile().isDirectory()) {
            ArrayList<BaseStructures.ModelPartStructure> result = new ArrayList<>(1);
            result.add(parseBBModel(p.toFile()));
            return result;
        }
        //Otherwise, if it doesn't exist, get root and return children
        BaseStructures.ModelPartStructure worldStructure = getRootForType("world");
        return worldStructure.children();
    }

    /**
     * "Handles" the given bbmodel file and returns a model part.
     * May also modify the state of this class in the process, adding
     * new textures or other data.
     */
    private BaseStructures.ModelPartStructure handleBBModel(JsonStructures.BBModel model, Path filePath) throws AspectImporterException {

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
            //Just don't name your textures in the specific format "{fileName}/ASPECT_GENERATED{index}",
            //if you don't want this to mess with you. Really shouldn't be too hard.
            if (textures.containsKey(jsonTexture.strippedName())) {
                //If a texture of the same name is loaded globally, create a mapping
                jsonToGlobalTextureMapper.add(indexOfKey(textures, jsonTexture.strippedName()));
            } else {
                //Otherwise, create mapping using the texture offset, and store texture in main list
                //Texture name contains the path to the bbmodel file concatenated with the name
                String relativePath = this.rootPath.relativize(filePath).toString();
                relativePath = relativePath.substring(0, relativePath.length() - ".bbmodel".length());
                //replace backslashes (again, if this breaks your files, then don't name them with backslashes :skull:)
                relativePath = relativePath.replace("\\", "/");
                AspectMod.LOGGER.debug("Relative texture path name: {}", relativePath);

                textures.put(relativePath + "/ASPECT_GENERATED" + numNewTextures, jsonTexture.toBaseStructure(relativePath));
                jsonToGlobalTextureMapper.add(numNewTextures + textureOffset);
                numNewTextures++;
            }
        }
        textureOffset += numNewTextures;

        //Gather children
        List<BaseStructures.ModelPartStructure> children = new ArrayList<>(model.fixedOutliner.length);
        for (JsonStructures.Part p : model.fixedOutliner)
            children.add(p.toBaseStructure(jsonToGlobalTextureMapper, model.resolution));

        //Construct name of part
        File partFile = filePath.toFile();
        String partName = partFile.getName().substring(0, partFile.getName().length() - ".bbmodel".length());

        //Create final model part
        return new BaseStructures.ModelPartStructure(
                partName, new Vector3f(), new Vector3f(), new Vector3f(),
                true, children, AspectModelPart.ModelPartType.GROUP,
                null, null
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