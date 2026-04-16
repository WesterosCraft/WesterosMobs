package com.westeroscraft.mount;

import com.westeroscraft.config.WesterosMobsConfig;
import com.westeroscraft.mixin.HorseInvoker;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
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

import java.util.UUID;

/**
 * Manages horse lifecycle and player-horse tracking for the /horse command.
 * Mount data is persisted on players via mixin (survives server restarts).
 */
public class MountManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("westerosmobs");

    public static void init() {
        // Despawn horse when player disconnects (must schedule on main thread)
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
            if (origin != destination) {
                despawnMount(player, origin);
            }
        });

        LOGGER.info("Mount system initialized");
    }

    public static boolean hasMount(ServerPlayerEntity player) {
        return ((IPlayerMountData) player).westerosmobs$getMountUuid() != null;
    }

    /**
     * Teleport existing mount to player, or spawn a new one.
     * Handles ghost mounts (stale UUID) by clearing and respawning.
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
            LOGGER.warn("Mount {} for player {} not found, respawning", horseUuid, player.getName().getString());
            mountData.westerosmobs$setMountUuid(null);
        }
        spawnMount(player);
    }

    /**
     * Spawn a tamed, saddled horse near the player using saved appearance,
     * or randomising on first spawn.
     */
    public static void spawnMount(ServerPlayerEntity player) {
        if (!WesterosMobsConfig.mountEnabled) return;

        ServerWorld world = player.getServerWorld();
        HorseEntity horse = new HorseEntity(EntityType.HORSE, world);

        IPlayerMountData mountData = (IPlayerMountData) player;
        int savedColor   = mountData.westerosmobs$getMountColor();
        int savedMarking = mountData.westerosmobs$getMountMarking();

        HorseColor color;
        HorseMarking marking;
        if (savedColor >= 0 && savedMarking >= 0) {
            color   = HorseColor.byId(savedColor);
            marking = HorseMarking.byIndex(savedMarking);
        } else {
            Random random = world.getRandom();
            color   = HorseColor.values()[random.nextInt(HorseColor.values().length)];
            marking = HorseMarking.values()[random.nextInt(HorseMarking.values().length)];
            mountData.westerosmobs$setMountColor(color.getId());
            mountData.westerosmobs$setMountMarking(marking.getId());
        }
        ((HorseInvoker) horse).invokeSetVariant(color, marking);

        horse.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        horse.setTame(true);
        horse.setOwnerUuid(player.getUuid());
        horse.setPersistent();
        horse.setInvulnerable(true);
        horse.saddle(new ItemStack(Items.SADDLE), SoundCategory.NEUTRAL);

        String customName = mountData.westerosmobs$getMountName();
        if (customName != null) {
            horse.setCustomName(Text.literal(customName));
            horse.setCustomNameVisible(true);
        }

        world.spawnEntity(horse);
        mountData.westerosmobs$setMountUuid(horse.getUuid());
        LOGGER.info("Spawned mount for player {} (UUID: {})", player.getName().getString(), horse.getUuid());
    }

    public static void despawnMount(ServerPlayerEntity player, ServerWorld world) {
        IPlayerMountData mountData = (IPlayerMountData) player;
        UUID horseUuid = mountData.westerosmobs$getMountUuid();
        if (horseUuid == null) return;

        if (world.getEntity(horseUuid) instanceof HorseEntity horse) {
            horse.discard();
            LOGGER.info("Despawned mount {} for player {}", horseUuid, player.getName().getString());
        }
        mountData.westerosmobs$setMountUuid(null);
    }

    public static void despawnMount(ServerPlayerEntity player) {
        despawnMount(player, player.getServerWorld());
    }

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
