package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    
    @Inject(at = @At(value = "TAIL"), method = "initRenderer", remap = true)
    private static void initRenderer(CallbackInfo cbi) {
        RenderSystem.assertOnRenderThread();
        LogUtils.getLogger().info("[ImGui] Initializing ImGui for Forge 1.21.4");
        ImGuiRenderer.getInstance().init(() -> {
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        });
    }
    
    // CORRECT for Minecraft 1.21.4
    // The actual method is flipFrame(long, TracyFrameCapturer)
    // Use full internal name in the descriptor + Object as parameter type
    // (this prevents "package net.minecraft.client.util.tracy does not exist")
    @Inject(at = @At(value = "HEAD"), 
            method = "flipFrame(JLnet/minecraft/client/util/tracy/TracyFrameCapturer;)V", 
            remap = true)
    private static void flipFrame(long window, Object tracyCapturer, CallbackInfo cbi) {
        RenderSystem.recordRenderCall(() -> {
            ImGuiRenderer.getInstance().render();
        });
    }
}
