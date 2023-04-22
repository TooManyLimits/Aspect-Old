package io.github.moonlightmaya.script.apis;

import io.github.moonlightmaya.Aspect;
import org.joml.Vector3d;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.PetPetTable;
import petpet.types.immutable.PetPetListView;
import petpet.types.immutable.PetPetTableView;

import java.util.UUID;

/**
 * There is a good reason for this API to exist instead of supplying
 * the Aspect object directly. This is because of an issue of permission.
 *
 * Some script runtimes should be able to edit variables of an aspect,
 * while others should not be able to. This requirement has led to a couple
 * of workarounds being used - namely, the existence of this class (which wraps
 * together an Aspect instance, as well as whether this API should have write
 * access), as well as the workaround for the EntityAPI getAspect() method, which
 * is not reflectively generated and is instead created dynamically for each
 * Aspect's script runtime.
 */
@PetPetWhitelist
public class AspectAPI {
    public final Aspect aspect;
    public final boolean hasWriteAccess;
    public AspectAPI(Aspect aspect, boolean hasWriteAccess) {
        this.aspect = aspect;
        this.hasWriteAccess = hasWriteAccess;
    }

    @PetPetWhitelist
    public boolean canEdit() {
        return hasWriteAccess;
    }

    @PetPetWhitelist
    public String name() {
        return aspect.metadata.name;
    }

    @PetPetWhitelist
    public String version() {
        return aspect.metadata.version;
    }

    @PetPetWhitelist
    public AspectAPI color_3(double x, double y, double z) {
        if (!hasWriteAccess) throw new PetPetException("Cannot set color of aspect you don't own");
        aspect.metadata.color.set(x, y, z);
        return this;
    }

    @PetPetWhitelist
    public AspectAPI color_1(Vector3d c) {
        if (!hasWriteAccess) throw new PetPetException("Cannot set color of aspect you don't own");
        aspect.metadata.color.set(c);
        return this;
    }

    @PetPetWhitelist
    public Vector3d color_0() {
        return new Vector3d(aspect.metadata.color);
    }

    @PetPetWhitelist
    public boolean isHost() {
        return aspect.isHost;
    }

    @PetPetWhitelist
    public PetPetList<String> getAuthors() {
        return hasWriteAccess ? aspect.metadata.authors : new PetPetListView<>(aspect.metadata.authors);
    }

    @PetPetWhitelist
    public PetPetTable<Object, Object> vars() {
        //Depending on write access, either give the table, or just a view.
        return hasWriteAccess ? aspect.aspectVars : new PetPetTableView<>(aspect.aspectVars);
    }

    @PetPetWhitelist
    public String getUserUUID() {
        return aspect.userUUID.toString();
    }

    public String toString() {
        return "Aspect(user=" + getUserUUID() + ")";
    }

}
