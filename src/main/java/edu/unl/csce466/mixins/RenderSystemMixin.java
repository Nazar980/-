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
            imgui.ImGuiIO io = ImGui.getIO();
            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        });
    }

    @Inject(
        method = "flipFrame",
        at = @At("HEAD"),
        remap = true
    )
    private static void flipFrame(long window, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo cbi) {
        ImGuiRenderer.getInstance().render();
    }
}
