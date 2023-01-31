package io.github.moonlightmaya;

import net.minecraft.client.render.VertexConsumerProvider;

import java.util.UUID;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {

    public AspectModelPart root; //temporarily public for testing
    private UUID user;


    public void renderCompatibly(VertexConsumerProvider vcp) {
        root.render(vcp, true);
    }

}
