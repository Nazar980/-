package edu.unl.csce466.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(at = @At("TAIL"), method = "initRenderer", remap = false, require = 0)
    private static void csce466$onInitRenderer(CallbackInfo callbackInfo) {
        try {
            RenderSystem.assertOnRenderThread();
            long windowHandle = GLFW.glfwGetCurrentContext();

            if (windowHandle == 0L) {
                LogUtils.getLogger().error("[ImGui] GLFW current context is 0, cannot init");
                return;
            }

            ImGuiRenderer.getInstance().init(windowHandle, () -> {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
            });
        } catch (Throwable throwable) {
            LogUtils.getLogger().error("[ImGui] Init failed", throwable);
        }
    }

    @Inject(
        method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
        at = @At("HEAD"),
        remap = false,
        require = 0
    )
    private static void csce466$onFlipFrame(long window, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo callbackInfo) {
        ImGuiRenderer.getInstance().render();
    }
}