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
    
    // FIXED for 1.21.4 - use the base flipFrame(J)V method only.
    // This signature is always present and avoids any TracyFrameCapturer issues.
    @Inject(at = @At(value = "HEAD"), method = "flipFrame(J)V", remap = true)
    private static void flipFrame(long window, CallbackInfo cbi) {
        RenderSystem.recordRenderCall(() -> {
            ImGuiRenderer.getInstance().render();
        });
    }
}
