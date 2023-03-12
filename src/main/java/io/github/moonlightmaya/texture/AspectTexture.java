package io.github.moonlightmaya.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.conversion.BaseStructures;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class AspectTexture extends ResourceTexture {

    private String name;
    private final NativeImage image;

    /**
     * Whether this texture has been modified and needs to be reuploaded.
     * Starts as true, so we upload it on the first use.
     */
    private boolean dirty = true;

    /**
     * Whether this texture has been registered to the texture manager yet.
     * False to start, becomes true on first use. Unlike dirty, this doesn't
     * change when the texture is modified.
     */
    private boolean registered = false;

    /**
     * Kept because in some instances the texture may be accessed after it's closed.
     * Upon closing we set this to true, and check that it's true before doing operations
     * involving the texture data.
     */
    private boolean isClosed = false;

    public AspectTexture(Aspect aspect, BaseStructures.Texture baseTex) throws IOException {
        super(AspectMod.id("aspect_textures/" + aspect.getAspectId() + "/" + baseTex.name()));
        ByteBuffer buffer = BufferUtils.createByteBuffer(baseTex.data().length);
        buffer.put(baseTex.data());
        buffer.rewind();
        this.image = NativeImage.read(buffer);
        this.name = baseTex.name();
    }

    public Identifier getIdentifier() {
        return location;
    }

    //Disable load. We don't get our textures from the resource manager, we get them from
    //our own Aspect managers.
    @Override
    public void load(ResourceManager manager) throws IOException {}

    @Override
    public void close() {
        //No need to close multiple times
        if (isClosed) return;

        image.close(); //Close the native image resource
        this.clearGlId(); //Delete the texture handle in openGL

        //Ensure this method isn't run again on the same texture, and inform
        //other method calls that this texture is not in a usable state
        isClosed = true;
    }

    /**
     * Registers this texture if it hasn't yet been registered.
     * Then, re-uploads it if it's marked as dirty.
     */
    public void uploadIfNeeded() {
        //If closed, we don't need to do anything
        if (!isClosed) {
            //If the texture is not yet registered, then register it and mark as registered
            if (!registered) {
                TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
                textureManager.registerTexture(this.location, this);
                registered = true;
            }
            //If the texture needs to be uploaded, then upload it
            if (dirty) {
                RenderUtils.executeOnRenderThread(() -> {
                    TextureUtil.prepareImage(this.getGlId(), image.getWidth(), image.getHeight());
                    image.upload(0, 0, 0, false);
                });
                dirty = false;
            }
        }
    }


}
