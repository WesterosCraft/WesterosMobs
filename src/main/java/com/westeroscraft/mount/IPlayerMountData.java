package com.westeroscraft.mount;

import java.util.UUID;

/**
 * Interface for accessing mount data stored on players via mixin.
 */
public interface IPlayerMountData {

    UUID westerosmobs$getMountUuid();

    void westerosmobs$setMountUuid(UUID uuid);

    String westerosmobs$getMountName();

    void westerosmobs$setMountName(String name);

    int westerosmobs$getMountColor();

    void westerosmobs$setMountColor(int color);

    int westerosmobs$getMountMarking();

    void westerosmobs$setMountMarking(int marking);
}
