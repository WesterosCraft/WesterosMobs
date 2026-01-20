package com.westeroscraft.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.westeroscraft.mount.IPlayerMountData;

import java.util.UUID;

/**
 * Mixin to persist mount data on players across server restarts.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerMountDataMixin implements IPlayerMountData {

    @Unique
    private boolean westerosmobs_hasMount = false;

    @Unique
    private UUID westerosmobs_mountUuid = null;

    @Override
    public boolean westerosmobs$hasMount() {
        return westerosmobs_hasMount;
    }

    @Override
    public void westerosmobs$setHasMount(boolean value) {
        this.westerosmobs_hasMount = value;
    }

    @Override
    public UUID westerosmobs$getMountUuid() {
        return westerosmobs_mountUuid;
    }

    @Override
    public void westerosmobs$setMountUuid(UUID uuid) {
        this.westerosmobs_mountUuid = uuid;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void westerosmobs_writeData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("westerosmobs.hasMount", westerosmobs_hasMount);
        if (westerosmobs_mountUuid != null) {
            nbt.putUuid("westerosmobs.mountUuid", westerosmobs_mountUuid);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void westerosmobs_readData(NbtCompound nbt, CallbackInfo ci) {
        westerosmobs_hasMount = nbt.getBoolean("westerosmobs.hasMount");
        if (nbt.containsUuid("westerosmobs.mountUuid")) {
            westerosmobs_mountUuid = nbt.getUuid("westerosmobs.mountUuid");
        } else {
            westerosmobs_mountUuid = null;
        }
    }
}
