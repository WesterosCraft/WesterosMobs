package com.westeroscraft;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Optional integration with LuckPerms for permission checks.
 */
public class LuckPermsIntegration {
    private static LuckPerms api = null;
    private static boolean checked = false;

    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            try {
                Class.forName("net.luckperms.api.LuckPermsProvider");
                api = LuckPermsProvider.get();
                WesterosMobs.LOGGER.info("LuckPerms integration enabled");
            } catch (ClassNotFoundException | IllegalStateException e) {
                api = null;
                WesterosMobs.LOGGER.info("LuckPerms not found, permission integration disabled");
            }
        }
        return api != null;
    }

    /**
     * Check if a player has a specific permission.
     * Returns true if LuckPerms is not available (fail-open)
     */
    public static boolean hasPermission(ServerPlayerEntity player, String permission) {
        if (!isAvailable()) {
            return true; // Fail-open when LuckPerms not installed
        }
        return hasPermissionStrict(player.getUuid(), permission);
    }

    /**
     * Check if a player has a specific permission.
     * Returns false if LuckPerms is not available (fail-closed).
     */
    public static boolean hasPermissionStrict(ServerPlayerEntity player, String permission) {
        if (!isAvailable()) {
            return false; // Fail-closed when LuckPerms not installed
        }
        return hasPermissionStrict(player.getUuid(), permission);
    }

    /**
     * Check if a player (by UUID) has a specific permission.
     */
    public static boolean hasPermissionStrict(UUID playerUuid, String permission) {
        if (!isAvailable()) {
            return false;
        }
        try {
            User user = api.getUserManager().getUser(playerUuid);
            if (user == null) {
                return false;
            }
            return user.getCachedData()
                    .getPermissionData(user.getQueryOptions())
                    .checkPermission(permission)
                    .asBoolean();
        } catch (Exception e) {
            WesterosMobs.LOGGER.warn("Error checking permission {} for {}: {}", permission, playerUuid, e.getMessage());
            return false;
        }
    }
}
