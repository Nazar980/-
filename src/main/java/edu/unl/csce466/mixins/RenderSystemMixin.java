package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import org.lwjgl.glfw.GLFW;

/**
 * RenderSystem mixin — kept minimal.
 *
 * ImGui GL3 rendering was removed because imgui-java 1.87's OpenGL pipeline
 * is fundamentally incompatible with Minecraft 1.21.4's new GpuDevice and
 * crashes NVIDIA drivers (nvoglv64.dll EXCEPTION_ACCESS_VIOLATION).
 *
 * The mod now uses native Minecraft Screen rendering (ModMenuScreen) instead.
 * This mixin is kept as a no-op placeholder for future use.
 */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    // Intentionally empty — no ImGui GL rendering hooks.
    // The mod menu is now a native Minecraft Screen (ModMenuScreen).
}
