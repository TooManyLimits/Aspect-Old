package io.github.moonlightmaya.script;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectManager;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.script.apis.AspectAPI;
import io.github.moonlightmaya.script.apis.HostAPI;
import io.github.moonlightmaya.script.apis.ItemStackAPI;
import io.github.moonlightmaya.script.apis.entity.EntityAPI;
import io.github.moonlightmaya.script.apis.entity.LivingEntityAPI;
import io.github.moonlightmaya.script.apis.entity.PlayerAPI;
import io.github.moonlightmaya.script.apis.world.BiomeAPI;
import io.github.moonlightmaya.script.apis.world.BlockStateAPI;
import io.github.moonlightmaya.script.apis.world.DimensionAPI;
import io.github.moonlightmaya.script.apis.world.WorldAPI;
import io.github.moonlightmaya.script.apis.math.Matrices;
import io.github.moonlightmaya.script.apis.math.Vectors;
import io.github.moonlightmaya.script.events.AspectEvent;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.vanilla.VanillaPart;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;
import petpet.lang.compile.Compiler;
import petpet.lang.lex.Lexer;
import petpet.lang.parse.Parser;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.lang.run.PetPetClosure;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The interface between an Aspect and its scripts.
 * This is how events are called, for instance.
 * It provides a layer of abstraction over the actual
 * calling of events and running of scripts.
 */
public class AspectScriptHandler {

    private final Aspect aspect;
    private final PetPetInstance instance;

    private final Map<String, PetPetClosure> compiledScripts;


    private Throwable error; //Null until an error occurs

    private boolean shouldPrintToChat = true; //Whether this Aspect should print its output to chat


    /**
     * Saved important script variables
     */
    private JavaFunction requireFunction;
    private EventHandler eventHandler;
    private HostAPI hostAPI;


    /**
     * When first creating the script handler, we will compile
     * all the scripts, and potentially report errors.
     * Then, we run the "main" script.
     */
    public AspectScriptHandler(Aspect aspect) {
        this.aspect = aspect;

        //Create new instance
        instance = new PetPetInstance();

//        instance.debugBytecode = true;

        //Compile all the scripts
        compiledScripts = new HashMap<>();
        compileScripts();

        //Register the types
        registerTypes();

        //Set up the globals
        setupGlobals();
    }

    /**
     * Run the "main" script!
     */
    public void runMain() {
        //If there are no compiled scripts, just do nothing
        if (compiledScripts.size() > 0) {
            String main = "main"; //Maybe changeable later
            try {
                requireFunction.call(main);
            } catch (Throwable t) {
                error = t;
                DisplayUtils.displayError(t.getMessage(), shouldPrintToChat);
            }
        }
    }

    public Object runCode(String name, String code) throws Exception {
        return instance.runScript(name, code);
    }

    PetPetTable<String, Object> modelsTable = new PetPetTable<>();

    /**
     * When the user of the Aspect this script handler is for
     * first loads in, we add the related fields into the script environment.
     */
    public void onEntityFirstLoad() {
        instance.setGlobal("vanilla", aspect.vanillaRenderer);
        modelsTable.put("entity", aspect.entityRoot);
        callEvent(EventHandler.USER_INIT);
    }

