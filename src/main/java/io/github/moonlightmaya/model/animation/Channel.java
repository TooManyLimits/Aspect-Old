package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.manage.data.BaseStructures;
import org.joml.Vector3f;

import java.util.List;

/**
 * Represents one list of keyframes that can be queried with times for values
 */
public class Channel {

    private final Keyframe[] keyframes;

    /**
     * Tracks the current index and time, so it can
     * easily increment/change in response to updates
     */
    private int curTarget = 1;
    private float curTime;
    private Interpolation curInterpolation = null;

    public Channel(Animation animation, List<BaseStructures.KeyframeStructure> baseStructure) {
        this.keyframes = new Keyframe[baseStructure.size()];
        int i = 0;
        for (BaseStructures.KeyframeStructure kfStructure : baseStructure) {
            Keyframe kf;
            float time = kfStructure.time();

            //Create base keyframe
            Interpolation interpolation = kfStructure.interpolation().function;
            if (kfStructure.isFunction()) {
                kf = new Keyframe.FunctionKeyframe(time, interpolation, animation, kfStructure.x(), kfStructure.y(), kfStructure.z());
            } else {
                float x = ((Number) kfStructure.x()).floatValue();
                float y = ((Number) kfStructure.y()).floatValue();
                float z = ((Number) kfStructure.z()).floatValue();
                kf = new Keyframe.ConstantNumberKeyframe(time, interpolation, x, y, z);
            }

            //If bezier, wrap with extra data
            if (kfStructure.isBezier()) {
                kf = new Keyframe.BezierKeyframe(kf,
                        kfStructure.bezierLeftTime(),
                        kfStructure.bezierLeftValue(),
                        kfStructure.bezierRightTime(),
                        kfStructure.bezierRightValue()
                );
            }
            keyframes[i++] = kf;
        }
    }

    /**
     * The query functions do not update the time,
     * they just grab the value at a specific time
     * without updating any state.
     */
    public Vector3f query(float time, Vector3f out) {
        if (this.keyframes.length == 0)
            return out.set(0, 0, 0);

        if (time <= keyframes[0].time) return keyframes[0].evaluate();
        if (time >= keyframes[keyframes.length-1].time) return keyframes[keyframes.length-1].evaluate();

        Keyframe start = keyframes[0];
        Keyframe end = keyframes[1];
        int i;
        for (i = 1; i < keyframes.length; i++) {
            if (keyframes[i].time > time) {
                end = keyframes[i];
                start = keyframes[i-1];
                break;
            }
        }

        Interpolation interpolation = getInterpolation(start, end);
        return interpolation.interpolate(keyframes, time, i, out);
    }

    public Vector3f query(float time) {
        return query(time, noAlloc);
    }

    /**
     * Chooser taken from blockbench code,
     * https://github.com/JannisX11/blockbench/blob/0bc15c4da7e19900a47d856102610a587c69c0de/js/animations/timeline_animators.js#L408
     */
    private static Interpolation getInterpolation(Keyframe start, Keyframe end) {
        if (start.interpolation == end.interpolation) return start.interpolation;
        if (start.interpolation == Interpolation.LERP && end.interpolation == Interpolation.STEP) return Interpolation.LERP;
        if (start.interpolation == Interpolation.CATMULLROM || end.interpolation == Interpolation.CATMULLROM) return Interpolation.CATMULLROM;
        if (start.interpolation == Interpolation.BEZIER || end.interpolation == Interpolation.BEZIER) return Interpolation.BEZIER;
        if (start.interpolation == Interpolation.SLERP || end.interpolation == Interpolation.SLERP) return Interpolation.SLERP; //except this one
        return start.interpolation; //Default
    }

    //A more optimized query that skips the search and bases it off of the last updateTime
    private final Vector3f noAlloc = new Vector3f();
    public Vector3f queryLatest(float defaultX, float defaultY, float defaultZ) {
        if (this.keyframes.length == 0) return noAlloc.set(defaultX, defaultY, defaultZ);
        return curInterpolation.interpolate(this.keyframes, curTime, curTarget, noAlloc);
    }

    public void updateTime(float newTime) {
        //Empty channels do nothing
        if (this.keyframes.length == 0) return;


        if (newTime > curTime) {
            while (curTarget < this.keyframes.length - 1 && this.keyframes[curTarget].time <= newTime) {
                curTarget++;
            }
        } else if (newTime < curTime) {
            while (curTarget > 1 && this.keyframes[curTarget-1].time > newTime) {
                curTarget--;
            }
        }

        //Update interpolation
        if (newTime != curTime) {
            Keyframe start = this.keyframes[Math.max(0, curTarget - 1)];
            Keyframe end = this.keyframes[Math.min(curTarget, this.keyframes.length - 1)];
            curInterpolation = getInterpolation(start, end);
        }
        this.curTime = newTime;

    }

    public int keyframeCount() {
        return keyframes.length;
    }




}
