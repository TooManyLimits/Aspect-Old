package io.github.moonlightmaya.game_interfaces;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moonlightmaya.AspectMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    //The max instructions allowed for any aspect at various phases
    //Eventually this will have more advanced options, but this is just here for now to prevent
    //infinite loops from freezing the game entirely
    public static final Setting<Integer> INIT_INSTRUCTIONS = new Setting<>("init_instructions", 10_000_000);
    public static final Setting<Integer> TICK_INSTRUCTIONS = new Setting<>("tick_instructions", 200_000);
    public static final Setting<Integer> RENDER_INSTRUCTIONS = new Setting<>("render_instructions", 50_000);

    /**
     * Methods
     */

    public static void save() {
        try {
            //Create config file if not already exists
            if (FILE.createNewFile())
                AspectMod.LOGGER.info("Did not find Aspect config file, creating");

            //For each setting, save it in a json object
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, Setting<?>> settingEntry : SETTINGS_LIST.entrySet()) {
                String settingName = settingEntry.getKey();
                Setting<?> setting = settingEntry.getValue();
                JsonElement jsonnedValue = GSON.toJsonTree(setting.get());
                obj.add(settingName, jsonnedValue);
            }


            //Write the json object to disk
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
                //Read the json file into an object
                JsonObject json = GSON.fromJson(Files.readString(FILE.toPath()), JsonObject.class);
                //For each setting, if the json file has that setting, read the value and store it
                for (Map.Entry<String, Setting<?>> settingEntry : SETTINGS_LIST.entrySet()) {
                    String settingName = settingEntry.getKey();
                    Setting<?> setting = settingEntry.getValue();
                    try {
                        if (json.has(settingName)) {
                            Object o = GSON.fromJson(json.get(settingName), setting.expectedClass);
                            setting.trySet(o);
                        }
                    } catch (ClassCastException e) {
                        AspectMod.LOGGER.error("Failed to load config for setting \"" + settingName + "\"", e);
                    }
                }
            }
        } catch (Exception e) {
            AspectMod.LOGGER.error("Failed to load config", e);
        }
    }

    public static class Setting<T> {
        private T value;
        private final Class<?> expectedClass; //because java sucks :p
        private Setting(String key, T defaultValue) {
            this.value = defaultValue;
            SETTINGS_LIST.put(key, this);

            if (defaultValue.getClass() == String.class)
                expectedClass = String.class;
            else
                expectedClass = void.class;
        }
        public void set(T val) {
            this.value = val;
            save(); //Save the settings after changing one :P
        }
        private void trySet(Object val) throws ClassCastException {
            if (val.getClass() != expectedClass)
                throw new ClassCastException("Expected " + expectedClass + ", got " + val.getClass());
            //No need to save, this is only used when loading
            this.value = (T) val;
        }
        public T get() {
            return this.value;
        }
    }

}
