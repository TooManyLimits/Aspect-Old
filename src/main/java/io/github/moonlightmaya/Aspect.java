package io.github.moonlightmaya;

import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.List;
import java.util.UUID;

/**
 * An entity can equip one or more Aspects, which will alter the entity's physical appearance.
 */
public class Aspect {


    //Temporarily public for testing
    public AspectModelPart entityRoot;
    public List<WorldRootModelPart> worldRoots;
    public AspectModelPart skullRoot;
    public AspectModelPart hudRoot;
    public AspectModelPart portraitRoot;

    private UUID user;

    public void renderCompatibly(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        entityRoot.render(vcp, matrixStack);
    }

}
