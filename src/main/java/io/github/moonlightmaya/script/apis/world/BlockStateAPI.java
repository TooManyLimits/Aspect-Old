package io.github.moonlightmaya.script.apis.world;

import io.github.moonlightmaya.util.MathUtils;
import io.github.moonlightmaya.util.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import org.joml.Vector3d;
import petpet.external.PetPetReflector;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.JavaFunction;
import petpet.lang.run.PetPetClass;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;

import java.util.Collection;
import java.util.function.Function;

@PetPetWhitelist
public class BlockStateAPI {
    public static final PetPetClass BLOCK_STATE_CLASS;

    static {
        BLOCK_STATE_CLASS = PetPetReflector.reflect(BlockStateAPI.class, "BlockState");

        genWorldPosAcceptors("isTranslucent", BlockState::isTranslucent);
        genWorldPosAcceptors("getOpacity", BlockState::getOpacity);
        genWorldPosAcceptors("isSolidBlock", BlockState::isSolidBlock);

        genWorldPosAcceptors("isFullCube", BlockState::isFullCube);
        genWorldPosAcceptors("hasEmissiveLighting", BlockState::hasEmissiveLighting);
        genWorldPosAcceptors("getHardness", BlockState::getHardness);
        genWorldPosAcceptors("getComparatorOutput", BlockState::getComparatorOutput);
        genWorldPosAcceptors("getOcclusion", BlockState::getAmbientOcclusionLightLevel);

        genWorldPosAcceptors("toStateString", (state, world, pos) -> {
            BlockEntity be = world.getBlockEntity(pos);
            return BlockArgumentParser.stringifyBlockState(state) + (be == null ? "{}" : be.createNbt().toString());
        });
        genWorldPosAcceptors("getEntityData", (state, world, pos) -> {
            BlockEntity be = world.getBlockEntity(pos);
            return NbtUtils.toPetPet(be == null ? new NbtCompound() : be.createNbt());
        });

        genWorldPosAcceptorsWithTransformer("hasCollision", BlockState::getCollisionShape, shape -> !shape.isEmpty());
        genWorldPosAcceptorsWithTransformer("getMapColor", BlockState::getMapColor, i -> MathUtils.intToRGBA(i.color));
    }

