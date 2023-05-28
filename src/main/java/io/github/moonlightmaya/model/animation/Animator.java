package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.data.BaseStructures;
import io.github.moonlightmaya.model.Transformable;
import io.github.moonlightmaya.util.DataStructureUtils;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class Animator implements Transformable.Transformer {

    public final Channel pos, rot, scale;
    private boolean isActive = false;
    private final Animation animation;

    public Animator(BaseStructures.AnimatorStructure baseStructure, Aspect aspect) {
        int animIndex = baseStructure.animationIndex();
        animation = DataStructureUtils.valueAtIndex(aspect.animations, animIndex);
        animation.animators.add(this); //Add this to the animation's list
        pos = new Channel(animation, baseStructure.posKeyframes());
        rot = new Channel(animation, baseStructure.rotKeyframes());
        scale = new Channel(animation, baseStructure.scaleKeyframes());
    }

    /**
     * Updating the time will set the animator as active, and it will affect parts
     */
    public void updateTime(float newTime) {
        isActive = true;
        pos.updateTime(newTime);
        rot.updateTime(newTime);
        scale.updateTime(newTime);
    }

    /**
     * Will set the animator as inactive, not affecting the part or
     * forcing recalculation
     */
    public void deactivate() {
        isActive = false;
    }

    @Override
    public boolean forceRecalculation() {
        return isActive;
    }

    @Override
    public boolean affectMatrix(Matrix4f matrix) {
        if (isActive) {
            //Get weight, multiply by it
            float w = (float) animation.weight_0();
            //Values in keyframes are degrees, but we need radians
            //Also, blockbench reverses the X and Y values of rotations for animations vs. just rotating the parts
            matrix.rotateXYZ(rot.queryLatest(0,0,0).mul(w * MathHelper.PI / 180).mul(-1, -1, 1));
            matrix.scale(scale.queryLatest(1,1,1).sub(1,1,1).mul(w).add(1,1,1));
            matrix.translate(pos.queryLatest(0,0,0).mul(w));
        }
        return false;
    }

    public String toString() {
        return "Animator(keyframes=" + rot.keyframeCount() + "," + pos.keyframeCount() + "," + scale.keyframeCount() + ")";
    }
}
