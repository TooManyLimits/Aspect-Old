package io.github.moonlightmaya.model.rendertasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;

@PetPetWhitelist
public class ItemTask extends RenderTask {

    private ItemStack stack;
    private boolean leftHanded = false;
    private ModelTransformationMode mode = ModelTransformationMode.NONE;
    private @Nullable LivingEntity entity;
    private @Nullable ClientWorld world;


    public ItemTask(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    protected void specializedRender(VertexConsumerProvider vcp, int light, int overlay) {
        int seed = entity == null ? 0 : entity.getId() + mode.ordinal();
        MinecraftClient.getInstance().getItemRenderer().renderItem(entity, stack, mode, leftHanded, VANILLA_STACK, vcp, world, light, overlay, seed);
    }

    @PetPetWhitelist
    public ItemTask item(ItemStack stack) {
        this.stack = stack;
        return this;
    }

    @PetPetWhitelist
    public ItemTask mode(String mode) {
        try {
            this.mode = ModelTransformationMode.valueOf(mode);
        } catch (Exception e) {
            throw new PetPetException("Invalid item render mode " + mode);
        }
        return this;
    }

    @PetPetWhitelist
    public ItemTask leftHanded(boolean leftHanded) {
        this.leftHanded = leftHanded;
        return this;
    }

    @PetPetWhitelist
    public ItemTask entity(LivingEntity entity) {
        this.entity = entity;
        return this;
    }

    @PetPetWhitelist
    public ItemTask world(ClientWorld world) {
        this.world = world;
        return this;
    }

}
