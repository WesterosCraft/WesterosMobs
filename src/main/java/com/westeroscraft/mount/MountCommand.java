package com.westeroscraft.mount;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.westeroscraft.LuckPermsIntegration;
import com.westeroscraft.config.WesterosMobsConfig;

/**
 * Registers and handles the /mount command.
 */
public class MountCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mount")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    // Check if command is run by a player
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        source.sendError(Text.literal("This command can only be used by players."));
                        return 0;
                    }

                    // Check if mount system is enabled
                    if (!WesterosMobsConfig.mount.enabled) {
                        source.sendError(Text.literal("The mount command is disabled."));
                        return 0;
                    }

                    // Check permission (fail-open)
                    if (!LuckPermsIntegration.hasPermission(player, WesterosMobsConfig.mount.permission)) {
                        source.sendError(Text.literal("You don't have permission to use this command."));
                        return 0;
                    }

                    // If player already has a mount, despawn it first
                    if (MountManager.hasMount(player)) {
                        MountManager.despawnMount(player);
                    }

                    // Spawn the mount
                    MountManager.spawnMount(player);

                    source.sendFeedback(() -> Text.literal("Your mount has been summoned!"), false);
                    return 1;
                }));
    }
}
