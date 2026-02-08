package com.westeroscraft.pet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.westeroscraft.LuckPermsIntegration;
import com.westeroscraft.config.WesterosMobsConfig;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Registers and handles the /wcm command tree.
 */
public class PetCommand {

    private static final SuggestionProvider<ServerCommandSource> PET_TYPE_SUGGESTIONS =
            (context, builder) -> CommandSource.suggestMatching(
                    Arrays.stream(PetType.values()).map(PetType::getId),
                    builder
            );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wcm")
                // /wcm - show help
                .executes(PetCommand::showHelp)

                // /wcm summon [type] [name] - summon active pet or create new one
                .then(CommandManager.literal("summon")
                        .executes(PetCommand::summonActivePet)
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests(PET_TYPE_SUGGESTIONS)
                                .executes(PetCommand::summonNewPet)
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(PetCommand::summonNewPet))))

                // /wcm dismiss - despawn active pet
                .then(CommandManager.literal("dismiss")
                        .executes(PetCommand::dismissPet))

                // /wcm list - list owned pets
                .then(CommandManager.literal("list")
                        .executes(PetCommand::listPets))

                // /wcm select <number> - select a pet by list number
                .then(CommandManager.literal("select")
                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                                .executes(PetCommand::selectPet)))

                // /wcm rename <name> - rename active pet
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(PetCommand::renamePet)))

                // /wcm info - show info about active pet
                .then(CommandManager.literal("info")
                        .executes(PetCommand::petInfo))

                // /wcm release <number> - permanently release a pet
                .then(CommandManager.literal("release")
                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                                .executes(PetCommand::releasePet)))

                // /wcm admin - admin commands
                .then(CommandManager.literal("admin")
                        // /wcm admin give <player> <type> [name]
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                .suggests(PET_TYPE_SUGGESTIONS)
                                                .executes(PetCommand::adminGivePet)
                                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                        .executes(PetCommand::adminGivePet)))))
                        // /wcm admin remove <player> <number>
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                                                .executes(PetCommand::adminRemovePet)))))
        );
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("=== Pet Commands ==="), false);
        source.sendFeedback(() -> Text.literal("/wcm summon [type] - Summon a pet"), false);
        source.sendFeedback(() -> Text.literal("/wcm dismiss - Despawn active pet"), false);
        source.sendFeedback(() -> Text.literal("/wcm list - List your pets"), false);
        source.sendFeedback(() -> Text.literal("/wcm select <#> - Select active pet"), false);
        source.sendFeedback(() -> Text.literal("/wcm rename <name> - Rename active pet"), false);
        source.sendFeedback(() -> Text.literal("/wcm info - Show pet details"), false);
        source.sendFeedback(() -> Text.literal("/wcm release <#> - Release a pet"), false);
        source.sendFeedback(() -> Text.literal("Available types: horse, direwolf"), false);
        return 1;
    }

    private static int summonActivePet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();

        if (!checkPermission(player, "westerosmobs.pet", source)) {
            return 0;
        }

        // If player has active pet, dismiss it first (toggle behavior)
        if (PetManager.hasActivePet(player)) {
            PetManager.dismissPet(player);
            source.sendFeedback(() -> Text.literal("Your pet has been dismissed."), false);
            return 1;
        }

        // Try to summon the player's selected pet
        PetSaveData saveData = PetSaveData.get(player.getServer());
        Pet activePet = saveData.getActivePet(player.getUuid());

        // If no active pet selected, get the first owned pet
        if (activePet == null) {
            List<Pet> pets = saveData.getPetsByOwner(player.getUuid());
            if (pets.isEmpty()) {
                source.sendError(Text.literal("You don't have any pets! Use /wcm summon <type> to get one."));
                return 0;
            }
            activePet = pets.get(0);
        }

        if (PetManager.summonPet(player, activePet)) {
            final String petName = activePet.getDisplayName();
            source.sendFeedback(() -> Text.literal("Your " + petName + " has been summoned!"), false);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to summon pet."));
            return 0;
        }
    }

    private static int summonNewPet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        String typeId = StringArgumentType.getString(context, "type");

        // Check permission for specific pet type
        String typePermission = "westerosmobs.pet." + typeId;
        if (!checkPermission(player, typePermission, source)) {
            return 0;
        }

        PetType type = PetType.fromId(typeId);
        if (type == null) {
            source.sendError(Text.literal("Unknown pet type: " + typeId + ". Available: horse, direwolf"));
            return 0;
        }

        // Dismiss current pet if any
        if (PetManager.hasActivePet(player)) {
            PetManager.dismissPet(player);
        }

        // Check one-per-type limit
        PetSaveData saveData = PetSaveData.get(player.getServer());
        List<Pet> existingPets = saveData.getPetsByOwner(player.getUuid());
        for (Pet existing : existingPets) {
            if (existing.getType() == type) {
                source.sendError(Text.literal("You already have a " + type.getId() + "."));
                return 0;
            }
        }

        // Create and summon new pet
        String name = getOptionalString(context, "name");
        Pet pet = PetManager.createPet(player, type, name);
        if (PetManager.summonPet(player, pet)) {
            source.sendFeedback(() -> Text.literal("A new " + type.getId() + " has joined you!"), false);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to summon pet."));
            return 0;
        }
    }

    private static int dismissPet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();

        if (!PetManager.hasActivePet(player)) {
            source.sendError(Text.literal("You don't have an active pet."));
            return 0;
        }

        PetManager.dismissPet(player);
        source.sendFeedback(() -> Text.literal("Your pet has been dismissed."), false);
        return 1;
    }

    private static int listPets(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        PetSaveData saveData = PetSaveData.get(player.getServer());
        List<Pet> pets = saveData.getPetsByOwner(player.getUuid());

        if (pets.isEmpty()) {
            source.sendFeedback(() -> Text.literal("You don't have any pets yet."), false);
            return 1;
        }

        source.sendFeedback(() -> Text.literal("=== Your Pets ==="), false);
        IPlayerPetData petData = (IPlayerPetData) player;
        UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

        for (int i = 0; i < pets.size(); i++) {
            Pet pet = pets.get(i);
            final int num = i + 1;
            String active = pet.getUuid().equals(activePetUuid) ? " [ACTIVE]" : "";
            String name = pet.getDisplayName();
            String type = pet.getType().getId();
            source.sendFeedback(() -> Text.literal(num + ". " + name + " (" + type + ")" + active), false);
        }

        return 1;
    }

    private static int selectPet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        int number = IntegerArgumentType.getInteger(context, "number");

        PetSaveData saveData = PetSaveData.get(player.getServer());
        List<Pet> pets = saveData.getPetsByOwner(player.getUuid());

        if (pets.isEmpty()) {
            source.sendError(Text.literal("You don't have any pets."));
            return 0;
        }

        if (number < 1 || number > pets.size()) {
            source.sendError(Text.literal("Invalid pet number. You have " + pets.size() + " pet(s)."));
            return 0;
        }

        Pet selectedPet = pets.get(number - 1);

        // Dismiss current pet if active
        if (PetManager.hasActivePet(player)) {
            PetManager.dismissPet(player);
        }

        // Set the selected pet as active and summon it
        saveData.setActivePet(player.getUuid(), selectedPet.getUuid());

        if (PetManager.summonPet(player, selectedPet)) {
            final String petName = selectedPet.getDisplayName();
            source.sendFeedback(() -> Text.literal("Selected and summoned " + petName + "!"), false);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to summon selected pet."));
            return 0;
        }
    }

    private static int renamePet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        String newName = StringArgumentType.getString(context, "name");

        // Validate name length
        if (newName.length() > 32) {
            source.sendError(Text.literal("Pet name must be 32 characters or less."));
            return 0;
        }

        IPlayerPetData petData = (IPlayerPetData) player;
        UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

        PetSaveData saveData = PetSaveData.get(player.getServer());
        Pet activePet = activePetUuid != null ? saveData.getPet(activePetUuid) : null;

        // If no spawned pet, try to get the selected active pet from save data
        if (activePet == null) {
            activePet = saveData.getActivePet(player.getUuid());
        }

        // If still no pet, get first owned pet
        if (activePet == null) {
            List<Pet> pets = saveData.getPetsByOwner(player.getUuid());
            if (!pets.isEmpty()) {
                activePet = pets.get(0);
            }
        }

        if (activePet == null) {
            source.sendError(Text.literal("You don't have any pets to rename."));
            return 0;
        }

        activePet.setCustomName(newName);
        saveData.updatePet(activePet);

        // Update spawned entity name if present
        if (petData.westerosmobs$hasActivePet()) {
            var entity = PetManager.getSpawnedPetEntity(player);
            if (entity != null) {
                entity.setCustomName(Text.literal(newName));
                entity.setCustomNameVisible(true);
            }
        }

        source.sendFeedback(() -> Text.literal("Renamed your pet to: " + newName), false);
        return 1;
    }

    private static int petInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        IPlayerPetData petData = (IPlayerPetData) player;
        UUID activePetUuid = petData.westerosmobs$getActivePetUuid();

        PetSaveData saveData = PetSaveData.get(player.getServer());
        Pet pet = activePetUuid != null ? saveData.getPet(activePetUuid) : saveData.getActivePet(player.getUuid());

        if (pet == null) {
            List<Pet> pets = saveData.getPetsByOwner(player.getUuid());
            if (!pets.isEmpty()) {
                pet = pets.get(0);
            }
        }

        if (pet == null) {
            source.sendError(Text.literal("You don't have any pets."));
            return 0;
        }

        final Pet finalPet = pet;
        source.sendFeedback(() -> Text.literal("=== Pet Info ==="), false);
        source.sendFeedback(() -> Text.literal("Name: " + finalPet.getDisplayName()), false);
        source.sendFeedback(() -> Text.literal("Type: " + finalPet.getType().getId()), false);
        source.sendFeedback(() -> Text.literal("Level: " + finalPet.getLevel()), false);
        source.sendFeedback(() -> Text.literal("Status: " + (finalPet.isActive() ? "Active" : "Resting")), false);

        return 1;
    }

    private static int releasePet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!checkPlayerAndEnabled(source)) {
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        int number = IntegerArgumentType.getInteger(context, "number");

        PetSaveData saveData = PetSaveData.get(player.getServer());
        List<Pet> pets = saveData.getPetsByOwner(player.getUuid());

        if (pets.isEmpty()) {
            source.sendError(Text.literal("You don't have any pets."));
            return 0;
        }

        if (number < 1 || number > pets.size()) {
            source.sendError(Text.literal("Invalid pet number. You have " + pets.size() + " pet(s)."));
            return 0;
        }

        Pet petToRelease = pets.get(number - 1);
        final String petName = petToRelease.getDisplayName();

        PetManager.releasePet(player, petToRelease);
        source.sendFeedback(() -> Text.literal("You have released " + petName + ". Farewell!"), false);
        return 1;
    }

    private static int adminGivePet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Check admin permission
        if (source.getEntity() instanceof ServerPlayerEntity admin) {
            if (!LuckPermsIntegration.hasPermissionStrict(admin, "westerosmobs.pet.admin")) {
                source.sendError(Text.literal("You don't have permission to use admin commands."));
                return 0;
            }
        }

        String playerName = StringArgumentType.getString(context, "player");
        String typeId = StringArgumentType.getString(context, "type");

        PetType type = PetType.fromId(typeId);
        if (type == null) {
            source.sendError(Text.literal("Unknown pet type: " + typeId));
            return 0;
        }

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
        if (targetPlayer == null) {
            source.sendError(Text.literal("Player not found: " + playerName));
            return 0;
        }

        // Check one-per-type limit
        PetSaveData saveData = PetSaveData.get(source.getServer());
        List<Pet> existingPets = saveData.getPetsByOwner(targetPlayer.getUuid());
        for (Pet existing : existingPets) {
            if (existing.getType() == type) {
                source.sendError(Text.literal(playerName + " already has a " + type.getId() + "."));
                return 0;
            }
        }

        String name = getOptionalString(context, "name");
        Pet pet = PetManager.createPet(targetPlayer, type, name);
        source.sendFeedback(() -> Text.literal("Gave a " + type.getId() + " to " + playerName), false);
        targetPlayer.sendMessage(Text.literal("An admin has given you a new " + type.getId() + "!"), false);

        return 1;
    }

    private static int adminRemovePet(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Check admin permission
        if (source.getEntity() instanceof ServerPlayerEntity admin) {
            if (!LuckPermsIntegration.hasPermissionStrict(admin, "westerosmobs.pet.admin")) {
                source.sendError(Text.literal("You don't have permission to use admin commands."));
                return 0;
            }
        }

        String playerName = StringArgumentType.getString(context, "player");
        int number = IntegerArgumentType.getInteger(context, "number");

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
        if (targetPlayer == null) {
            source.sendError(Text.literal("Player not found: " + playerName));
            return 0;
        }

        PetSaveData saveData = PetSaveData.get(source.getServer());
        List<Pet> pets = saveData.getPetsByOwner(targetPlayer.getUuid());

        if (number < 1 || number > pets.size()) {
            source.sendError(Text.literal("Invalid pet number. Player has " + pets.size() + " pet(s)."));
            return 0;
        }

        Pet petToRemove = pets.get(number - 1);
        final String petName = petToRemove.getDisplayName();

        PetManager.releasePet(targetPlayer, petToRemove);
        source.sendFeedback(() -> Text.literal("Removed " + petName + " from " + playerName), false);
        targetPlayer.sendMessage(Text.literal("An admin has removed your " + petName + "."), false);

        return 1;
    }

    private static boolean checkPlayerAndEnabled(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendError(Text.literal("This command can only be used by players."));
            return false;
        }

        if (!WesterosMobsConfig.petEnabled) {
            source.sendError(Text.literal("The pet system is disabled."));
            return false;
        }

        return true;
    }

    private static boolean checkPermission(ServerPlayerEntity player, String permission, ServerCommandSource source) {
        if (!LuckPermsIntegration.hasPermission(player, permission)) {
            source.sendError(Text.literal("You don't have permission to use this command."));
            return false;
        }
        return true;
    }

    private static String getOptionalString(CommandContext<ServerCommandSource> context, String argName) {
        try {
            return StringArgumentType.getString(context, argName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
