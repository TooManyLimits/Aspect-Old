package io.github.moonlightmaya;

import io.github.moonlightmaya.util.AspectMatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Vector3d;

/**
 * This class is a special case because it needs to contain a high-precision world vector.
 * This is used when rendering in order to counteract the floating point issues that Figura
 * world-parented model parts face at high coordinates.
 */
public class WorldRootModelPart extends AspectModelPart {

    /**
     * The world position of this root model part. Has a higher precision than other values,
     * double precision. This property only exists on the roots of world model trees, and
     * exists to counteract the problems of floating point precision at high coordinate values.
     */
    public final Vector3d worldPos = new Vector3d();

    public void setWorldPos(Vector3d pos) {
        worldPos.set(pos);
    }

    public void renderWorld(VertexConsumerProvider vcp, AspectMatrixStack matrixStack, double cameraX, double cameraY, double cameraZ) {
        matrixStack.translate(worldPos.x - cameraX, worldPos.y - cameraY, worldPos.z - cameraZ);
        super.render(vcp, matrixStack);
    }

    /**
     * Do not call this method for world root parts, as they need the camera position.
     * Use the renderWorld() method, which passes in the camera position.
     */
    @Override
    @Deprecated
    public void render(VertexConsumerProvider vcp, AspectMatrixStack matrixStack) {
        throw new UnsupportedOperationException("World parts cannot be rendered using this method! Use renderWorld().");
    }
}
