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
    private UUID westerosmobs_mountUuid = null;

    @Unique
    private String westerosmobs_mountName = null;

    @Unique
    private int westerosmobs_mountColor = -1;

    @Unique
    private int westerosmobs_mountMarking = -1;

    @Override
    public UUID westerosmobs$getMountUuid() {
        return westerosmobs_mountUuid;
    }

    @Override
    public void westerosmobs$setMountUuid(UUID uuid) {
        this.westerosmobs_mountUuid = uuid;
    }

    @Override
    public String westerosmobs$getMountName() {
        return westerosmobs_mountName;
    }

    @Override
    public void westerosmobs$setMountName(String name) {
        this.westerosmobs_mountName = name;
    }

    @Override
    public int westerosmobs$getMountColor() {
        return westerosmobs_mountColor;
    }

    @Override
    public void westerosmobs$setMountColor(int color) {
        this.westerosmobs_mountColor = color;
    }

    @Override
    public int westerosmobs$getMountMarking() {
        return westerosmobs_mountMarking;
    }

    @Override
    public void westerosmobs$setMountMarking(int marking) {
        this.westerosmobs_mountMarking = marking;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void westerosmobs_writeData(NbtCompound nbt, CallbackInfo ci) {
        if (westerosmobs_mountUuid != null) {
            nbt.putUuid("westerosmobs.mountUuid", westerosmobs_mountUuid);
        }
        if (westerosmobs_mountName != null) {
            nbt.putString("westerosmobs.mountName", westerosmobs_mountName);
        }
        nbt.putInt("westerosmobs.mountColor", westerosmobs_mountColor);
        nbt.putInt("westerosmobs.mountMarking", westerosmobs_mountMarking);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void westerosmobs_readData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.containsUuid("westerosmobs.mountUuid")) {
            westerosmobs_mountUuid = nbt.getUuid("westerosmobs.mountUuid");
        } else {
            westerosmobs_mountUuid = null;
        }
        if (nbt.contains("westerosmobs.mountName")) {
            westerosmobs_mountName = nbt.getString("westerosmobs.mountName");
        } else {
            westerosmobs_mountName = null;
        }
        westerosmobs_mountColor = nbt.contains("westerosmobs.mountColor") ? nbt.getInt("westerosmobs.mountColor") : -1;
        westerosmobs_mountMarking = nbt.contains("westerosmobs.mountMarking") ? nbt.getInt("westerosmobs.mountMarking") : -1;
    }
}
