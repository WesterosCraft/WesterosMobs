package com.westeroscraft.pet;

import java.util.List;
import java.util.UUID;

/**
 * Interface for accessing pet ownership data stored on players via mixin.
 * Stores list of owned pet UUIDs and the currently active pet UUID.
 */
public interface IPlayerPetData {

    /**
     * Get the list of pet UUIDs owned by this player.
     */
    List<UUID> westerosmobs$getOwnedPetUuids();

    /**
     * Set the list of pet UUIDs owned by this player.
     */
    void westerosmobs$setOwnedPetUuids(List<UUID> uuids);

    /**
     * Add a pet UUID to the player's owned pets.
     */
    void westerosmobs$addOwnedPet(UUID petUuid);

    /**
     * Remove a pet UUID from the player's owned pets.
     */
    void westerosmobs$removeOwnedPet(UUID petUuid);

    /**
     * Get the currently active (spawned) pet UUID.
     */
    UUID westerosmobs$getActivePetUuid();

    /**
     * Set the currently active (spawned) pet UUID.
     */
    void westerosmobs$setActivePetUuid(UUID uuid);

    /**
     * Check if the player has any owned pets.
     */
    default boolean westerosmobs$hasOwnedPets() {
        return !westerosmobs$getOwnedPetUuids().isEmpty();
    }

    /**
     * Check if the player has an active (spawned) pet.
     */
    default boolean westerosmobs$hasActivePet() {
        return westerosmobs$getActivePetUuid() != null;
    }
}
