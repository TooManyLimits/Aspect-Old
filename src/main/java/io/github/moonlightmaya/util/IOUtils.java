package io.github.moonlightmaya.util;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class IOUtils {

    /**
     * Doesn't recurse because im too dumb
     * to allow subfolders in aspects
     */
    public static ArrayList<File> getByExtension(Path root, String extension) {
        File file = root.toFile();
        ArrayList<File> list = new ArrayList<File>();
        if (file.exists() && file.isDirectory()) {
            File[] arr = root.toFile().listFiles((f,s) -> s.endsWith("."+extension));
            if (arr != null)
                Collections.addAll(list, arr);
        }
        return list;
    }

}
