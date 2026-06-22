package edu.unl.csce466.mixins;

import edu.unl.csce466.ExampleMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the Minecraft instance at construction time via mixin.
 * This avoids the SRG mapping issue with Minecraft.getInstance() (m_91087_)
 * which doesn't exist in 1.21.4 production runtime.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        ExampleMod.mcInstance = this;
    }
}
