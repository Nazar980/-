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

    @Inject(at = @At(value = "TAIL"), method = "initRenderer", remap = false)
    private static void onInitRenderer(CallbackInfo cbi) {
        RenderSystem.assertOnRenderThread();
        
        try {
            // FIX: Получаем реальный GLFW window handle от Minecraft,
            // а не glfwGetCurrentContext() (который возвращает GL контекст!)
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                LogUtils.getLogger().warn("[ImGui] Cannot init: Minecraft/Window null during initRenderer");
                return;
            }
            
            long windowHandle = mc.getWindow().getWindow();
            LogUtils.getLogger().info("[ImGui] Window handle: 0x" + Long.toHexString(windowHandle));
            
            ImGuiRenderer.getInstance().init(windowHandle, () -> {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
            });
            
        } catch (Exception e) {
            LogUtils.getLogger().error("[ImGui] Init in RenderSystem failed, will try lazy init: " + e.getMessage(), e);
        }
    }

    @Inject(
        method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
        at = @At("HEAD"),
        remap = false
    )
    private static void onFlipFrame(long window, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo cbi) {
        ImGuiRenderer.getInstance().render();
    }
}
