package io.github.moonlightmaya.mixin.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientWorld.class)
public interface ClientWorldInvoker {

    @Invoker("getEntityLookup")
    EntityLookup<Entity> aspect$getEntityLookup();

}
