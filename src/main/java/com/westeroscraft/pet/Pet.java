package com.westeroscraft.pet;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * Represents a pet owned by a player.
 * Pet data is stored in world PersistentState for persistence.
 */
public class Pet {
    private final UUID uuid;
    private final UUID ownerUuid;
    private final PetType type;
    private String customName;
    private int level;
    private float health;
    private boolean active;
    private NbtCompound extraData;

    public Pet(UUID uuid, UUID ownerUuid, PetType type) {
        this.uuid = uuid;
        this.ownerUuid = ownerUuid;
        this.type = type;
        this.customName = null;
        this.level = 1;
        this.health = 20.0f;
        this.active = false;
        this.extraData = new NbtCompound();
    }

    // Getters
    public UUID getUuid() {
        return uuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public PetType getType() {
        return type;
    }

    public String getCustomName() {
        return customName;
    }

    public int getLevel() {
        return level;
    }

    public float getHealth() {
        return health;
    }

    public boolean isActive() {
        return active;
    }

    public NbtCompound getExtraData() {
        return extraData;
    }

    // Setters
    public void setCustomName(String name) {
        this.customName = name;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setExtraData(NbtCompound data) {
        this.extraData = data != null ? data : new NbtCompound();
    }

    /**
     * Get display name for this pet (custom name or type name).
     */
    public String getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        // Capitalize first letter of type
        String typeName = type.getId();
        return typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
    }

    /**
     * Serialize this pet to NBT.
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("uuid", uuid);
        nbt.putUuid("ownerUuid", ownerUuid);
        nbt.putString("type", type.getId());
        if (customName != null) {
            nbt.putString("customName", customName);
        }
        nbt.putInt("level", level);
        nbt.putFloat("health", health);
        nbt.putBoolean("active", active);
        nbt.put("extraData", extraData);
        return nbt;
    }

    /**
     * Deserialize a pet from NBT.
     * @return the Pet, or null if data is invalid
     */
    public static Pet fromNbt(NbtCompound nbt) {
        if (!nbt.containsUuid("uuid") || !nbt.containsUuid("ownerUuid") || !nbt.contains("type")) {
            return null;
        }

        UUID uuid = nbt.getUuid("uuid");
        UUID ownerUuid = nbt.getUuid("ownerUuid");
        PetType type = PetType.fromId(nbt.getString("type"));

        if (type == null) {
            return null;
        }

        Pet pet = new Pet(uuid, ownerUuid, type);

        if (nbt.contains("customName")) {
            pet.setCustomName(nbt.getString("customName"));
        }
        if (nbt.contains("level")) {
            pet.setLevel(nbt.getInt("level"));
        }
        if (nbt.contains("health")) {
            pet.setHealth(nbt.getFloat("health"));
        }
        if (nbt.contains("active")) {
            pet.setActive(nbt.getBoolean("active"));
        }
        if (nbt.contains("extraData")) {
            pet.setExtraData(nbt.getCompound("extraData"));
        }

        return pet;
    }
}
