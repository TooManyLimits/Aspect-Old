package io.github.moonlightmaya.script;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.script.apis.math.Vectors;
import io.github.moonlightmaya.script.events.AspectEvent;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.ScriptUtils;
import io.github.moonlightmaya.vanilla.VanillaPart;
import io.github.moonlightmaya.vanilla.VanillaRenderer;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;
import petpet.lang.compile.Compiler;
import petpet.lang.lex.Lexer;
import petpet.lang.parse.Parser;
import petpet.lang.run.JavaFunction;
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
    private JavaFunction requireFunction;

    private EventHandler eventHandler;

    private Throwable error; //Null until an error occurs

    private boolean shouldPrintToChat = true; //Whether this Aspect should print its output to chat

    /**
     * When first creating the script handler, we will compile
     * all the scripts, and potentially report errors.
     * Then, we run the "main" script.
     */
    public AspectScriptHandler(Aspect aspect) {
        this.aspect = aspect;

        //Create new instance
        instance = new PetPetInstance();

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

    PetPetTable<String, Object> modelsTable = new PetPetTable<>();

    /**
     * When the user of the Aspect this script handler is for
     * first loads in, we add the related fields into the script environment.
     */
    public void onEntityFirstLoad() {
        instance.setGlobal("vanilla", aspect.vanillaRenderer);
        modelsTable.put("entity", aspect.entityRoot);
    }

    /**
     * Register the different types which are allowed
     */
    private void registerTypes() {
        //Math
        instance.registerClass(Vector2d.class, Vectors.VEC_2);
        instance.registerClass(Vector3d.class, Vectors.VEC_3);
        instance.registerClass(Vector4d.class, Vectors.VEC_4);

        //Events
        instance.registerClass(AspectEvent.class, PetPetReflector.reflect(AspectEvent.class, "Event"));

        //Model Parts
        instance.registerClass(WorldRootModelPart.class, PetPetReflector.reflect(WorldRootModelPart.class, "WorldRootModelPart"));
        instance.registerClass(AspectModelPart.class, PetPetReflector.reflect(AspectModelPart.class, "ModelPart"));

        //Vanilla renderer
        instance.registerClass(VanillaRenderer.class, PetPetReflector.reflect(VanillaRenderer.class, "VanillaRenderer"));
        instance.registerClass(VanillaPart.class, PetPetReflector.reflect(VanillaPart.class, "VanillaPart"));
    }

    /**
     * Set all the global variables that are needed
     */
    private void setupGlobals() {
        //Print functions, if should print
        instance.setGlobal("print", shouldPrintToChat ? DisplayUtils.PRINT_FUNCTION : ScriptUtils.DO_NOTHING_1_ARG);
        instance.setGlobal("log", shouldPrintToChat ? DisplayUtils.PRINT_FUNCTION : ScriptUtils.DO_NOTHING_1_ARG);

        //Require
        requireFunction = setupRequire();
        instance.setGlobal("require", requireFunction);

        //Models
        instance.setGlobal("models", modelsTable);

        //World roots in models
        PetPetList<WorldRootModelPart> worldRoots = new PetPetList<>(aspect.worldRoots.size());
        worldRoots.addAll(aspect.worldRoots);
        modelsTable.put("world", worldRoots);

        //Events
        //Code for events is all inside EventHandler, which
        //deals with creating the events and also adding it
        //as a global variable
        eventHandler = new EventHandler(instance);
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

    public Object callEvent(String eventName, Object... args) {
        if (isErrored()) return null;
        try {
            return eventHandler.callEvent(eventName, args);
        } catch (Throwable t) {
            error = t;
            DisplayUtils.displayError(t.getMessage(), shouldPrintToChat);
            return null;
        }
    }

    public boolean isErrored() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

}
