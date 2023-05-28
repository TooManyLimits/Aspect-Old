package io.github.moonlightmaya.script.apis;

import com.mojang.brigadier.StringReader;
import io.github.moonlightmaya.mixin.render.world.WorldRendererAccessor;
import io.github.moonlightmaya.util.DisplayUtils;
import io.github.moonlightmaya.util.MathUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.util.Window;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.*;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.lang.Runtime;
import java.util.HashMap;

@PetPetWhitelist
public class ClientAPI {

    //Simple boolean

    @PetPetWhitelist
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @PetPetWhitelist
    public boolean isDebugOverlayEnabled() {
        return MinecraftClient.getInstance().options.debugEnabled;
    }

    @PetPetWhitelist
    public boolean isHudEnabled() {
        return !MinecraftClient.getInstance().options.hudHidden;
    }

    @PetPetWhitelist
    public boolean hasResource(String path) {
        return MinecraftClient.getInstance().getResourceManager().getResource(new Identifier(path)).isPresent();
    }

    @PetPetWhitelist
    public boolean isPaused() {
        return MinecraftClient.getInstance().isPaused();
    }

    //Simple string

    @PetPetWhitelist
    public String getVersion() {
        return MinecraftClient.getInstance().getGameVersion();
    }

    @PetPetWhitelist
    public String getServerBrand() {
        if (MinecraftClient.getInstance().getServer() != null)
            return "Integrated";
        if (MinecraftClient.getInstance().player == null)
            return null;
        return MinecraftClient.getInstance().player.getServerBrand();
    }

    @PetPetWhitelist
    public String getVersionType() {
        return MinecraftClient.getInstance().getVersionType();
    }

    @PetPetWhitelist
    public String getCurrentEffect() {
        return MinecraftClient.getInstance().gameRenderer.getPostProcessor().getName();
    }

    @PetPetWhitelist
    public String getJavaVersion() {
        return System.getProperty("java.version");
    }

    @PetPetWhitelist
    public String getSoundStatistics() {
        return MinecraftClient.getInstance().getSoundManager().getDebugString();
    }

    @PetPetWhitelist
    public String getActiveLang() {
        return MinecraftClient.getInstance().getLanguageManager().getLanguage();
    }

    //Statistics

    @PetPetWhitelist
    public String getEntityStatistics() {
        return MinecraftClient.getInstance().worldRenderer.getEntitiesDebugString();
    }

    @PetPetWhitelist
    public String getChunkStatistics() {
        return MinecraftClient.getInstance().worldRenderer.getChunksDebugString();
    }

    @PetPetWhitelist
    public double getEntityCount() {
        return ((WorldRendererAccessor) MinecraftClient.getInstance().worldRenderer).getRegularEntityCount();
    }

    @PetPetWhitelist
    public double getBlockEntityCount() {
        return ((WorldRendererAccessor) MinecraftClient.getInstance().worldRenderer).getBlockEntityCount();
    }

    @PetPetWhitelist
    public double getParticleCount() {
        return Double.parseDouble(MinecraftClient.getInstance().particleManager.getDebugString());
    }

    //Memory