    /**
     * Register the different types which are allowed
     */
    private void registerTypes() {
        //Math
        instance.registerClass(Vector2d.class, Vectors.VEC_2.copy().makeEditable());
        instance.registerClass(Vector3d.class, Vectors.VEC_3.copy().makeEditable());
        instance.registerClass(Vector4d.class, Vectors.VEC_4.copy().makeEditable());
        instance.registerClass(Matrix2d.class, Matrices.MAT_2.copy().makeEditable());
        instance.registerClass(Matrix3d.class, Matrices.MAT_3.copy().makeEditable());
        instance.registerClass(Matrix4d.class, Matrices.MAT_4.copy().makeEditable());

        //Events
        instance.registerClass(AspectEvent.class, PetPetReflector.reflect(AspectEvent.class, "Event").copy().makeEditable());

        //Model Parts
        PetPetClass modelPartClass = PetPetReflector.reflect(AspectModelPart.class, "ModelPart").copy().makeEditable();
        instance.registerClass(AspectModelPart.class, modelPartClass);
        instance.registerClass(WorldRootModelPart.class, PetPetReflector.reflect(WorldRootModelPart.class, "WorldRootModelPart").copy().makeEditable().setParent(modelPartClass));

        //Vanilla renderer
        instance.registerClass(VanillaRenderer.class, PetPetReflector.reflect(VanillaRenderer.class, "VanillaRenderer").copy().makeEditable());
        instance.registerClass(VanillaPart.class, PetPetReflector.reflect(VanillaPart.class, "VanillaPart").copy().makeEditable());

        //World
        instance.registerClass(ClientWorld.class, WorldAPI.WORLD_CLASS.copy().makeEditable());
        instance.registerClass(BlockState.class, BlockStateAPI.BLOCK_STATE_CLASS.copy().makeEditable());
        instance.registerClass(ItemStack.class, ItemStackAPI.ITEMSTACK_CLASS.copy().makeEditable());
        instance.registerClass(DimensionType.class, DimensionAPI.DIMENSION_CLASS.copy().makeEditable());
        instance.registerClass(Biome.class, BiomeAPI.BIOME_CLASS.copy().makeEditable());

        //Entity
        PetPetClass entityClass = EntityAPI.ENTITY_CLASS.copy().makeEditable();
        instance.registerClass(Entity.class, entityClass);
        PetPetClass livingEntityClass = LivingEntityAPI.LIVING_ENTITY_CLASS.copy().makeEditable().setParent(entityClass);
        instance.registerClass(LivingEntity.class, livingEntityClass);
        instance.registerClass(PlayerEntity.class, PlayerAPI.PLAYER_CLASS.copy().makeEditable().setParent(livingEntityClass));

        //Aspect (Only the API. For explanation of why a wrapper API is needed here instead of the raw object, see AspectAPI.class.)
        instance.registerClass(AspectAPI.class, PetPetReflector.reflect(AspectAPI.class, "Aspect").copy().makeEditable());

        //Host
        instance.registerClass(HostAPI.class, PetPetReflector.reflect(HostAPI.class, "Host").copy().makeEditable());

        //Special permission methods for prior APIs
        entityClass.addMethod("getAspect", EntityAPI.getGetAspectMethod(aspect));
    }

    /**
     * Set all the global variables that are needed
     */
    private void setupGlobals() {
        //Print functions, if should print
        JavaFunction printFunc = getPrintFunction();
        setGlobal("print", printFunc);
        setGlobal("log", printFunc);

        //Math structures
        setGlobal("vec2", Vectors.VEC_2_CREATE);
        setGlobal("vec3", Vectors.VEC_3_CREATE);
        setGlobal("vec4", Vectors.VEC_4_CREATE);
        setGlobal("mat2", Matrices.MAT_2_CREATE);
        setGlobal("mat3", Matrices.MAT_3_CREATE);
        setGlobal("mat4", Matrices.MAT_4_CREATE);

        //Require
        requireFunction = setupRequire();
        setGlobal("require", requireFunction);

        //Models
        setGlobal("models", modelsTable);

        //World roots in models
        PetPetTable<String, WorldRootModelPart> worldRoots = new PetPetTable<>(aspect.worldRoots.size());
        for (WorldRootModelPart worldRootModelPart : aspect.worldRoots)
            worldRoots.put(worldRootModelPart.name, worldRootModelPart);
        modelsTable.put("world", worldRoots);
        modelsTable.put("hud", aspect.hudRoot);

        //Events
        //Code for events is all inside EventHandler, which
        //deals with creating the events and also adding it
        //as a global variable
        eventHandler = new EventHandler(instance);

        //Aspect api
        setGlobal("aspect", new AspectAPI(aspect, true));

        //Host api
        hostAPI = new HostAPI(aspect);
        setGlobal("host", hostAPI);

        //Other APIs not shown here:

        //world api: set during aspect.tick()
        //user api: set during aspect.tick()
        //vanilla api: set when the user's entity first loads in
    }

