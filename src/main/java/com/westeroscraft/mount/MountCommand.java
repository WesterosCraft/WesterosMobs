package com.westeroscraft.mount;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.westeroscraft.LuckPermsIntegration;
import com.westeroscraft.config.WesterosMobsConfig;

public class MountCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("horse")
                .executes(MountCommand::toggleMount)
                .then(CommandManager.literal("setname")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(MountCommand::setName))));
    }

    private static int toggleMount(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command can only be used by players."));
            return 0;
        }
        if (!WesterosMobsConfig.mountEnabled) {
            source.sendError(Text.literal("The mount command is disabled."));
            return 0;
        }
        if (!player.hasPermissionLevel(2) && !LuckPermsIntegration.hasPermission(player, "westerosmobs.horse")) {
            source.sendError(Text.literal("You don't have permission to use this command."));
            return 0;
        }

        MountManager.teleportOrSpawnMount(player);
        IPlayerMountData mountData = (IPlayerMountData) player;
        String horseName = mountData.westerosmobs$getMountName();
        source.sendFeedback(() -> Text.literal(
                horseName != null ? horseName + " has been summoned!" : "Your horse has been summoned!"), false);
        return 1;
    }

    private static int setName(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command can only be used by players."));
            return 0;
        }
        if (!WesterosMobsConfig.mountEnabled) {
            source.sendError(Text.literal("The mount command is disabled."));
            return 0;
        }
        if (!player.hasPermissionLevel(2) && !LuckPermsIntegration.hasPermission(player, "westerosmobs.horse.name")) {
            source.sendError(Text.literal("You don't have permission to name your horse."));
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        MountManager.renameMount(player, name);
        source.sendFeedback(() -> Text.literal("Horse name set to: " + name), false);
        return 1;
    }
}
