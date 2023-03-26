package io.github.moonlightmaya.vanilla;

import io.github.moonlightmaya.mixin.models.AnimalModelAccessor;
import io.github.moonlightmaya.mixin.models.PlayerEntityModelAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.*;

import java.util.*;

public class VanillaModelPartSorter {

    //Keys to map are strings if known, or ints otherwise
    private static final Map<EntityModel<?>, Map<Object, ModelPart>> MODEL_PARTS_BY_ENTITY_MODEL = new HashMap<>();

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
                result.put("BODY", bodyParts.next());
                result.put("RIGHT_ARM", bodyParts.next());
                result.put("LEFT_ARM", bodyParts.next());
                result.put("RIGHT_LEG", bodyParts.next());
                result.put("LEFT_LEG", bodyParts.next());
                result.put("HAT", bodyParts.next());
                result.put("HEAD", headParts.next());

                if (biped instanceof PlayerEntityModel<?> player) {
                    result.put("LEFT_PANTS", bodyParts.next());
                    result.put("RIGHT_PANTS", bodyParts.next());
                    result.put("LEFT_SLEEVE", bodyParts.next());
                    result.put("RIGHT_SLEEVE", bodyParts.next());
                    result.put("JACKET", bodyParts.next());

                    //Ears and cape, special fields of player.
                    //We probably will need to handle several more special cases like these.
                    result.put("EARS", ((PlayerEntityModelAccessor) player).getEar());
                    result.put("CAPE", ((PlayerEntityModelAccessor) player).getCloak());
                } else if (biped instanceof ArmorStandEntityModel armorStand) {
                    //Really not needed right now but why not
                    result.put("LEFT_STICK", bodyParts.next());
                    result.put("RIGHT_STICK", bodyParts.next());
                    result.put("SHOULDER_STICK", bodyParts.next());
                    result.put("BASEPLATE", bodyParts.next());
                }
            }

            //In the default case we'll still store the model parts, just with default number keys
            int i = 0;
            while (bodyParts.hasNext()) result.put(i, bodyParts.next());
            while (headParts.hasNext()) result.put(i, headParts.next());

        } else if (model instanceof SinglePartEntityModel<?> singlePart) {
            //Single part models just have a single part :p
            result.put("ROOT", singlePart.getPart());
        }
        return result;
    }

}
