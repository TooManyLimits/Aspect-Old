package io.github.moonlightmaya.model;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.util.AspectMatrixStack;
import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;

import java.util.Objects;

/**
 * This class is a special case because it needs to contain a high-precision world vector.
 * This is used when rendering in order to counteract the floating point issues that Figura's
 * world-parented model parts face at high coordinates.
 */
@PetPetWhitelist
public class WorldRootModelPart extends AspectModelPart {

    /**
     * The world position of this root model part. Has a higher precision than other values,
     * double precision. This property only exists on the roots of world model trees, and
     * exists to counteract the problems of floating point precision at high coordinate values.
     */
    public final Vector3d worldPos = new Vector3d();

    /**
     * The id of the dimension this model part is located in. In Aspect, model parts are loaded and rendered
     * separately from the actual entity which uses the Aspect, meaning that it's possible to view
     * models in people's Aspects even when they're not in the same dimension as you. In order for
     * people to be able to choose which dimension their parts exist in, and to prevent the parts
     * from appearing across all dimensions all the time, this field needs to exist.
     */
    public String dimension;

    public WorldRootModelPart(BaseStructures.ModelPartStructure nbt, Aspect owningAspect) {
        super(nbt, owningAspect, null);
    }

    public void setWorldPos(Vector3d pos) {
        worldPos.set(pos);
    }
    public void setWorldPos(double x, double y, double z) {
        worldPos.set(x, y, z);
    }


    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return; //you have bigger problems, dont render

        // If dimension is null, then the part appears regardless of dimension
        // If dimension is not null, then restrict the part to only appear in the provided dimension
        if (dimension != null && !Objects.equals(dimension, world.getDimensionKey().getValue().toString())) return;

        matrixStack.push();
        matrixStack.translate(worldPos);

        //This is how minecraft selects the light level for rendering an entity
        BlockPos lightChoosePos = MathUtils.getBlockPos(worldPos);
        int light = LightmapTextureManager.pack(
                world.getLightLevel(LightType.BLOCK, lightChoosePos),
                world.getLightLevel(LightType.SKY, lightChoosePos)
        );

        super.render(vcp, matrixStack, light);
        matrixStack.pop();
    }

    @Override
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, int light) {
        //Shouldn't render this with a light level, as the light level is calculated by the world part's
        //world pos instead of the entity's light level!
        throw new UnsupportedOperationException("This render method should not work on world root types, if this happens it's a mistake by the mod devs!");
    }

    //PETPET METHODS

    //worldPos()
    @PetPetWhitelist
    public WorldRootModelPart worldPos_3(double x, double y, double z) {
        setWorldPos(x, y, z);
        return this;
    }
    @PetPetWhitelist
    public WorldRootModelPart worldPos_1(Vector3d v) {
        setWorldPos(v);
        return this;
    }
    @PetPetWhitelist
    public Vector3d worldPos_0() {
        return new Vector3d(worldPos);
    }


    //Also have the option to set field directly, but might as well provide a method version too i guess
    @PetPetWhitelist
    public WorldRootModelPart dimension_1(String dimension) {
        this.dimension = dimension;
        return this;
    }
    @PetPetWhitelist
    public String dimension_0() {
        return dimension;
    }

}
