package io.github.moonlightmaya;

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
    public Vector3d worldPos;

    @Override
    public void setWorldPos(Vector3d pos) {
        worldPos.set(pos);
    }
}
