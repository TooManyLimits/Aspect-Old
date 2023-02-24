package io.github.moonlightmaya.conversion.importing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.moonlightmaya.conversion.BaseStructures;
import io.github.moonlightmaya.util.IOUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Deals with the task of importing an Aspect
 * from the file system. Calling the constructor
 * with a path will perform this operation.
 * Consider running it on a different thread.
 */
public class AspectImporter {

    private final Path rootPath;
    private Throwable error = null;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Vector3f.class, JsonStructures.Vector3fDeserializer.INSTANCE)
            .registerTypeAdapter(Vector4f.class, JsonStructures.Vector4fDeserializer.INSTANCE)
            .setPrettyPrinting().create();

    private LinkedHashMap<String, BaseStructures.Texture> textures;

    public AspectImporter(Path aspectFolder) {
        this.rootPath = aspectFolder;
        try {
            //Read the global textures to here
            textures = getTextures();
            //Read scripts
            //scripts = getScripts();
            //Get entity model parts:



        } catch (Exception e) {
            error = e;
        }
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
        for (File f : files) {
            //Read to a bbmodel object, and handle it
            String str = Files.readString(f.toPath());
            JsonStructures.BBModel bbmodel = gson.fromJson(str, JsonStructures.BBModel.class);
            bbmodel.fixedOutliner = bbmodel.getGson().fromJson(bbmodel.outliner, JsonStructures.Part[].class);
        }
    }

    /**
     * "Handles" the given bbmodel file and returns a model part.
     * May also modify the state of this class in the process, adding
     * new textures or other data.
     */
    private BaseStructures.ModelPartStructure handleBBModel(JsonStructures.BBModel model) {

    }

}
