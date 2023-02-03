package io.github.moonlightmaya;

import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.UUID;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {

    public AspectModelPart root; //temporarily public for testing
    private UUID user;

    public void renderCompatibly(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        root.render(vcp, matrixStack);
    }

}
