package io.github.moonlightmaya.vanilla;

import io.github.moonlightmaya.mixin.ModelPartAccessor;
import net.minecraft.client.model.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for an aspect to interact with vanilla ModelPart objects
 */
public class VanillaPart {

    public final ModelPart referencedPart;

    private final Map<String, VanillaPart> children = new HashMap<>();

    /**
     * The transform applied _by vanilla_ to this part, which
     * we save and can then later query from code.
     */
    public final Matrix4f savedTransform = new Matrix4f();

    /**
     * The transform applied _to the vanilla part_ by aspect,
     * which can be modified through code.
     */
    public final Matrix4f appliedTransform = new Matrix4f();

    public VanillaPart(ModelPart part) {
        this.referencedPart = part;
        for (Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).getChildren().entrySet()) {
            children.put(child.getKey(), new VanillaPart(child.getValue()));
        }
    }

    public Stream<VanillaPart> traverse() {
        return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(VanillaPart::traverse));
    }

}
