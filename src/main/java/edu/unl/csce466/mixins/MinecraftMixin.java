package edu.unl.csce466.mixins;

import edu.unl.csce466.ExampleMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Minecraft constructor to store the instance safely.
 * This runs AFTER Minecraft is fully constructed, so all fields are set.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onMinecraftConstructed(CallbackInfo cbi) {
        ExampleMod.MINECRAFT = (Minecraft) (Object) this;
    }
}
