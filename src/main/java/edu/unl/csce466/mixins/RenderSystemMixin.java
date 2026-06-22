package edu.unl.csce466.mixins;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

/**
 * Mixin to hook ImGui into Minecraft's render loop.
 * 
 * KEY INSIGHT: We DO NOT access Minecraft.getInstance() or any Minecraft API
 * from mixin targets that run early in initialization (to avoid TLauncher
 * remapping issues). Instead we use raw GLFW to get the window handle.
 */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    private static boolean imGuiWindowInitialized = false;

    /**
     * Hook into the END of flipFrame — by this point the GLFW window is 100%
     * created and OpenGL context is current. We get the window via GLFW directly.
     */
    @Inject(
        method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
        at = @At("HEAD"),
        remap = false
    )
    private static void onFlipFrame(long windowPtr, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo cbi) {
        // windowPtr IS the GLFW window handle in MC 1.21.4 flipFrame!
        // Check the method signature: flipFrame(long window, TracyFrameCapture capture)
        // So 'windowPtr' here is actually the GLFW window!
        
        if (!imGuiWindowInitialized) {
            try {
                LogUtils.getLogger().info("[ImGui] flipFrame hook: window=0x{}", Long.toHexString(windowPtr));
                ImGuiRenderer.getInstance().initFromMixin(windowPtr);
                imGuiWindowInitialized = true;
            } catch (Exception e) {
                LogUtils.getLogger().error("[ImGui] Init from flipFrame failed: {}", e.getMessage());
            }
        }
        
        // Render ImGui every frame
        ImGuiRenderer.getInstance().render();
    }
}
