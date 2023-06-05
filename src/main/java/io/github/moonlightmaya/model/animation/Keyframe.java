package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.util.MathUtils;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import petpet.lang.run.PetPetCallable;
import petpet.lang.run.PetPetException;

/**
 * A keyframe is an object with the following capabilities:
 *
 * - A time value (probably in ticks? figura uses real time,
 * which might be useful for some fringe applications, but I
 * think using a tick-based system would be more consistent)
 *
 */
public abstract class Keyframe {
    public final float time;
    public Interpolation interpolation;

    public Keyframe(float time, Interpolation interpolation) {
        this.time = time;
        this.interpolation = interpolation;
    }

    public abstract Vector3f evaluate();

    //Bezier things
    public static final Vector3f NEGATIVE_ZERO_POINT_ONE = new Vector3f();
    public static final Vector3f ZERO_POINT_ONE = new Vector3f();
    public Vector3f getBezierLeftTime() {
        return NEGATIVE_ZERO_POINT_ONE;
    }
    public Vector3f getBezierLeftValue() {
        return MathUtils.ZERO_VEC_3F;
    }
    public Vector3f getBezierRightTime() {
        return ZERO_POINT_ONE;
    }
    public Vector3f getBezierRightValue() {
        return MathUtils.ZERO_VEC_3F;
    }

    public static class ConstantNumberKeyframe extends Keyframe {
        public final Vector3f value;
        public ConstantNumberKeyframe(float time, Interpolation interpolation, float x, float y, float z) {
            super(time, interpolation);
            value = new Vector3f(x, y, z);
        }
        @Override
        public Vector3f evaluate() {
            return value;
        }
    }

    public static class FunctionKeyframe extends Keyframe {
        //Some may be floats, some may be functions
        public final @Nullable PetPetCallable xFunction, yFunction, zFunction;
        public final String animName;
        public final @Nullable String xSrc, ySrc, zSrc; //source code of functions, for easier error reports
        public final @Nullable Float xValue, yValue, zValue;
        private final Vector3f saveAlloc = new Vector3f(); //Save on allocating a new vector on each evaluation
        public FunctionKeyframe(float time, Interpolation interpolation, Animation anim, Object x, Object y, Object z) {
            super(time, interpolation);

            //Error reporting
            animName = anim.name;
            xSrc = x instanceof String s ? s : null;
            ySrc = x instanceof String s ? s : null;
            zSrc = x instanceof String s ? s : null;

            Object xo = checkValue(anim, x, "X");
            xValue = xo instanceof Number n ? n.floatValue() : null;
            xFunction = xo instanceof PetPetCallable c ? c : null;

            Object yo = checkValue(anim, y, "Y");
            yValue = yo instanceof Number n ? n.floatValue() : null;
            yFunction = yo instanceof PetPetCallable c ? c : null;

            Object zo = checkValue(anim, z, "Z");
            zValue = zo instanceof Number n ? n.floatValue() : null;
            zFunction = zo instanceof PetPetCallable c ? c : null;

            //Ensure at least one of each is non-null
            if (
                    xValue == null && xFunction == null ||
                    yValue == null && yFunction == null ||
                    zValue == null && zFunction == null
            ) throw new RuntimeException("Unexpected error with keyframe parsing, keyframe has invalid values. Bug, send to devs!");
        }

        /**
         * Check the given object parameter, and return either a PetPetCallable or a Float
         */
        public Object checkValue(Animation anim, Object o, String axis) {
            String name = animName + ", keyframe at t=" + this.time + "," + axis + " value. CODE: [[" + o + "]]";
            return o instanceof String src ? anim.aspect.script.compile(name, src) : o;
        }

