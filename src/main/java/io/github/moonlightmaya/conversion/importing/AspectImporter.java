package io.github.moonlightmaya.conversion.importing;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    private BaseStructures.AspectStructure result;

    public AspectImporter(Path aspectFolder) {
        this.rootPath = aspectFolder;
        try {
            //Read the global textures to here
            textures = getTextures();
            //Read scripts
            //scripts = getScripts();
            //Get entity model parts:
            BaseStructures.ModelPartStructure entityRoot = getEntityModels();

            result = new BaseStructures.AspectStructure(entityRoot, List.of(), null, null);
        } catch (Exception e) {
            error = e;
        }
    }

    public BaseStructures.AspectStructure getImportResult() throws Throwable {
        if (error != null) throw error;
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
        return texes;
    }

    private BaseStructures.ModelPartStructure getEntityModels() throws IOException {
        Path p = rootPath.resolve("entity");
        List<File> files = IOUtils.getByExtension(p, "bbmodel");
        List<BaseStructures.ModelPartStructure> bbmodels = new ArrayList<>(files.size());
        for (File f : files) {
            //Read to a bbmodel object, and handle it
            String str = Files.readString(f.toPath());
            JsonStructures.BBModel bbmodel = gson.fromJson(str, JsonStructures.BBModel.class);
            bbmodel.fixedOutliner = bbmodel.getGson().fromJson(bbmodel.outliner, JsonStructures.Part[].class);
            bbmodels.add(handleBBModel(bbmodel, f.getName()));
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
    private BaseStructures.ModelPartStructure handleBBModel(JsonStructures.BBModel model, String fileName) {
        return new BaseStructures.ModelPartStructure(
                fileName, new Vector3f(), new Vector3f(), new Vector3f(), null,
                Arrays.stream(model.fixedOutliner).map(JsonStructures.Part::toBaseStructure).collect(Collectors.toList()),
                AspectModelPart.ModelPartType.GROUP,
                null
        );
    }

}
