package io.github.moonlightmaya.util;

import io.github.moonlightmaya.AspectMod;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Function;

public class DataStructureUtils {

    public static <K> int indexOfKey(LinkedHashMap<K, ?> map, K key) {
        int i = 0;
        for (K elem : map.keySet()) {
            if (elem.equals(key))
                return i;
            i++;
        }
        return -1;
    }

    public static <V> V valueAtIndex(LinkedHashMap<?, V> map, int index) {
        Iterator<V> vals = map.values().iterator();
        for (int i = 0; i < index - 1; i++)
            vals.next();
        return vals.next();
    }

    /**
     * Used for enums with strings, since it's annoying to have to try-catch the
     * function to get a default value if it errors.
     */
    public static <K, V> V getOrDefault(Function<K, V> func, K key, V defaultVal, @Nullable String infomessage) {
        try {
            return func.apply(key);
        } catch (Exception e) {
            if (infomessage != null)
                AspectMod.LOGGER.info(infomessage);
            return defaultVal;
        }
    }

}
