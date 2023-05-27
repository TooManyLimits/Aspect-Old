package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Handles interpolating between keyframes
 */
@FunctionalInterface
public interface Interpolation {

    /**
     * @param keyframes The list of all keyframes in the animation channel
     * @param time The current "time" of the animation, NOT a percentage between this and the next
     * @param targetIndex The index of the keyframe we're moving towards right now.
     *                    It's assumed this is at least 1.
     */
    Vector3f interpolate(Keyframe[] keyframes, float time, int targetIndex, Vector3f out);

    static float calculateDelta(Keyframe[] frames, float time, int targetIndex) {
        return (time - frames[targetIndex-1].time) / (frames[targetIndex].time - frames[targetIndex-1].time);
    }

    Interpolation LERP = (frames, time, targetIndex, out) ->
        out.set(frames[targetIndex-1].evaluate()).lerp(frames[targetIndex].evaluate(), calculateDelta(frames, time, targetIndex));

    //Slerps between the values, interpreting rotation order as XYZ
    Interpolation SLERP = (frames, time, targetIndex, out) -> {
        Vector3f prevVec = frames[targetIndex-1].evaluate();
        Quaternionf prev = new Quaternionf().rotationXYZ(prevVec.x, prevVec.y, prevVec.z);
        Vector3f nextVec = frames[targetIndex].evaluate();
        Quaternionf next = new Quaternionf().rotationXYZ(nextVec.x, nextVec.y, nextVec.z);
        return prev.slerp(next, calculateDelta(frames, time, targetIndex)).getEulerAnglesXYZ(out);
    };

    Interpolation CATMULLROM = (frames, time, targetIndex, out) -> {
        float delta = calculateDelta(frames, time, targetIndex);
        Vector3f p0 = frames[Math.max(targetIndex - 2, 0)].evaluate();
        Vector3f p1 = frames[targetIndex - 1].evaluate();
        Vector3f p2 = frames[targetIndex].evaluate();
        Vector3f p3 = frames[Math.min(targetIndex+1, frames.length-1)].evaluate();
        return out.set(
                MathHelper.catmullRom(delta, p0.x, p1.x, p2.x, p3.x),
                MathHelper.catmullRom(delta, p0.y, p1.y, p2.y, p3.y),
                MathHelper.catmullRom(delta, p0.z, p1.z, p2.z, p3.z)
        );
    };

    Interpolation BEZIER = (frames, time, targetIndex, out) -> {
        //For bezier, can assume that one of them is a bezier keyframe
        //Calculate the T values accordingly
        Keyframe.BezierKeyframe bezier;
        Keyframe neighbor;
        boolean isNeighborLeft;
        if (frames[targetIndex-1] instanceof Keyframe.BezierKeyframe bez)  {
            bezier = bez;
            neighbor = frames[targetIndex];
            isNeighborLeft = false;
        } else {
            bezier = (Keyframe.BezierKeyframe) frames[targetIndex];
            neighbor = frames[targetIndex-1];
            isNeighborLeft = true;
        }
        Vector3f tValues = bezier.calculateTValue(neighbor, isNeighborLeft, time);

        //Get the control points
        Vector3f p0 = frames[targetIndex-1].evaluate();
        Vector3f p1 = new Vector3f(p0).add(frames[targetIndex-1].getBezierRightValue());
        Vector3f p3 = frames[targetIndex].evaluate();
        Vector3f p2 = new Vector3f(p3).add(frames[targetIndex].getBezierLeftValue());

        //Perform bezier things
        return out.set(
                MathUtils.bezier(p0.x, p1.x, p2.x, p3.x, tValues.x),
                MathUtils.bezier(p0.y, p1.y, p2.y, p3.y, tValues.y),
                MathUtils.bezier(p0.z, p1.z, p2.z, p3.z, tValues.z)
        );
    };



    Interpolation STEP = (frames, time, targetIndex, out) ->
            out.set(frames[targetIndex-1].evaluate());

    enum Builtin {
        LINEAR(Interpolation.LERP), //default
        CATMULLROM(Interpolation.CATMULLROM),
        SLERP(Interpolation.SLERP),
        BEZIER(Interpolation.BEZIER),
        STEP(Interpolation.STEP);

        public final Interpolation function;
        Builtin(Interpolation func) {
            this.function = func;
        }
    }
}
