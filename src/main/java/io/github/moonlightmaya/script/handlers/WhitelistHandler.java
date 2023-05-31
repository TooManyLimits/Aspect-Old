package io.github.moonlightmaya.script.handlers;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.AspectMetadata;
import io.github.moonlightmaya.model.AspectModelPart;
import io.github.moonlightmaya.model.AspectTexture;
import io.github.moonlightmaya.model.Transformable;
import io.github.moonlightmaya.model.WorldRootModelPart;
import io.github.moonlightmaya.model.animation.Animation;
import io.github.moonlightmaya.model.animation.Animator;
import io.github.moonlightmaya.model.rendertasks.BlockTask;
import io.github.moonlightmaya.model.rendertasks.ItemTask;
import io.github.moonlightmaya.model.rendertasks.TextTask;
import io.github.moonlightmaya.script.apis.AspectAPI;
import io.github.moonlightmaya.script.apis.ClientAPI;
import io.github.moonlightmaya.script.apis.HostAPI;
import io.github.moonlightmaya.script.apis.RendererAPI;
import io.github.moonlightmaya.script.apis.entity.EntityAPI;
import io.github.moonlightmaya.script.apis.entity.LivingEntityAPI;
import io.github.moonlightmaya.script.apis.entity.PlayerAPI;
import io.github.moonlightmaya.script.apis.gui.ManagerAPI;
import io.github.moonlightmaya.script.apis.math.Matrices;
import io.github.moonlightmaya.script.apis.math.Quaternions;
import io.github.moonlightmaya.script.apis.math.Vectors;
import io.github.moonlightmaya.script.apis.world.*;
import io.github.moonlightmaya.script.events.AspectEvent;
import io.github.moonlightmaya.script.vanilla.VanillaFeature;
import io.github.moonlightmaya.script.vanilla.VanillaPart;
import io.github.moonlightmaya.script.vanilla.VanillaRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import org.joml.*;
import petpet.external.PetPetReflector;
import petpet.lang.run.PetPetClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Handles whitelisting of classes and setup of the legal types.
 * If other mods want to register classes as allowed, they can do so by
 * calling these register() methods.
 */
public class WhitelistHandler {

    private static final List<Consumer<AspectScriptHandler>> REGISTERED = new ArrayList<>();

    public static void setupInstance(AspectScriptHandler scriptHandler) {
        for (Consumer<AspectScriptHandler> handler : REGISTERED)
            handler.accept(scriptHandler);
    }

    private static final Predicate<Aspect> ALWAYS = a -> true;
    private static final Predicate<Aspect> GUI_ONLY = a -> a.isGui;
    private static final Predicate<Aspect> HOST_ONLY = a -> a.isHost;

