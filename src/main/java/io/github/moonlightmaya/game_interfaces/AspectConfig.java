package io.github.moonlightmaya.game_interfaces;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.moonlightmaya.AspectMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the Aspect configs
 */
public class AspectConfig {

    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve(AspectMod.MODID + ".json").toFile();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<Setting<?>> SETTINGS_LIST = new ArrayList<>(); //Keep them in order

    /**
     * Settings
     */

    //Gui path relative to the .minecraft/aspect/guis folder
    public static final Setting<String> GUI_PATH = new Setting<>("gui_path", "");


    /**
     * Methods
     */

    public void save() {
        try {
            //Create config file if not already exists
            FILE.createNewFile();
            JsonObject obj = new JsonObject();
            for (Setting<?> setting : SETTINGS_LIST)
                obj.add(setting.key, GSON.toJsonTree(setting.value));
            String jsonString = GSON.toJson(obj);
            try(FileWriter writer = new FileWriter(FILE)) {
                writer.write(jsonString);
            }
        } catch (Exception e) {
            AspectMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static class Setting<T> {
        private final String key;
        private T value;
        private final T defaultValue;
        private Setting(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            SETTINGS_LIST.add(this);
        }
        public void set(T val) {
            this.value = val;
        }
        public T get() {
            return this.value;
        }
    }

}
