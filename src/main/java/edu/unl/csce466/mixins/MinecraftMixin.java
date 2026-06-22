package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Empty placeholder mixin. No longer needed since SRG mappings
 * now match runtime, so Minecraft.getInstance() works directly.
 */
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftMixin {
}
