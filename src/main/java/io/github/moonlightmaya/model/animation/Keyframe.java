package io.github.moonlightmaya.model.animation;

/**
 * A keyframe is an object with the following capabilities:
 *
 * - A time value (probably in ticks? figura uses real time,
 * which might be useful for some fringe applications, but I
 * think using a tick-based system would be more consistent)
 *
 * - The ability to be __evaluated__ to a value of type T. This
 * value is a Vector3 for position and scale transforms, and
 * a Quaternion for rotation transforms.
 *
 * @param <T> The type which the keyframe evaluates to.
 */
public class Keyframe<T> {

}
