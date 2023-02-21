package io.github.moonlightmaya.util;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class IOUtils {

    private static final Map<String, String> SHORTEN_MAP = ImmutableMap.<String, String>builder()
            .put("name", "n")
            .build();

    public static String shorten(String s) {
        if (SHORTEN_MAP.containsKey(s))
            return SHORTEN_MAP.get(s);
        throw new IllegalArgumentException("Invalid argument to IOUtils.shorten: \"" + s + "\"");
    }


}
