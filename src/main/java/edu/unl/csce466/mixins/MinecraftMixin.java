package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Compatibility no-op mixin.
 *
 * Older versions of this project used this class to cache the client instance.
 * That code was removed because direct client API calls were crashing in the
 * launcher runtime mappings. Keep this class empty so stale project files/configs
 * cannot break compilation.
 */
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftMixin {
}
