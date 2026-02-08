package com.westeroscraft.pet;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.westeroscraft.config.WesterosMobsConfig;
import com.westeroscraft.mixin.HorseInvoker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages pet lifecycle including spawning, despawning, and persistence.
 * Handles pet immortality by despawning instead of killing.
 */
public class PetManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("westerosmobs");

    // Track spawned pet entities by their pet UUID
    private static final Map<UUID, UUID> spawnedPetEntities = new HashMap<>();

    /**
     * Initialize event handlers for pet lifecycle management.
     */
    public static void init() {
        // Despawn pet when player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            IPlayerPetData petData = (IPlayerPetData) player;
            UUID activePetUuid = petData.westerosmobs$getActivePetUuid();
            ServerWorld world = player.getServerWorld();

            if (activePetUuid != null) {
                server.execute(() -> {
                    despawnPetEntity(world, activePetUuid);
                    petData.westerosmobs$setActivePetUuid(null);

                    // Mark pet as inactive in save data
                    PetSaveData saveData = PetSaveData.get(server);
                    saveData.deactivateAllPets(player.getUuid());

                    LOGGER.info("Despawned pet {} for disconnecting player {}",
                            activePetUuid, player.getName().getString());
                });
            }
        });

        // Despawn pet when player changes dimension
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (origin != destination) {
                IPlayerPetData petData = (IPlayerPetData) player;
                UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

                if (activePetUuid != null) {
                    despawnPetEntity(origin, activePetUuid);
                    petData.westerosmobs$setActivePetUuid(null);

                    // Mark pet as inactive
                    PetSaveData saveData = PetSaveData.get(player.getServer());
                    saveData.deactivateAllPets(player.getUuid());

                    player.sendMessage(Text.literal("Your pet has retreated due to dimension change."), false);
                    LOGGER.info("Despawned pet {} for dimension-changing player {}",
                            activePetUuid, player.getName().getString());
                }
            }
        });

        // Handle pet immortality - prevent death, despawn instead
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            UUID petUuid = getPetUuidForEntity(entity);
            if (petUuid != null) {
                // This is a pet - prevent death and despawn instead
                MinecraftServer server = entity.getServer();
                if (server != null) {
                    PetSaveData saveData = PetSaveData.get(server);
                    Pet pet = saveData.getPet(petUuid);
                    if (pet != null) {
                        // Notify owner
                        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(pet.getOwnerUuid());
                        if (owner != null) {
                            owner.sendMessage(Text.literal("Your " + pet.getDisplayName() + " has retreated! Use /wcm summon to call them back."), false);

                            IPlayerPetData petData = (IPlayerPetData) owner;
                            petData.westerosmobs$setActivePetUuid(null);
                        }

                        // Mark as inactive
                        pet.setActive(false);
                        saveData.updatePet(pet);

                        // Remove from spawned tracking
                        spawnedPetEntities.remove(petUuid);

                        LOGGER.info("Pet {} retreated due to fatal damage", petUuid);
                    }
                }

                // Despawn the entity
                entity.discard();

                // Return false to prevent death (though entity is already discarded)
                return false;
            }
            return true; // Allow death for non-pets
        });

        LOGGER.info("Pet system initialized");
    }

    /**
     * Check if an entity is a spawned pet and return its pet UUID.
     */
    private static UUID getPetUuidForEntity(LivingEntity entity) {
        UUID entityUuid = entity.getUuid();
        for (Map.Entry<UUID, UUID> entry : spawnedPetEntities.entrySet()) {
            if (entry.getValue().equals(entityUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Summon a player's active pet.
     * @return true if successful
     */
    public static boolean summonPet(ServerPlayerEntity player, Pet pet) {
        if (!WesterosMobsConfig.pet.enabled) {
            return false;
        }

        ServerWorld world = player.getServerWorld();
        LivingEntity entity = createPetEntity(world, pet);

        if (entity == null) {
            return false;
        }

        // Position entity at player's location
        entity.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);

        // Spawn the entity
        world.spawnEntity(entity);

        // Track the spawned entity
        spawnedPetEntities.put(pet.getUuid(), entity.getUuid());

        // Update player data
        IPlayerPetData petData = (IPlayerPetData) player;
        petData.westerosmobs$setActivePetUuid(pet.getUuid());

        // Mark pet as active in save data
        PetSaveData saveData = PetSaveData.get(player.getServer());
        saveData.setActivePet(player.getUuid(), pet.getUuid());

        LOGGER.info("Summoned pet {} ({}) for player {}",
                pet.getUuid(), pet.getType().getId(), player.getName().getString());

        return true;
    }

    /**
     * Dismiss a player's active pet.
     */
    public static void dismissPet(ServerPlayerEntity player) {
        IPlayerPetData petData = (IPlayerPetData) player;
        UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

        if (activePetUuid == null) {
            return;
        }

        // Despawn the entity
        despawnPetEntity(player.getServerWorld(), activePetUuid);

        // Clear player data
        petData.westerosmobs$setActivePetUuid(null);

        // Mark pet as inactive
        PetSaveData saveData = PetSaveData.get(player.getServer());
        saveData.deactivateAllPets(player.getUuid());

        LOGGER.info("Dismissed pet {} for player {}", activePetUuid, player.getName().getString());
    }

    /**
     * Create a new pet for a player.
     */
    public static Pet createPet(ServerPlayerEntity player, PetType type) {
        UUID petUuid = UUID.randomUUID();
        Pet pet = new Pet(petUuid, player.getUuid(), type);

        // Add to world save data
        PetSaveData saveData = PetSaveData.get(player.getServer());
        saveData.addPet(pet);

        // Track on player
        IPlayerPetData petData = (IPlayerPetData) player;
        petData.westerosmobs$addOwnedPet(petUuid);

        LOGGER.info("Created new pet {} ({}) for player {}",
                petUuid, type.getId(), player.getName().getString());

        return pet;
    }

    /**
     * Release (permanently delete) a pet.
     */
    public static void releasePet(ServerPlayerEntity player, Pet pet) {
        // Dismiss if active
        IPlayerPetData petData = (IPlayerPetData) player;
        if (pet.getUuid().equals(petData.westerosmobs$getActivePetUuid())) {
            dismissPet(player);
        }

        // Remove from player ownership
        petData.westerosmobs$removeOwnedPet(pet.getUuid());

        // Remove from save data
        PetSaveData saveData = PetSaveData.get(player.getServer());
        saveData.removePet(pet.getUuid());

        LOGGER.info("Released pet {} for player {}", pet.getUuid(), player.getName().getString());
    }

    /**
     * Check if a player has an active pet.
     */
    public static boolean hasActivePet(ServerPlayerEntity player) {
        return ((IPlayerPetData) player).westerosmobs$hasActivePet();
    }

    /**
     * Get the currently spawned pet entity for a player.
     */
    public static LivingEntity getSpawnedPetEntity(ServerPlayerEntity player) {
        IPlayerPetData petData = (IPlayerPetData) player;
        UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

        if (activePetUuid == null) {
            return null;
        }

        UUID entityUuid = spawnedPetEntities.get(activePetUuid);
        if (entityUuid == null) {
            return null;
        }

        Entity entity = player.getServerWorld().getEntity(entityUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    /**
     * Create the Minecraft entity for a pet.
     */
    private static LivingEntity createPetEntity(ServerWorld world, Pet pet) {
        PetType type = pet.getType();
        Random random = world.getRandom();

        LivingEntity entity;

        switch (type) {
            case HORSE -> {
                HorseEntity horse = new HorseEntity(EntityType.HORSE, world);

                // Randomize appearance
                HorseColor[] colors = HorseColor.values();
                HorseMarking[] markings = HorseMarking.values();
                ((HorseInvoker) horse).invokeSetVariant(
                        colors[random.nextInt(colors.length)],
                        markings[random.nextInt(markings.length)]
                );

                // Tame and saddle
                horse.setTame(true);
                horse.setOwnerUuid(pet.getOwnerUuid());
                horse.saddle(new ItemStack(Items.SADDLE), SoundCategory.NEUTRAL);

                // Apply custom name
                if (pet.getCustomName() != null) {
                    horse.setCustomName(Text.literal(pet.getCustomName()));
                    horse.setCustomNameVisible(true);
                }

                entity = horse;
            }

            case DIREWOLF -> {
                WolfEntity wolf = new WolfEntity(EntityType.WOLF, world);

                // Tame the wolf
                wolf.setTamed(true, true);
                wolf.setOwnerUuid(pet.getOwnerUuid());

                // Make it sit by default (player can toggle)
                wolf.setSitting(false);

                // Apply custom name
                if (pet.getCustomName() != null) {
                    wolf.setCustomName(Text.literal(pet.getCustomName()));
                    wolf.setCustomNameVisible(true);
                }

                entity = wolf;
            }

            default -> {
                LOGGER.warn("Unknown pet type: {}", type);
                return null;
            }
        }

        return entity;
    }

    /**
     * Despawn a pet entity by its pet UUID.
     */
    private static void despawnPetEntity(ServerWorld world, UUID petUuid) {
        UUID entityUuid = spawnedPetEntities.remove(petUuid);
        if (entityUuid != null) {
            Entity entity = world.getEntity(entityUuid);
            if (entity != null) {
                entity.discard();
            }
        }
    }
}
