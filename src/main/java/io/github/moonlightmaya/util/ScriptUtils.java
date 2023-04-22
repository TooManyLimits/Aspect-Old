package io.github.moonlightmaya.util;

import petpet.lang.run.Interpreter;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetException;

public class ScriptUtils {

    /**
     * Installs a check
     * @param javaFunction
     * @param required
     */
    public static void installHostCheck(JavaFunction javaFunction, Interpreter required) {
        javaFunction.costPenalizer = i -> {
            if (i != required) throw new PetPetException("Cannot call this function, are not host");
            else return 0;
        };
    }

}
