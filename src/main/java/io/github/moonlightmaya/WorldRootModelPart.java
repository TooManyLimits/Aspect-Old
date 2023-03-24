package io.github.moonlightmaya;

import io.github.moonlightmaya.data.BaseStructures;
import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Vector3d;

/**
 * This class is a special case because it needs to contain a high-precision world vector.
 * This is used when rendering in order to counteract the floating point issues that Figura's
 * world-parented model parts face at high coordinates.
 */
public class WorldRootModelPart extends AspectModelPart {

    /**
     * The world position of this root model part. Has a higher precision than other values,
     * double precision. This property only exists on the roots of world model trees, and
     * exists to counteract the problems of floating point precision at high coordinate values.
     */
    public final Vector3d worldPos = new Vector3d();

    public WorldRootModelPart(BaseStructures.ModelPartStructure nbt, Aspect owningAspect) {
        super(nbt, owningAspect);
    }

    public void setWorldPos(Vector3d pos) {
        worldPos.set(pos);
    }
    public void setWorldPos(double x, double y, double z) {
        worldPos.set(x, y, z);
    }

    @Override
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        matrixStack.push();
        matrixStack.translate(worldPos);
        super.render(vcp, matrixStack);
        matrixStack.pop();
    }
}
