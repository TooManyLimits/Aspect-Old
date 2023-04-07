package io.github.moonlightmaya.vanilla;

import io.github.moonlightmaya.mixin.models.AnimalModelAccessor;
import io.github.moonlightmaya.mixin.models.PlayerEntityModelAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VanillaModelPartSorter {

    //Keys to map are strings if known, or ints otherwise
    private static final Map<EntityModel<?>, Map<Object, ModelPart>> MODEL_PARTS_BY_ENTITY_MODEL = new ConcurrentHashMap<>();

    /**
     * This map *might* fill up over time, causing a memory leak, when
     * resources are reloaded repeatedly? Since the EntityModel instances change?
     * I don't think it'll be much of a problem. If it is, I'll come back
     * later and add a solution to clear unused stuff.
     */
    public static Map<Object, ModelPart> getModelInfo(EntityModel<?> model) {
        return MODEL_PARTS_BY_ENTITY_MODEL.computeIfAbsent(model, VanillaModelPartSorter::structureModel);
    }

    private static Map<Object, ModelPart> structureModel(EntityModel<?> model) {
        Map<Object, ModelPart> result = new HashMap<>();
        if (model instanceof AnimalModel<?> animalModel) {
            Iterator<ModelPart> bodyParts = ((AnimalModelAccessor) animalModel).bodyParts().iterator();
            Iterator<ModelPart> headParts = ((AnimalModelAccessor) animalModel).headParts().iterator();
            if (animalModel instanceof BipedEntityModel<?> biped) {
                //All bipeds have these parts, so they go in the biped section
                putAliased(result, bodyParts.next(), "body");
                putAliased(result, bodyParts.next(), "rightarm", "right_arm");
                putAliased(result, bodyParts.next(), "leftarm", "left_arm");
                putAliased(result, bodyParts.next(), "rightleg", "right_leg");
                putAliased(result, bodyParts.next(), "leftleg", "left_leg");
                putAliased(result, bodyParts.next(), "hat");
                putAliased(result, headParts.next(), "head");

                if (biped instanceof PlayerEntityModel<?> player) {
                    putAliased(result, bodyParts.next(), "leftpants", "left_pants");
                    putAliased(result, bodyParts.next(), "rightpants", "right_pants");
                    putAliased(result, bodyParts.next(), "leftsleeve", "left_sleeve");
                    putAliased(result, bodyParts.next(), "rightsleeve", "right_sleeve");
                    putAliased(result, bodyParts.next(), "jacket");

                    //Ears and cape, special fields of player.
                    //We probably will need to handle several more special cases like these.
                    putAliased(result, ((PlayerEntityModelAccessor) player).getEar(), "ears");
                    putAliased(result, ((PlayerEntityModelAccessor) player).getCloak(), "cape");
                } else if (biped instanceof ArmorStandEntityModel armorStand) {
                    //Really not needed right now but why not
                    putAliased(result, bodyParts.next(), "leftstick", "left_stick");
                    putAliased(result, bodyParts.next(), "rightstick", "right_stick");
                    putAliased(result, bodyParts.next(), "shoulderstick", "shoulder_stick");
                    putAliased(result, bodyParts.next(), "baseplate", "base_plate");
                }
            } else if (animalModel instanceof FoxEntityModel<?> fox) {
                putAliased(result, bodyParts.next(), "body");
                putAliased(result, result.get("body").getChild(EntityModelPartNames.TAIL), "tail");
                putAliased(result, bodyParts.next(), "righthindleg", "right_hind_leg", "leg1");
                putAliased(result, bodyParts.next(), "lefthindleg", "left_hind_leg", "leg2");
                putAliased(result, bodyParts.next(), "rightfrontleg", "right_front_leg", "leg3");
                putAliased(result, bodyParts.next(), "leftfrontleg", "left_front_leg", "leg4");
                putAliased(result, headParts.next(), "head");
            }

            //In the default case we'll still store the model parts, just with default number keys
            double i = 0;
            while (bodyParts.hasNext()) result.put(i++, bodyParts.next());
            while (headParts.hasNext()) result.put(i++, headParts.next());

        } else if (model instanceof SinglePartEntityModel<?> singlePart) {
            //Single part models just have a single part :p
            putAliased(result, singlePart.getPart(), "root");
        }
        return result;
    }

    private static <K,V> void putAliased(Map<K, V> map, V v, K... keys) {
        for (K alias : keys)
            map.put(alias, v);
    }

}
