package io.github.moonlightmaya.script;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.script.events.AspectEvent;
import io.github.moonlightmaya.script.events.EventHandler;
import io.github.moonlightmaya.util.DisplayUtils;
import petpet.external.PetPetInstance;
import petpet.external.PetPetReflector;
import petpet.lang.compile.Compiler;
import petpet.lang.lex.Lexer;
import petpet.lang.parse.Parser;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClosure;
import petpet.lang.run.PetPetException;
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
        String main = "main";
        requireFunction.call(main);
    }

    /**
     * Register the different types which are allowed
     */
    private void registerTypes() {
        //Events
        instance.registerClass(AspectEvent.class, PetPetReflector.reflect(AspectEvent.class, "Event"));

        //Model Parts
        //instance.registerClass(WorldRootModelPart.class, PetPetReflector.reflect(WorldRootModelPart.class, "WorldRootModelPart"));
        instance.registerClass(AspectModelPart.class, PetPetReflector.reflect(AspectModelPart.class, "ModelPart"));
    }

    /**
     * Set all the global variables that are needed
     */
    private void setupGlobals() {
        //Print functions
        instance.setGlobal("print", DisplayUtils.PRINT_FUNCTION);
        instance.setGlobal("log", DisplayUtils.PRINT_FUNCTION);

        //Require
        requireFunction = setupRequire();
        instance.setGlobal("require", requireFunction);

        //Models
        PetPetTable models = new PetPetTable();
        models.put("entity", aspect.entityRoot);
        instance.setGlobal("models", models);

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

    public EventHandler getEventHandler() {
        return eventHandler;
    }

}
