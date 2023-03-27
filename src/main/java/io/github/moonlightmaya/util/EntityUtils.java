package io.github.moonlightmaya.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityUtils {

    @Nullable public static Entity getEntityByUUID(ClientWorld world, UUID uuid) {
        for (Entity e : world.getEntities())
            if (e.getUuid().equals(uuid))
                return e;
        return null;
    }

}
