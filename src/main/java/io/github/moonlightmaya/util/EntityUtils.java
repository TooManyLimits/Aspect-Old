package io.github.moonlightmaya.util;

import io.github.moonlightmaya.mixin.render.world.ClientWorldInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityUtils {

    public static UUID getLocalUUID() {
        return MinecraftClient.getInstance().getSession().getProfile().getId();
    }

    @Nullable public static Entity getEntityByUUID(ClientWorld world, UUID uuid) {
        if (world == null) return null;
        return ((ClientWorldInvoker) world).aspect$getEntityLookup().get(uuid);
    }

    @Nullable public static PlayerListEntry getPlayerListEntry(UUID playerUUID) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return networkHandler == null ? null : networkHandler.getPlayerListEntry(playerUUID);
    }

}
