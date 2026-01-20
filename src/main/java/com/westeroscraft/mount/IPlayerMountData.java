package com.westeroscraft.mount;

import java.util.UUID;

/**
 * Interface for accessing mount data stored on players via mixin.
 */
public interface IPlayerMountData {

    boolean westerosmobs$hasMount();

    void westerosmobs$setHasMount(boolean value);

    UUID westerosmobs$getMountUuid();

    void westerosmobs$setMountUuid(UUID uuid);
}