    /**
     * Attempts to compile every script in the Aspect.
     * If any fails, it will report the message in chat, and then
     * throw a RuntimeException, aborting the rest of the Aspect load.
     */
    private void compileScripts() {
        for (Map.Entry<String, String> entry : aspect.scripts.entrySet()) {
            String name = entry.getKey();
            String source = entry.getValue();
            try {
                PetPetClosure compiled = instance.compile(name, source);
                compiledScripts.put(name, compiled);
            } catch (Lexer.LexingException e) {
                DisplayUtils.displayError("Lexing error in script " + name + ": " + e.getMessage(), true);
                throw new RuntimeException("Failed to load script " + name, e);
            } catch (Parser.ParserException e) {
                DisplayUtils.displayError("Parsing error in script " + name + ": " + e.getMessage(), true);
                throw new RuntimeException("Failed to load script " + name, e);
            } catch (Compiler.CompilationException e) {
                DisplayUtils.displayError("Compilation error in script " + name + ": " + e.getMessage(), true);
                throw new RuntimeException("Failed to load script " + name, e);
            }
        }
    }

    /**
     * Generates the require function for these scripts
     * and returns it.
     */
    private JavaFunction setupRequire() {
        Map<String, Object> savedOutputs = new HashMap<>();
        Set<String> inProgress = new HashSet<>();
        return new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object arg) {
                if (arg instanceof String name) {
                    //If still in progress and we require it again, then we have a circular require
                    if (inProgress.contains(name))
                        throw new PetPetException("Discovered circular require() call, asking for \"" + name + "\"");

                    //If we've already required this before, then return the saved output value
                    if (savedOutputs.containsKey(name))
                        return savedOutputs.get(name);

                    //Otherwise, make the call and save the value
                    inProgress.add(name);
                    PetPetClosure compiled = compiledScripts.get(name);
                    if (compiled == null)
                        throw new PetPetException("Tried to require nonexistent script \"" + name + "\"");
                    Object result = compiled.call();
                    inProgress.remove(name);
                    savedOutputs.put(name, result);
                    return result;
                } else {
                    //Type handling
                    throw new PetPetException("Attempt to call require() with non-string argument " + arg);
                }
            }
        };
    }

    /**
     * Create a print function
     */
    private JavaFunction getPrintFunction() {
        return new JavaFunction(true, 1) {
            @Override
            public Object invoke(Object o) {
                if (shouldPrintToChat)
                    DisplayUtils.displayPetPetMessage(getStringFor(o));
                return null;
            }
        };
    }

    public void setGlobal(String name, Object o) {
        instance.setGlobal(name, o);
    }

    public String getStringFor(Object o) {
        return instance.interpreter.getString(o);
    }

    public void callEvent(String eventName, Object... args) {
        if (isErrored()) return;
        try {
            eventHandler.callEvent(eventName, args);
        } catch (Throwable t) {
            error = t;
            DisplayUtils.displayError(t.getMessage(), shouldPrintToChat);
        }
    }

    /**
     * Return true if should cancel
     */
    public boolean callEventCancellable(String eventName, Object... args) {
        if (isErrored()) return false;
        try {
            return eventHandler.callEventCancellable(eventName, args);
        } catch (Throwable t) {
            error = t;
            DisplayUtils.displayError(t.getMessage(), shouldPrintToChat);
            return false;
        }
    }

    /**
     * Calls the event in the "piped" format. If the aspect
     * is errored, just returns the provided arg back.
     */
    public Object callEventPiped(String eventName, Object arg) {
        if (isErrored()) return arg;
        try {
            return eventHandler.callEventPiped(eventName, arg);
        } catch (Throwable t) {
            error = t;
            DisplayUtils.displayError(t.getMessage(), shouldPrintToChat);
        }
        return arg;
    }

    public boolean isErrored() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    /**
     * Return the host api for this aspect, but only if it has
     * isHost true. If not true, then it will return null.
     * Never returns a host api without isHost permissions.
     */
    public @Nullable HostAPI getHostAPI() {
        return hostAPI.isHost() ? hostAPI : null;
    }

}
