package io.github.moonlightmaya.script;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.script.initializers.ScriptInitializer;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.IOUtils;
import petpet.external.PetPetInstance;
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
    private final JavaFunction requireFunction;

    /**
     * When first creating the script handler, we will compile
     * all the scripts, and potentially report errors.
     * Then, we run the "main" script.
     */
    public AspectScriptHandler(Aspect aspect) {
        this.aspect = aspect;

        //Create new instance
        instance = new ScriptInitializer(aspect).createInstance();

        //Compile all the scripts
        compiledScripts = new HashMap<>();
        compileScripts();

        //Generate the "require" function using those compiled scripts
        //and add it to the globals table
        requireFunction = setupRequire();
        instance.setGlobal("require", requireFunction);

        if (!compiledScripts.isEmpty()) {
            //Run the main script
            //Maybe in the future it would be good to make the "running" of
            //the script be in another method.
            String main = "main";
            requireFunction.call(main);
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

}