    /**
     * Register the Aspect classes
     * Other mods may later also call these register() methods to allow
     * their own classes
     */
    static {
        //Math
        registerDirect(Vector2d.class, Vectors.VEC_2, ALWAYS, null);
        registerDirect(Vector3d.class, Vectors.VEC_3, ALWAYS, null);
        registerDirect(Vector4d.class, Vectors.VEC_4, ALWAYS, null);
        registerDirect(Matrix2d.class, Matrices.MAT_2, ALWAYS, null);
        registerDirect(Matrix3d.class, Matrices.MAT_3, ALWAYS, null);
        registerDirect(Matrix4d.class, Matrices.MAT_4, ALWAYS, null);
        registerDirect(Quaterniond.class, Quaternions.QUATERNION_CLASS, ALWAYS, null);

        //Model objects
        register(Transformable.class, "Transformable", ALWAYS);
        registerWithParent(AspectModelPart.class, "ModelPart", ALWAYS, Transformable.class);
        registerWithParent(WorldRootModelPart.class, "WorldRootModelPart", ALWAYS, AspectModelPart.class);

        registerWithParent(BlockTask.class, "BlockTask", ALWAYS, Transformable.class);
        registerWithParent(ItemTask.class, "ItemTask", ALWAYS, Transformable.class);
        registerWithParent(TextTask.class, "TextTask", ALWAYS, Transformable.class);
        register(AspectTexture.class, "Texture", ALWAYS, AspectTexture::addPenalties);

        //Miscellaneous
        register(AspectEvent.class, "Event", ALWAYS);
        register(AspectAPI.class, "Aspect", ALWAYS);
        register(ClientAPI.class, "Client", ALWAYS);
        register(RendererAPI.class, "Renderer", ALWAYS);
        register(Animation.class, "Animation", ALWAYS);

        //Vanilla things
        register(VanillaRenderer.class, "VanillaRenderer", ALWAYS);
        registerWithParent(VanillaPart.class, "VanillaPart", ALWAYS, Transformable.class);
        registerWithParent(VanillaFeature.class, "VanillaFeature", ALWAYS, Transformable.class);

        //Minecraft interaction
        registerDirect(ClientWorld.class, WorldAPI.WORLD_CLASS, ALWAYS, null);
        registerDirect(BlockState.class, BlockStateAPI.BLOCK_STATE_CLASS, ALWAYS, null);
        registerDirect(ItemStack.class, PetPetReflector.reflect(ItemStackAPI.class, "ItemStack"), ALWAYS, null);
        registerDirect(DimensionType.class, PetPetReflector.reflect(DimensionAPI.class, "Dimension"), ALWAYS, null);
        registerDirect(DimensionEffects.class, PetPetReflector.reflect(DimensionEffectsAPI.class, "DimensionEffects"), ALWAYS, null);
        registerDirect(Biome.class, BiomeAPI.BIOME_CLASS, ALWAYS, null);
        registerDirect(Particle.class, PetPetReflector.reflect(ParticleAPI.class, "Particle"), ALWAYS, null);

        //Entities
        registerDirect(Entity.class, PetPetReflector.reflect(EntityAPI.class, "Entity"), ALWAYS, null, EntityAPI::addGetAspectMethod);
        registerDirect(LivingEntity.class, PetPetReflector.reflect(LivingEntityAPI.class, "LivingEntity"), ALWAYS, Entity.class);
        registerDirect(PlayerEntity.class, PetPetReflector.reflect(PlayerAPI.class, "Player"), ALWAYS, LivingEntity.class);

        //GUI-only objects
        register(ManagerAPI.class, "Manager", GUI_ONLY);
        register(AspectMetadata.class, "Metadata", GUI_ONLY);

        //Host only
        register(HostAPI.class, "Host", HOST_ONLY);

        //no methods or anything, just registering these so they're legal objects to store as a variable
        registerDirect(RenderLayer.class, new PetPetClass("RenderLayer"), ALWAYS, null);
        registerDirect(ParticleEffect.class, new PetPetClass("ParticleEffect"), ALWAYS, null);
        registerDirect(Animator.class, new PetPetClass("Animator"), ALWAYS, null);
    }

    /**
     * Register a class to be reflected with a given name, and modified according to the modifiers
     * in order.
     */
    public static void register(Class<?> clazz, String name, Predicate<Aspect> shouldAdd, BiFunction<AspectScriptHandler, PetPetClass, PetPetClass>... modifiers) {
        registerWithParent(clazz, name, shouldAdd, null, modifiers);
    }

    /**
     * Reflect and register with a parent. The parent must be registered before this one is
     */
    public static void registerWithParent(Class<?> clazz, String name, Predicate<Aspect> shouldAdd, Class<?> superClass, BiFunction<AspectScriptHandler, PetPetClass, PetPetClass>... modifiers) {
        registerDirect(clazz, PetPetReflector.reflect(clazz, name), shouldAdd, superClass, modifiers);
    }

    /**
     * Register a petpet class directly
     */
    public static void registerDirect(Class<?> javaClass, PetPetClass petPetClass, Predicate<Aspect> shouldAdd, Class<?> superClass, BiFunction<AspectScriptHandler, PetPetClass, PetPetClass>... modifiers) {
        REGISTERED.add(scriptHandler -> {
            //If this class doesn't want to be in the aspect, return early
            if (!shouldAdd.test(scriptHandler.aspect)) return;

            //Copy it, make editable, set the parent, and add the modifiers
            //Always copy and make editable
            PetPetClass curClass = petPetClass.copy().makeEditable();

            //If there's a superclass, add it
            if (superClass != null) {
                PetPetClass parent = scriptHandler.instance.interpreter.classMap.get(superClass);
                if (parent == null)
                    throw new IllegalStateException("Attempt to register class " + javaClass + " with parent " + superClass + ", but parent was not registered yet");
                curClass.setParent(parent);
            }

            //Apply all the modifiers
            for (BiFunction<AspectScriptHandler, PetPetClass, PetPetClass> modifier : modifiers)
                curClass = modifier.apply(scriptHandler, curClass);

            //Register
            scriptHandler.instance.registerClass(javaClass, curClass);
        });
    }
}
