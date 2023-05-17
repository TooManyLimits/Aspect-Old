package io.github.moonlightmaya.game_interfaces;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.moonlightmaya.AspectMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Handles the Aspect configs
 */
public class AspectConfig {

    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve(AspectMod.MODID + ".json").toFile();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final LinkedHashMap<String, Setting<?>> SETTINGS_LIST = new LinkedHashMap<>(); //Keep them in order

    /**
     * Settings
     */

    //Gui path relative to the .minecraft/aspect/guis folder
    //If empty, uses the default gui aspect included with the mod
    public static final Setting<String> GUI_PATH = new Setting<>("gui_path", "");



    /**
     * Methods
     */

    public static void save() {
        try {
            //Create config file if not already exists
            if (FILE.createNewFile())
                AspectMod.LOGGER.info("Did not find Aspect config file, creating");
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, Setting<?>> setting : SETTINGS_LIST.entrySet())
                obj.add(setting.getKey(), GSON.toJsonTree(setting.getValue()));
            String jsonString = GSON.toJson(obj);
            try(FileWriter writer = new FileWriter(FILE)) {
                writer.write(jsonString);
            }
        } catch (Exception e) {
            AspectMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static void load() {
        try {
            if (Files.exists(FILE.toPath())) {
                JsonObject json = GSON.fromJson(Files.readString(FILE.toPath()), JsonObject.class);
                for (Map.Entry<String, Setting<?>> setting : SETTINGS_LIST.entrySet()) {
                    try {
                        if (json.has(setting.getKey()))
                            setting.getValue().trySet(json.get(setting.getKey()));
                    } catch (ClassCastException e) {
                        AspectMod.LOGGER.error("Failed to load config for setting \"" + setting.getKey() + "\"", e);
                    }
                }
            }
        } catch (Exception e) {
            AspectMod.LOGGER.error("Failed to load config", e);
        }
    }

    public static class Setting<T> {
        private T value;
        private Setting(String key, T defaultValue) {
            this.value = defaultValue;
            SETTINGS_LIST.put(key, this);
        }
        public void set(T val) {
            this.value = val;
            save(); //Save the settings after changing one :P
        }
        private void trySet(Object val) throws ClassCastException {
            //No need to save, this is only used when loading
            this.value = (T) val;
        }
        public T get() {
            return this.value;
        }
    }

}
