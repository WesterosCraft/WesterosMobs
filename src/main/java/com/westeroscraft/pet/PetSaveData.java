package com.westeroscraft.pet;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * World-level persistent storage for pet data.
 * Stores detailed information about all pets in the world.
 */
public class PetSaveData extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger("westerosmobs");
    private static final String DATA_NAME = "westerosmobs_pets";

    // All pets indexed by their UUID
    private final Map<UUID, Pet> petsById = new HashMap<>();

    // Pets indexed by owner UUID for quick lookup
    private final Map<UUID, List<UUID>> petsByOwner = new HashMap<>();

    public PetSaveData() {
    }

    /**
     * Get a pet by its UUID.
     */
    public Pet getPet(UUID petUuid) {
        return petsById.get(petUuid);
    }

    /**
     * Get all pets owned by a player.
     */
    public List<Pet> getPetsByOwner(UUID ownerUuid) {
        List<UUID> petUuids = petsByOwner.get(ownerUuid);
        if (petUuids == null) {
            return Collections.emptyList();
        }
        List<Pet> pets = new ArrayList<>();
        for (UUID petUuid : petUuids) {
            Pet pet = petsById.get(petUuid);
            if (pet != null) {
                pets.add(pet);
            }
        }
        return pets;
    }

    /**
     * Add a new pet.
     */
    public void addPet(Pet pet) {
        petsById.put(pet.getUuid(), pet);
        petsByOwner.computeIfAbsent(pet.getOwnerUuid(), k -> new ArrayList<>())
                .add(pet.getUuid());
        markDirty();
    }

    /**
     * Remove a pet permanently.
     */
    public void removePet(UUID petUuid) {
        Pet pet = petsById.remove(petUuid);
        if (pet != null) {
            List<UUID> ownerPets = petsByOwner.get(pet.getOwnerUuid());
            if (ownerPets != null) {
                ownerPets.remove(petUuid);
                if (ownerPets.isEmpty()) {
                    petsByOwner.remove(pet.getOwnerUuid());
                }
            }
            markDirty();
        }
    }

    /**
     * Update a pet's data.
     */
    public void updatePet(Pet pet) {
        if (petsById.containsKey(pet.getUuid())) {
            petsById.put(pet.getUuid(), pet);
            markDirty();
        }
    }

    /**
     * Get the active pet for a player.
     */
    public Pet getActivePet(UUID ownerUuid) {
        List<Pet> pets = getPetsByOwner(ownerUuid);
        for (Pet pet : pets) {
            if (pet.isActive()) {
                return pet;
            }
        }
        return null;
    }

    /**
     * Set a pet as active for its owner, deactivating others.
     */
    public void setActivePet(UUID ownerUuid, UUID petUuid) {
        List<Pet> pets = getPetsByOwner(ownerUuid);
        for (Pet pet : pets) {
            pet.setActive(pet.getUuid().equals(petUuid));
        }
        markDirty();
    }

    /**
     * Deactivate all pets for an owner.
     */
    public void deactivateAllPets(UUID ownerUuid) {
        List<Pet> pets = getPetsByOwner(ownerUuid);
        for (Pet pet : pets) {
            pet.setActive(false);
        }
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList petList = new NbtList();
        for (Pet pet : petsById.values()) {
            petList.add(pet.toNbt());
        }
        nbt.put("pets", petList);
        return nbt;
    }

    public static PetSaveData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        PetSaveData data = new PetSaveData();

        if (nbt.contains("pets", NbtElement.LIST_TYPE)) {
            NbtList petList = nbt.getList("pets", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < petList.size(); i++) {
                Pet pet = Pet.fromNbt(petList.getCompound(i));
                if (pet != null) {
                    data.petsById.put(pet.getUuid(), pet);
                    data.petsByOwner.computeIfAbsent(pet.getOwnerUuid(), k -> new ArrayList<>())
                            .add(pet.getUuid());
                }
            }
        }

        LOGGER.info("Loaded {} pets from world data", data.petsById.size());
        return data;
    }

    private static final PersistentState.Type<PetSaveData> TYPE = new PersistentState.Type<>(
            PetSaveData::new,
            PetSaveData::fromNbt,
            DataFixTypes.LEVEL
    );

    /**
     * Get the PetSaveData for a server.
     */
    public static PetSaveData get(MinecraftServer server) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return stateManager.getOrCreate(TYPE, DATA_NAME);
    }
}