    @PetPetWhitelist
    public static String getID(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    @PetPetWhitelist
    public static boolean hasBlockEntity(BlockState state) {
        return state.hasBlockEntity();
    }
    @PetPetWhitelist
    public static boolean isOpaque(BlockState state) {
        return state.isOpaque();
    }
    @PetPetWhitelist
    public static boolean emitsRedstonePower(BlockState state) {
        return state.emitsRedstonePower();
    }
    @PetPetWhitelist
    public static PetPetList<String> getTags(BlockState state) {
        PetPetList<String> list = new PetPetList<>();
        state.streamTags().map(TagKey::id).map(Identifier::toString).forEach(list::add);
        return list;
    }
    @PetPetWhitelist
    public static PetPetList<String> getFluidTags(BlockState state) {
        PetPetList<String> list = new PetPetList<>();
        state.getFluidState().streamTags().map(TagKey::id).map(Identifier::toString).forEach(list::add);
        return list;
    }
    @PetPetWhitelist
    public static double getSlipperiness(BlockState state) {
        return state.getBlock().getSlipperiness();
    }
    @PetPetWhitelist
    public static double getBlastResistance(BlockState state) {
        return state.getBlock().getBlastResistance();
    }
    @PetPetWhitelist
    public static double getLuminance(BlockState state) {
        return state.getLuminance();
    }
    @PetPetWhitelist
    public static double getVelocityMultiplier(BlockState state) {
        return state.getBlock().getVelocityMultiplier();
    }
    @PetPetWhitelist
    public static double getJumpVelocityMultiplier(BlockState state) {
        return state.getBlock().getJumpVelocityMultiplier();
    }
    @PetPetWhitelist
    public static ItemStack asItem(BlockState state) {
        return new ItemStack(state.getBlock().asItem());
    }
    @PetPetWhitelist
    public static PetPetTable<String, PetPetList<?>> getProperties(BlockState state) {
        Collection<Property<?>> properties = state.getProperties();
        PetPetTable<String, PetPetList<?>> result = new PetPetTable<>();
        for (Property<?> property : properties) {
            PetPetList<Object> list = new PetPetList<>(property.getValues().size());
            for (Object option : property.getValues())
                list.add(option.toString());
            result.put(property.getName(), list);
        }
        return result;
    }


    /**
     * Many of the BlockState functions have 0, 1, or 2 argument
     * variants. The full 2-arg version has you supply the
     * block pos and the world, the 1-arg version just supplies the
     * block pos (and assumes the world is the currently loaded
     * client world), and the 0-arg version assumes the block pos
     * to be 0,0,0.
     * There are also 3-arg and 4-arg versions which pass in
     * 3 numbers instead of a vector.
     */

    private static void genWorldPosAcceptors(String name, TriFunction<BlockState, World, BlockPos, ?> baseFunc) {
        BLOCK_STATE_CLASS.addMethod(name + "_0", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object blockState) {
                return baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, BlockPos.ORIGIN);
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_1", new JavaFunction(false, 2) {
            @Override
            public Object invoke(Object blockState, Object pos) {
                Vector3d p = (Vector3d) pos;
                return baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, new BlockPos(p.x, p.y, p.z));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_2", new JavaFunction(false, 3) {
            @Override
            public Object invoke(Object blockState, Object pos, Object world) {
                Vector3d p = (Vector3d) pos;
                return baseFunc.apply((BlockState) blockState, (World) world, new BlockPos(p.x, p.y, p.z));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_3", new JavaFunction(false, 4) {
            @Override
            public Object invoke(Object blockState, Object x, Object y, Object z) {
                return baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, new BlockPos((Double) x, (Double) y, (Double) z));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_4", new JavaFunction(false, 5) {
            @Override
            public Object invoke(Object blockState, Object x, Object y, Object z, Object world) {
                return baseFunc.apply((BlockState) blockState, (World) world, new BlockPos((Double) x, (Double) y, (Double) z));
            }
        });
    }

    //Same concept, but with an additional transformer function applied to the output.
    private static <T, S> void genWorldPosAcceptorsWithTransformer(String name, TriFunction<BlockState, World, BlockPos, T> baseFunc, Function<T, S> outputTransformer) {
        BLOCK_STATE_CLASS.addMethod(name + "_0", new JavaFunction(false, 1) {
            @Override
            public Object invoke(Object blockState) {
                return outputTransformer.apply(baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, BlockPos.ORIGIN));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_1", new JavaFunction(false, 2) {
            @Override
            public Object invoke(Object blockState, Object pos) {
                Vector3d p = (Vector3d) pos;
                return outputTransformer.apply(baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, new BlockPos(p.x, p.y, p.z)));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_2", new JavaFunction(false, 3) {
            @Override
            public Object invoke(Object blockState, Object pos, Object world) {
                Vector3d p = (Vector3d) pos;
                return outputTransformer.apply(baseFunc.apply((BlockState) blockState, (World) world, new BlockPos(p.x, p.y, p.z)));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_3", new JavaFunction(false, 4) {
            @Override
            public Object invoke(Object blockState, Object x, Object y, Object z) {
                return outputTransformer.apply(baseFunc.apply((BlockState) blockState, MinecraftClient.getInstance().world, new BlockPos((Double) x, (Double) y, (Double) z)));
            }
        });
        BLOCK_STATE_CLASS.addMethod(name + "_4", new JavaFunction(false, 5) {
            @Override
            public Object invoke(Object blockState, Object x, Object y, Object z, Object world) {
                return outputTransformer.apply(baseFunc.apply((BlockState) blockState, (World) world, new BlockPos((Double) x, (Double) y, (Double) z)));
            }
        });
    }




}