    @PetPetWhitelist
    public double getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @PetPetWhitelist
    public double getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @PetPetWhitelist
    public double getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }


    //Windowing things

    @PetPetWhitelist
    public Vector2d getWindowSize() {
        Window window = MinecraftClient.getInstance().getWindow();
        return new Vector2d(window.getFramebufferWidth(), window.getFramebufferHeight());
    }

    @PetPetWhitelist
    public double getGuiScale() {
        return MinecraftClient.getInstance().getWindow().calculateScaleFactor(
                MinecraftClient.getInstance().options.getGuiScale().getValue(),
                MinecraftClient.getInstance().forcesUnicodeFont());
    }

    @PetPetWhitelist
    public Vector2d getScaledWindowSize() {
        Window window = MinecraftClient.getInstance().getWindow();
        return new Vector2d(window.getScaledWidth(), window.getScaledHeight());
    }

    @PetPetWhitelist
    public Vector2d getMousePos() {
        Mouse m = MinecraftClient.getInstance().mouse;
        return new Vector2d(m.getX(), m.getY());
    }

    @PetPetWhitelist
    public boolean isWindowFocused() {
        return MinecraftClient.getInstance().isWindowFocused();
    }

    //Text utils
    //TODO: Line wrapping *dies*

    @PetPetWhitelist
    public double getTextWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(DisplayUtils.tryParseJsonText(text));
    }

    @PetPetWhitelist
    public double getTextHeight(String text) {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

//    @PetPetWhitelist
    public Vector2d getTextDimensions(String text) {
        return null;
    }

    //Time things

    //@PetPetWhitelist
    public PetPetTable<?, ?> getDate() {
        return null;
    }

    @PetPetWhitelist
    public double getSystemTime() {
        return System.currentTimeMillis();
    }

    @PetPetWhitelist
    public double getTickDelta() {
        return MinecraftClient.getInstance().getTickDelta();
    }

    @PetPetWhitelist
    public double getLastFrameTime() {
        return MinecraftClient.getInstance().getLastFrameDuration();
    }

    //Camera/viewer information

    @PetPetWhitelist
    public Vector3d getCameraPos() {
        return MathUtils.fromVec3d(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
    }

    @PetPetWhitelist
    public Vector3d getCameraRot() {
        Quaternionf quat = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
        Vector3f vec = new Vector3f();
        quat.getEulerAnglesYXZ(vec);
        return new Vector3d(vec).mul(MathHelper.DEGREES_PER_RADIAN, -MathHelper.DEGREES_PER_RADIAN, MathHelper.DEGREES_PER_RADIAN);
    }

    //@PetPetWhitelist
    public Quaterniond getCameraQuat() {
        return new Quaterniond(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
    }

    @PetPetWhitelist
    public PlayerEntity getViewer() {
        return MinecraftClient.getInstance().player;
    }

    //Other

    @PetPetWhitelist
    public double getFOV() {
        return MinecraftClient.getInstance().options.getFov().getValue();
    }

    @PetPetWhitelist
    public String getFPSString() {
        return MinecraftClient.getInstance().fpsDebugString;
    }

    @PetPetWhitelist
    public double getFPS() {
        return MinecraftClient.getInstance().getCurrentFps();
    }

    @PetPetWhitelist
    public PetPetList<String> getActiveResourcePacks() {
        PetPetList<String> res = new PetPetList<>();
        for (ResourcePackProfile profile : MinecraftClient.getInstance().getResourcePackManager().getEnabledProfiles())
            res.add(profile.getDisplayName().getString());
        return res;
    }

    /**
     * Particle spawning!
     */

    private boolean useParticleCache = true;
    private final HashMap<String, ParticleEffect> PARTICLE_EFFECTS_CACHE = new HashMap<>();

    /**
     * If you want to disable particle type caching for some reason, go for it
     * Setting to false clears the cache
     */
    @PetPetWhitelist
    public void cacheParticles(boolean bool) {
        this.useParticleCache = bool;
        if (!bool)
            PARTICLE_EFFECTS_CACHE.clear();
    }

    @PetPetWhitelist
    public Particle particle_2(Object str, Vector3d pos) {
        return particle_7(str, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    @PetPetWhitelist
    public Particle particle_3(Object str, Vector3d pos, Vector3d vel) {
        return particle_7(str, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
    }

    @PetPetWhitelist
    public Particle particle_4(Object str, double x, double y, double z) {
        return particle_7(str, x, y, z, 0, 0, 0);
    }

    /**
     * Creates a new particle of the given type, and returns it
     * If there is no world to spawn a particle in, returns null
     */
    @PetPetWhitelist
    public Particle particle_7(Object particleType, double x, double y, double z, double xVel, double yVel, double zVel) {
        if (MinecraftClient.getInstance().world == null)
            return null;
        try {
            ParticleEffect effect;
            if (particleType instanceof String str) {
                if (useParticleCache) {
                    effect = PARTICLE_EFFECTS_CACHE.get(str);
                    if (effect == null) {
                        effect = ParticleEffectArgumentType.readParameters(new StringReader(str), Registries.PARTICLE_TYPE.getReadOnlyWrapper());
                        //Only cache default particle effects, others are created dynamically on the fly
                        if (effect.getClass() == DefaultParticleType.class)
                            PARTICLE_EFFECTS_CACHE.put(str, effect);
                    }
                } else {
                    effect = ParticleEffectArgumentType.readParameters(new StringReader(str), Registries.PARTICLE_TYPE.getReadOnlyWrapper());
                }
            } else if (particleType instanceof ParticleEffect e) {
                effect = e;
            } else {
                throw new PetPetException("client.particle() expects a string or a particle type!");
            }
            ParticleManager manager = MinecraftClient.getInstance().particleManager;
            return manager.addParticle(effect, x, y, z, xVel, yVel, zVel);
        } catch (Exception e) {
            throw new PetPetException(e.getMessage());
        }
    }

    /**
     * Create a new particle type from a given string, which can be then
     * passed into the `.particle()` methods. Will offer improved performance
     * if you use the same particle many times, as it won't have to parse out
     * the string every time.
     * Less useful when using simple particles like "flame", since the
     * type caching system will handle those automatically, but it should still
     * be slightly faster.
     */
    @PetPetWhitelist
    public ParticleEffect newParticleType(String string) {
        try {
            return ParticleEffectArgumentType.readParameters(new StringReader(string), Registries.PARTICLE_TYPE.getReadOnlyWrapper());
        } catch (Exception e) {
            throw new PetPetException(e.getMessage());
        }
    }
}
