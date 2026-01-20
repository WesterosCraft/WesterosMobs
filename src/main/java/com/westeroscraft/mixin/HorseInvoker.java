package com.westeroscraft.mixin;

import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin invoker to access the private setVariant method on Horse.
 */
@Mixin(HorseEntity.class)
public interface HorseInvoker {

    @Invoker("setHorseVariant")
    void invokeSetVariant(HorseColor color, HorseMarking marking);
}