        @Override
        public Vector3f evaluate() {
            //Get x first
            Object x = xValue != null ? xValue : xFunction.call();

            //If x is a special type, no need to eval y and z
            if (x instanceof Vector3d vec)
                return saveAlloc.set(vec);

            //Evaluate y and z
            Object y = yValue != null ? yValue : yFunction.call();
            Object z = zValue != null ? zValue : zFunction.call();

            //Ensure doubles, show error code if not
            double dx, dy, dz;
            if (x instanceof Double d)
                dx = d;
            else throw new PetPetException("X field of keyframe at time " + this.time + " has invalid function, expected to return number. CODE: [[" + xSrc + "]]");
            if (y instanceof Double d2)
                dy = d2;
            else throw new PetPetException("Y field of keyframe at time " + this.time + " has invalid function, expected to return number. CODE: [[" + ySrc + "]]");
            if (z instanceof Double d3)
                dz = d3;
            else throw new PetPetException("Z field of keyframe at time " + this.time + " has invalid function, expected to return number. CODE: [[" + zSrc + "]]");

            //Return
            return saveAlloc.set(dx, dy, dz);
        }
    }

    /**
     * This wraps another keyframe object and steals its evaluate() value, but
     * also implements additional bezier information on top
     */
    public static class BezierKeyframe extends Keyframe {
        private final Keyframe wrapped;
        private final Vector3f bezierLeftTime, bezierLeftValue, bezierRightTime, bezierRightValue;
        public BezierKeyframe(Keyframe wrapped, Vector3f bezierLeftTime, Vector3f bezierLeftValue, Vector3f bezierRightTime, Vector3f bezierRightValue) {
            super(wrapped.time, Interpolation.BEZIER);
            this.wrapped = wrapped;
            this.bezierLeftTime = bezierLeftTime;
            this.bezierLeftValue = bezierLeftValue;
            this.bezierRightTime = bezierRightTime;
            this.bezierRightValue = bezierRightValue;
        }
        private final Vector3f tValueAvoidAlloc = new Vector3f();

        /**
         * Delegate evaluation to the wrapped value,
         * this class only provides bezier functionality
         */
        @Override
        public Vector3f evaluate() {
            return wrapped.evaluate();
        }

        /**
         * Calculate the bezier values for an interpolation with the left neighbor, and cache them
         */
        public Vector3f calculateTValues(Keyframe neighbor, boolean isNeighborLeft, float time) {
                //Get the 4 points
                Vector3f p0 = new Vector3f(isNeighborLeft ? neighbor.time : this.time);
                Vector3f p3 = new Vector3f(isNeighborLeft ? this.time : neighbor.time);
                Vector3f p1 = new Vector3f((isNeighborLeft ? neighbor : this).getBezierRightTime()).add(p0);
                Vector3f p2 = new Vector3f((isNeighborLeft ? this : neighbor).getBezierLeftTime()).add(p3);

                //Clamp p1 and p2 values between p0 and p3
                p1.set(
                        MathHelper.clamp(p1.x, p0.x, p3.x),
                        MathHelper.clamp(p1.y, p0.y, p3.y),
                        MathHelper.clamp(p1.z, p0.z, p3.z)
                );
                p2.set(
                        MathHelper.clamp(p2.x, p0.x, p3.x),
                        MathHelper.clamp(p2.y, p0.y, p3.y),
                        MathHelper.clamp(p2.z, p0.z, p3.z)
                );

                //Binary search go nyoom
                Vector3f start = new Vector3f(0);
                Vector3f end = new Vector3f(1);
                final int iterations = 8; //plenty
                for (int i = 0; i < iterations; i++) {
                    Vector3f test = tValueAvoidAlloc.set(start).add(end).mul(0.5f);
                    //Change start or end
                    if (MathUtils.bezier(p0.x, p1.x, p2.x, p3.x, test.x) < time) start.x = test.x; else end.x = test.x;
                    if (MathUtils.bezier(p0.y, p1.y, p2.y, p3.y, test.y) < time) start.y = test.y; else end.y = test.y;
                    if (MathUtils.bezier(p0.z, p1.z, p2.z, p3.z, test.z) < time) start.z = test.z; else end.z = test.z;
                }
                return tValueAvoidAlloc.set(start).add(end).mul(0.5f);
        }

        @Override
        public Vector3f getBezierLeftTime() {
            return bezierLeftTime;
        }
        @Override
        public Vector3f getBezierLeftValue() {
            return bezierLeftValue;
        }
        @Override
        public Vector3f getBezierRightTime() {
            return bezierRightTime;
        }
        @Override
        public Vector3f getBezierRightValue() {
            return bezierRightValue;
        }
    }
}
