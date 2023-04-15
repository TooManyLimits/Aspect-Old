package io.github.moonlightmaya.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

public class ItemUtils {

    /**
     * If the provided stack is null or empty, then return
     * a new ItemStack of Air, otherwise return the passed
     * argument.
     */
    public static ItemStack checkStack(@Nullable ItemStack itemStack) {
        return (itemStack != null && !itemStack.isEmpty()) ? itemStack : Items.AIR.getDefaultStack();
    }

}
