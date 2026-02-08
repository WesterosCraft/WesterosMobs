package com.westeroscraft.pet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.WolfEntity;

/**
 * Enum defining available pet types.
 * Initially uses vanilla entities; custom entities will be added later.
 */
public enum PetType {
    HORSE("horse", EntityType.HORSE, HorseEntity.class, true),
    DIREWOLF("direwolf", EntityType.WOLF, WolfEntity.class, false);
    // DRAGON will be added later with custom entity

    private final String id;
    private final EntityType<? extends LivingEntity> entityType;
    private final Class<? extends LivingEntity> entityClass;
    private final boolean rideable;

    PetType(String id, EntityType<? extends LivingEntity> entityType,
            Class<? extends LivingEntity> entityClass, boolean rideable) {
        this.id = id;
        this.entityType = entityType;
        this.entityClass = entityClass;
        this.rideable = rideable;
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T extends LivingEntity> EntityType<T> getEntityType() {
        return (EntityType<T>) entityType;
    }

    public Class<? extends LivingEntity> getEntityClass() {
        return entityClass;
    }

    public boolean isRideable() {
        return rideable;
    }

    /**
     * Get a PetType by its string ID.
     * @return the PetType, or null if not found
     */
    public static PetType fromId(String id) {
        for (PetType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
