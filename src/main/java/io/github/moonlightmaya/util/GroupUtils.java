package io.github.moonlightmaya.util;

import java.util.*;

public class GroupUtils {

    private static final Map<String, Set<UUID>> GROUPS = new HashMap<>();

    static {
        register("50de3aff-e8ef-4d55-9092-f96b7b40de7a", "fox", "cat"); //limits
        register("0d04770a-9482-4a39-8011-fcbb7c99b8e1", "fox"); //foxes
        register("8b07d8ad-352e-4b86-b1bc-2d2dad269c4b", "fox"); //ecorous
        register("93ab815f-92ab-4ea0-a768-c576896c52a8", "fox", "cat"); //auria
        register("cbb5b758-b72f-4bdd-80cb-7be302e087a0", "fox"); //blossoms
        register("7fd819d1-f8a2-48d3-9f69-fd5394f47030", "fox", "bunny"); //alice
        register("d2cf91ee-1d33-4ede-9468-f22d8ab750b2", "fox"); //sylvia
        register("dbe051b7-1e9a-433c-893e-96a89e93449e", "fox"); //emma
        register("dba79744-5129-4a97-aff9-71ca181faddd", "fox", "cat"); //lexize
        register("7c85c805-a137-4671-bc79-89c8480c2548", "cat"); //chroma
        register("ec3161aa-beb2-44cf-9c69-2adbde06d6fb","cat"); //chloe
        register("e2d8edf0-69b0-4c49-8315-2907f571d157", "cat"); //skye
        register("38b017bc-c341-444c-86f9-3bdf44ef3de0", "cookie"); //cookie
        register("66a6c5c4-963b-4b73-a0d9-162faedd8b7f", "bunny", "hamburger"); //fran
        register("4f097e00-da22-45ea-8e42-4643d0ea0cfe", "fox", "cat", "bird", "fish"); //nuke
    }

    public static void register(String user, String... groups) {
        UUID uuid = UUID.fromString(user);
        for (String group : groups)
            GROUPS.computeIfAbsent(group, g -> new HashSet<>()).add(uuid);
    }

    public static boolean is(UUID uuid, String group) {
        return GROUPS.get(group).contains(uuid);
    }

}
