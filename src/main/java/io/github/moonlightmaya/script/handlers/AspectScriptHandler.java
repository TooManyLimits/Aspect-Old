package io.github.moonlightmaya.script.handlers;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.model.animation.Animation;
import io.github.moonlightmaya.script.apis.*;
import io.github.moonlightmaya.script.apis.gui.ManagerAPI;
import io.github.moonlightmaya.script.apis.math.Quaternions;
import io.github.moonlightmaya.script.apis.math.Matrices;
import io.github.moonlightmaya.script.apis.math.Vectors;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.model.AspectTexture;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import petpet.external.PetPetInstance;
import petpet.lang.compile.Compiler;
import petpet.lang.lex.Lexer;
import petpet.lang.parse.Parser;
import petpet.lang.run.*;
import petpet.types.PetPetTable;

import java.io.InputStream;
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

    public final Aspect aspect;
    public final PetPetInstance instance;

    private final Map<String, PetPetClosure> compiledScripts;

    private boolean shouldPrintToChat = true; //Whether this Aspect should print its output to chat (true always atm)


    /**
     * Saved important script variables
     */
    private JavaFunction requireFunction;
    private EventHandler eventHandler;
    private @Nullable HostAPI hostAPI;


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
        WhitelistHandler.setupInstance(this);
        //Set up basic globals
        setupGlobals();
        //Run utils
        runUtils();

        //Set up extra (from other mods) globals
        ExtraGlobalHandler.setupInstance(this);
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
                error(t);
            }
        }
    }

    public Object runCode(String name, String code) throws Exception {
        return instance.runScript(name, code);
    }



    /**
     * When the user of the Aspect this script handler is for
     * first loads in, we add the related fields into the script environment.
     */
    public void onEntityFirstLoad() {
        instance.setGlobal("vanilla", aspect.vanillaRenderer);

        callEvent(EventHandler.USER_INIT);
    }

    /**
     * Set all the global variables that are needed
     */
    private void setupGlobals() {
        //Remove some petpet functions we don't want to give
        instance.interpreter.globals.remove("printStack");

        //Print functions, if should print
        JavaFunction printFunc = getPrintFunction();
        setGlobal("print", printFunc);
        setGlobal("log", printFunc);

        //Math constructors
        setGlobal("vec2", Vectors.VEC_2_CREATE);
        setGlobal("vec3", Vectors.VEC_3_CREATE);
        setGlobal("vec4", Vectors.VEC_4_CREATE);
        setGlobal("mat2", Matrices.MAT_2_CREATE);
        setGlobal("mat3", Matrices.MAT_3_CREATE);
        setGlobal("mat4", Matrices.MAT_4_CREATE);
        setGlobal("quat", Quaternions.QUAT_CREATE);

        //Classes (stored in table for convenience)
        PetPetTable<String, PetPetClass> classes = new PetPetTable<>();
        for (var clazz : instance.interpreter.classMap.values())
            classes.put(clazz.name, clazz);
        setGlobal("aspectClasses", classes);

        //Require
        requireFunction = setupRequire();
        setGlobal("require", requireFunction);

        //Textures
        PetPetTable<String, AspectTexture> texturesTable = new PetPetTable<>();
        for (AspectTexture tex : aspect.textures)
            texturesTable.put(tex.name(), tex);
        setGlobal("textures", texturesTable);

        //Animations
        PetPetTable<String, Animation> animationsTable = new PetPetTable<>();
        for (Animation anim : aspect.animations.values())
            animationsTable.put(anim.name, anim);
        setGlobal("animations", animationsTable);

        //Part roots in models table
        PetPetTable<String, Object> modelsTable = new PetPetTable<>();
        PetPetTable<String, WorldRootModelPart> worldRoots = new PetPetTable<>(aspect.worldRoots.size());
        for (WorldRootModelPart worldRootModelPart : aspect.worldRoots)
            worldRoots.put(worldRootModelPart.name, worldRootModelPart);
        modelsTable.put("world", worldRoots);
        modelsTable.put("hud", aspect.hudRoot);
        modelsTable.put("entity", aspect.entityRoot);
        setGlobal("models", modelsTable);

        //Events
        //Code for events is all inside EventHandler, which
        //deals with creating the events
        //
        //This constructor also adds the handler table as a global variable!
        eventHandler = new EventHandler(instance);

        //Misc apis
        setGlobal("aspect", new AspectAPI(aspect, true));
        setGlobal("client", new ClientAPI());
        setGlobal("renderer", new RendererAPI());

        //Host api if host
        if (aspect.isHost) {
            hostAPI = new HostAPI(aspect);
            setGlobal("host", hostAPI);
        } else {
            hostAPI = null;
        }

        //Manager api (gui aspects only)
        if (aspect.isGui) {
            setGlobal("manager", new ManagerAPI());
        }

        //Other APIs not shown here:

        //world api: set during aspect.tick()
        //user api and vanilla api: set when the user's entity first loads in
    }

    /**
     * Run the various PetPet util scripts
     * that Aspect defines
     */
    private void runUtils() {
        //Run aspect's util scripts for helpful
        //code defined in PetPet rather than Java
        runUtil("AspectInternalUtils"); //Random stuff, should sort later
        runUtil("Set");
        runUtil("JsonText"); //Class for json text
        runUtil("PrettyPrint"); //Functions for pretty printing things

        runUtil("ParticleHelpers"); //Functions that help with particles, matrices, etc
        runUtil("VanillaHelper"); //Functions that help set up vanilla parents and such
    }

    private void runUtil(String name) {
        try(InputStream in = IOUtils.getAsset("scripts/" + name + ".petpet")) {
            if (in == null) throw new RuntimeException("Failed to locate internal util script \"" + name + "\" - bug!");
            String code = new String(in.readAllBytes());
            runCode(name, code);
        } catch (Exception e) {
            error(e);
        }
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
            PetPetClosure compiled = this.compile(name, source);
            this.compiledScripts.put(name, compiled);
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
                if (shouldPrintToChat) {
                    String s = getStringFor(o);
                    //If the text is json, displays it formatted, otherwise just displays the literal text
                    Text possibleJsonText = DisplayUtils.tryParseJsonText(s);
                    DisplayUtils.displayPetPetMessage(possibleJsonText);
                }
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
            error(t);
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
            error(t);
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
            error(t);
        }
        return arg;
    }

    public PetPetClosure compile(String name, String src) {
        try {
            return instance.compile(name, src);
        } catch (Lexer.LexingException e) {
            DisplayUtils.displayError("Lexing error in " + name + ": " + e.getMessage(), shouldPrintToChat);
            throw new RuntimeException("Failed to load [" + name + "]", e);
        } catch (Parser.ParserException e) {
            DisplayUtils.displayError("Parsing error in " + name + ": " + e.getMessage(), shouldPrintToChat);
            throw new RuntimeException("Failed to load [" + name + "]", e);
        } catch (Compiler.CompilationException e) {
            DisplayUtils.displayError("Compilation error in " + name + ": " + e.getMessage(), shouldPrintToChat);
            throw new RuntimeException("Failed to load [" + name + "]", e);
        }
    }

    public void error(Throwable t) {
        aspect.error(t, Aspect.ErrorLocation.SCRIPT);
    }

    public boolean isErrored() {
        return aspect.isErrored();
    }

    /**
     * Return the host api for this aspect, but only if it has
     * isHost true. If not true, then it will return null.
     * Never returns a host api without isHost permissions.
     */
    public @Nullable HostAPI getHostAPI() {
        return hostAPI;
    }

}
