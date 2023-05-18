package io.github.moonlightmaya.model;

import com.mojang.blaze3d.platform.TextureUtil;
import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.AspectMod;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.mixin.TextureManagerAccessor;
import io.github.moonlightmaya.util.ColorUtils;
import io.github.moonlightmaya.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import org.lwjgl.BufferUtils;
import petpet.external.PetPetWhitelist;

import java.io.IOException;
import java.nio.ByteBuffer;

@PetPetWhitelist
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
        super(AspectMod.id("aspect_textures/" + aspect.getAspectUUID() + "/" + baseTex.name()));
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
    //our own Aspect loading.
    @Override
    public void load(ResourceManager manager) throws IOException {}

    @Override
    public void close() {
        RenderUtils.executeOnRenderThread(() -> {
            //No need to close multiple times
            if (isClosed) return;

            image.close(); //Close the native image resource
            this.clearGlId(); //Delete the texture handle in openGL

            //Ensure this method isn't run again on the same texture, and inform
            //other method calls that this texture is not in a usable state
            isClosed = true;

            //Delete ourself from the game's texture manager, if we don't do this then
            //the texture will stay registered there forever, leaking memory
            //Hopefully this doesn't concurrent access exception or whatever... it's on the render thread so should be ok?
            ((TextureManagerAccessor) MinecraftClient.getInstance().getTextureManager()).getTextures().remove(this.location);
        });
    }

    /**
     * Registers this texture if it hasn't yet been registered.
     * Then, re-uploads it if it's marked as dirty.
     */
    public void uploadIfNeeded() {
        if (dirty) {
            //dirty is true at the start, when texture is first created
            RenderUtils.executeOnRenderThread(() -> {
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
                        //Image may be closed in between registering this lambda and actually running it
                        if (!isClosed) {
                            TextureUtil.prepareImage(this.getGlId(), image.getWidth(), image.getHeight());
                            image.upload(0, 0, 0, false);
                        }
                        dirty = false;
                    }
                }
            });
        }
    }

    /**
     * PETPET METHODS BELOW!
     */

    @PetPetWhitelist
    public String name() {
        return this.name;
    }

    @PetPetWhitelist
    public void setPixel_3(int x, int y, Vector4d color) {
        image.setColor(x, y, ColorUtils.VecToIntABGR(color.x, color.y, color.z, color.w));
    }

    @PetPetWhitelist
    public void setPixel_5(int x, int y, double r, double g, double b) {
        image.setColor(x, y, ColorUtils.VecToIntABGR(r, g, b, 1));
    }

    @PetPetWhitelist
    public void setPixel_6(int x, int y, double r, double g, double b, double a) {
        image.setColor(x, y, ColorUtils.VecToIntABGR(r, g, b, a));
    }

    @PetPetWhitelist
    public Vector4d getPixel(int x, int y) {
        return ColorUtils.intABGRToVec(image.getColor(x, y));
    }

    /**
     * Transform the selected region using the given matrix. Only affects RGB, not alpha.
     * This is because I didn't want to make a 5x5 matrix impl oh god that sounds horrible
     */
    @PetPetWhitelist
    public void transform(int x, int y, int width, int height, Matrix4d matrix) {
        Vector4d vec = new Vector4d();
        for (int curX = 0; curX < width; curX++)
            for (int curY = 0; curY < height; curY++) {
                ColorUtils.intABGRToVec(image.getColor(curX, curY), vec);
                double oldAlpha = vec.w;
                vec.w = 1;
                vec.mul(matrix);
                setPixel_6(curX, curY, vec.x, vec.y, vec.z, oldAlpha);
            }
    }

    @PetPetWhitelist
    public void color(int x, int y, int width, int height, Vector4d colorMultiplier) {
        Vector4d vec = new Vector4d();
        for (int curX = 0; curX < width; curX++)
            for (int curY = 0; curY < height; curY++) {
                ColorUtils.intABGRToVec(image.getColor(curX, curY), vec).mul(colorMultiplier);
                setPixel_3(curX, curY, vec);
            }
    }

    @PetPetWhitelist
    public void fill(int x, int y, int width, int height, Vector4d color) {
        int abgr = ColorUtils.VecToIntABGR(color);
        image.fillRect(x, y, width, height, abgr);
    }

    /**
     * Does not update the texture instantly, but causes the part
     * to be updated at the beginning of world_render on the next frame.
     */
    @PetPetWhitelist
    public void update() {
        this.dirty = true;
    }


}
