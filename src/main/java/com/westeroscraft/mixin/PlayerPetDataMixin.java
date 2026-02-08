package com.westeroscraft.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.westeroscraft.pet.IPlayerPetData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mixin to persist pet ownership data on players across server restarts.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerPetDataMixin implements IPlayerPetData {

    @Unique
    private List<UUID> westerosmobs_ownedPetUuids = new ArrayList<>();

    @Unique
    private UUID westerosmobs_activePetUuid = null;

    @Override
    public List<UUID> westerosmobs$getOwnedPetUuids() {
        return westerosmobs_ownedPetUuids;
    }

    @Override
    public void westerosmobs$setOwnedPetUuids(List<UUID> uuids) {
        this.westerosmobs_ownedPetUuids = uuids != null ? new ArrayList<>(uuids) : new ArrayList<>();
    }

    @Override
    public void westerosmobs$addOwnedPet(UUID petUuid) {
        if (!westerosmobs_ownedPetUuids.contains(petUuid)) {
            westerosmobs_ownedPetUuids.add(petUuid);
        }
    }

    @Override
    public void westerosmobs$removeOwnedPet(UUID petUuid) {
        westerosmobs_ownedPetUuids.remove(petUuid);
    }

    @Override
    public UUID westerosmobs$getActivePetUuid() {
        return westerosmobs_activePetUuid;
    }

    @Override
    public void westerosmobs$setActivePetUuid(UUID uuid) {
        this.westerosmobs_activePetUuid = uuid;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void westerosmobs_writePetData(NbtCompound nbt, CallbackInfo ci) {
        // Write owned pet UUIDs as a list
        NbtList petList = new NbtList();
        for (UUID uuid : westerosmobs_ownedPetUuids) {
            NbtCompound petEntry = new NbtCompound();
            petEntry.putUuid("uuid", uuid);
            petList.add(petEntry);
        }
        nbt.put("westerosmobs.ownedPets", petList);

        // Write active pet UUID
        if (westerosmobs_activePetUuid != null) {
            nbt.putUuid("westerosmobs.activePetUuid", westerosmobs_activePetUuid);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void westerosmobs_readPetData(NbtCompound nbt, CallbackInfo ci) {
        // Read owned pet UUIDs
        westerosmobs_ownedPetUuids = new ArrayList<>();
        if (nbt.contains("westerosmobs.ownedPets", NbtElement.LIST_TYPE)) {
            NbtList petList = nbt.getList("westerosmobs.ownedPets", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < petList.size(); i++) {
                NbtCompound petEntry = petList.getCompound(i);
                if (petEntry.containsUuid("uuid")) {
                    westerosmobs_ownedPetUuids.add(petEntry.getUuid("uuid"));
                }
            }
        }

        // Read active pet UUID
        if (nbt.containsUuid("westerosmobs.activePetUuid")) {
            westerosmobs_activePetUuid = nbt.getUuid("westerosmobs.activePetUuid");
        } else {
            westerosmobs_activePetUuid = null;
        }
    }
}
