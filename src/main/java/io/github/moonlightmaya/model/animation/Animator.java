package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.model.Transformable;
import org.joml.Matrix4f;

public class Animator implements Transformable.Transformer {

    @Override
    public boolean forceRecalculation() {
        return false;
    }

    @Override
    public boolean affectMatrix(Matrix4f matrix) {
        return false;
    }
}
