package io.github.moonlightmaya.script.handlers;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.script.AspectScript;
import io.github.moonlightmaya.util.IOUtils;
import petpet.lang.compile.Compiler;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetClosure;
import petpet.lang.run.PetPetFunction;
import petpet.types.PetPetTable;

import java.io.InputStream;
import java.util.Map;

/**
 * Deals with all the different util scripts which
 * can be obtained through the util() function.
 */
public class UtilHandler {

    /**
     * Source code for all utils, indexed by name
     */
    private static final Map<String, String> UTIL_SOURCES = new PetPetTable<>();

    /**
     * Add a new util's source code to the map
     */
    public static void registerUtil(String name, String sourceCode) {
        if (UTIL_SOURCES.containsKey(name))
            throw new IllegalArgumentException("Util with name \"" + name + "\" already exists");
        UTIL_SOURCES.put(name, sourceCode);
    }

    /**
     * Generate a util() function for a given AspectScript instance
     */
    public static JavaFunction getUtilFunction(AspectScript scriptInstance) {
        //Compile all the scripts in the given instance
        Map<String, PetPetClosure> compiledScripts = new PetPetTable<>();
        for (Map.Entry<String, String> utilEntry : UTIL_SOURCES.entrySet()) {
            String name = utilEntry.getKey();
            String source = utilEntry.getValue();
            PetPetClosure callable = scriptInstance.compile(name, source);
            compiledScripts.put(name, callable);
        }
        //Create the java function and return
        return AspectScript.getRequireFunction("util", compiledScripts);
    }


    /**
     * Aspect's own util files
     */
    private static void registerAspectUtil(String name) {
        try(InputStream in = IOUtils.getAsset("scripts/" + name + ".petpet")) {
            if (in == null) throw new RuntimeException("Failed to locate internal util script \"" + name + "\" - bug!");
            String code = new String(in.readAllBytes());
            registerUtil(name, code);
        } catch (Exception e) {
            throw new IllegalStateException("Builtin aspect util \"" + name + "\" failed to load", e);
        }
    }

    static {
        //Classes
        registerAspectUtil("JsonText");
        registerAspectUtil("PrettyPrinter");
        registerAspectUtil("Set");

        //Functions
        registerAspectUtil("pprint");

        //No returns
        registerAspectUtil("keywords");
        registerAspectUtil("overloads");
        registerAspectUtil("featureAliases");
        registerAspectUtil("partHelpers");
    }


}
