package io.github.moonlightmaya.util;

import petpet.lang.run.JavaFunction;

public class ScriptUtils {

    /**
     * Do-nothing functions that stand in for non-host functions on other computers
     */
    public static void doNothing0() {}
    public static final JavaFunction DO_NOTHING_0_ARG = new JavaFunction(ScriptUtils.class, "doNothing0", false);
    public static void doNothing1(Object h) {}
    public static final JavaFunction DO_NOTHING_1_ARG = new JavaFunction(ScriptUtils.class, "doNothing1", false);


}
