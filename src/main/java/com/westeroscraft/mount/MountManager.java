package com.westeroscraft.mount;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.westeroscraft.config.WesterosMobsConfig;
import com.westeroscraft.mixin.HorseInvoker;

import java.util.UUID;

/**
 * Manages horse lifecycle and player-horse tracking for the /mount command.
 * Mount data is persisted on players via mixin (survives server restarts).
 */
public class MountManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("westerosmobs");

    /**
     * Initialize event handlers for mount cleanup.
     */
    public static void init() {
        // Despawn horse when player disconnects
        // Must schedule on main thread since DISCONNECT fires on Netty IO thread
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            IPlayerMountData mountData = (IPlayerMountData) player;
            UUID horseUuid = mountData.westerosmobs$getMountUuid();
            ServerWorld world = player.getServerWorld();

            if (horseUuid != null) {
                server.execute(() -> {
                    if (world.getEntity(horseUuid) instanceof HorseEntity horse) {
                        horse.discard();
                        LOGGER.info("Despawned mount {} for disconnecting player {}", horseUuid, player.getName().getString());
                    }
                    mountData.westerosmobs$setMountUuid(null);
                });
            }
        });

        // Despawn horse when player changes dimension
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            // Only despawn if player actually changed to a different dimension
            if (origin != destination) {
                despawnMount(player, origin);
            }
        });

        LOGGER.info("Mount system initialized");
    }

    /**
     * Check if a player has an active mount.
     */
    public static boolean hasMount(ServerPlayerEntity player) {
        return ((IPlayerMountData) player).westerosmobs$getMountUuid() != null;
    }

    /**
     * Teleport existing mount to player, or spawn a new one.
     * Handles ghost mounts (stale UUID) by clearing data and respawning.
     */
    public static void teleportOrSpawnMount(ServerPlayerEntity player) {
        IPlayerMountData mountData = (IPlayerMountData) player;
        UUID horseUuid = mountData.westerosmobs$getMountUuid();

        if (horseUuid != null) {
            ServerWorld world = player.getServerWorld();
            if (world.getEntity(horseUuid) instanceof HorseEntity horse) {
                horse.refreshPositionAndAngles(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYaw(), 0.0F);
                return;
            }
            // Ghost mount: entity gone, clear stale data, fall through to spawn
            LOGGER.warn("Mount {} for player {} not found, respawning",
                horseUuid, player.getName().getString());
            mountData.westerosmobs$setMountUuid(null);
        }
        spawnMount(player);
    }

    /**
     * Spawn a tamed, saddled horse near the player.
     */
    public static void spawnMount(ServerPlayerEntity player) {
        if (!WesterosMobsConfig.mountEnabled) {
            return;
        }

        ServerWorld world = player.getServerWorld();

        // Create horse entity using constructor
        HorseEntity horse = new HorseEntity(EntityType.HORSE, world);

        // Use saved appearance or randomize on first spawn
        IPlayerMountData mountData = (IPlayerMountData) player;
        int savedColor = mountData.westerosmobs$getMountColor();
        int savedMarking = mountData.westerosmobs$getMountMarking();

        HorseColor color;
        HorseMarking marking;
        if (savedColor >= 0 && savedMarking >= 0) {
            color = HorseColor.byId(savedColor);
            marking = HorseMarking.byIndex(savedMarking);
        } else {
            Random random = world.getRandom();
            HorseColor[] variants = HorseColor.values();
            HorseMarking[] markingValues = HorseMarking.values();
            color = variants[random.nextInt(variants.length)];
            marking = markingValues[random.nextInt(markingValues.length)];
            mountData.westerosmobs$setMountColor(color.getId());
            mountData.westerosmobs$setMountMarking(marking.getId());
        }
        ((HorseInvoker) horse).invokeSetVariant(color, marking);

        // Position horse at player's location with player's rotation
        horse.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);

        // Tame the horse and set owner
        horse.setTame(true);
        horse.setOwnerUuid(player.getUuid());
        horse.setPersistent();    // Prevent despawn checks from removing the horse
        horse.setInvulnerable(true);  // Prevent death from damage

        // Equip saddle
        horse.saddle(new ItemStack(Items.SADDLE), SoundCategory.NEUTRAL);

        // Apply custom name if the player has one saved
        String customName = mountData.westerosmobs$getMountName();
        if (customName != null) {
            horse.setCustomName(Text.literal(customName));
            horse.setCustomNameVisible(true);
        }

        // Add horse to the world
        world.spawnEntity(horse);
        mountData.westerosmobs$setMountUuid(horse.getUuid());

        LOGGER.info("Spawned mount for player {} (horse UUID: {})", player.getName().getString(), horse.getUuid());
    }

    /**
     * Despawn a player's mount from a specific level.
     */
    public static void despawnMount(ServerPlayerEntity player, ServerWorld world) {
        IPlayerMountData mountData = (IPlayerMountData) player;
        UUID horseUuid = mountData.westerosmobs$getMountUuid();

        if (horseUuid == null) {
            return;
        }

        // Find and remove the horse entity
        if (world.getEntity(horseUuid) instanceof HorseEntity horse) {
            horse.discard();
            LOGGER.info("Despawned mount {} for player {}", horseUuid, player.getName().getString());
        }

        mountData.westerosmobs$setMountUuid(null);
    }

    /**
     * Despawn a player's mount using their current level.
     */
    public static void despawnMount(ServerPlayerEntity player) {
        despawnMount(player, player.getServerWorld());
    }

    /**
     * Set the horse name and update the live entity if one is spawned.
     */
    public static void renameMount(ServerPlayerEntity player, String name) {
        IPlayerMountData mountData = (IPlayerMountData) player;
        mountData.westerosmobs$setMountName(name);
        UUID horseUuid = mountData.westerosmobs$getMountUuid();
        if (horseUuid != null) {
            ServerWorld world = player.getServerWorld();
            if (world.getEntity(horseUuid) instanceof HorseEntity horse) {
                horse.setCustomName(Text.literal(name));
                horse.setCustomNameVisible(true);
            }
        }
    }
}
