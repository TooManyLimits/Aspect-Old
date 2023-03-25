package io.github.moonlightmaya.vanilla;

import org.joml.Matrix4f;

import java.util.Stack;

/**
 * Meant to interface with elements of an EntityRenderer
 * or one of its subclasses.
 */
public class VanillaRenderer {

    /**
     * Mixins will modify values in the top value of CURRENT_RENDERER.
     * The current renderer pushed when we begin rendering an entity,
     * and is popped when we finish rendering it.
     * The reason it's a stack is that it's possible that
     * entities could be rendered recursively using this feature, so
     * we need to be able to handle multiple layers deep of renderers
     * at the same time.
     */
    public static final Stack<VanillaRenderer> CURRENT_RENDERER = new Stack<>();

    /**
     * The transformation applied to the entire model.
     */
    public final Matrix4f modelTransform = new Matrix4f();




}
