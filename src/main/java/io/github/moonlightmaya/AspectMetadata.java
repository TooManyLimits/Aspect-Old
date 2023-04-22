package io.github.moonlightmaya;

import io.github.moonlightmaya.data.BaseStructures;
import org.joml.Vector3d;
import petpet.types.PetPetList;

public class AspectMetadata {

    public String name;
    public String version;
    public final Vector3d color;
    public final PetPetList<String> authors;

    public AspectMetadata(BaseStructures.MetadataStructure baseStructure) {
        this.name = baseStructure.name() == null ? "" : baseStructure.name();
        this.version = baseStructure.version() == null ? "" : baseStructure.version();
        this.color = baseStructure.color() == null ? new Vector3d(1,1,1) : new Vector3d(baseStructure.color());

        this.authors = new PetPetList<>();
        if (baseStructure.authors() != null)
            this.authors.addAll(baseStructure.authors());
    }

    public AspectMetadata(String name, String version, Vector3d color, PetPetList<String> authors) {
        this.name = name;
        this.version = version;
        this.color = color;
        this.authors = authors;
    }

    public AspectMetadata() {
        this("", "", new Vector3d(1), new PetPetList<>());
    }

}
