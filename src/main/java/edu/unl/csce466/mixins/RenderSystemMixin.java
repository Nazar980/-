package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.client.Minecraft;

/**
 * Mixin to hook ImGui into Minecraft's render loop.
 * 
 * KEY: We store Minecraft instance from the constructor mixin,
 * then initialize ImGui from flipFrame where the window exists.
 * NO direct Minecraft.getInstance() calls from mixin targets!
 */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    private static boolean imGuiWindowInitialized = false;

    /**
     * flipFrame receives the GLFW window as first parameter.
     * By this point Minecraft window is fully created and GL context is current.
     */
    @Inject(
        method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
        at = @At("HEAD"),
        remap = false
    )
    private static void onFlipFrame(long windowPtr, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo cbi) {
        // Init ImGui on first frame
        if (!imGuiWindowInitialized) {
            try {
                LogUtils.getLogger().info("[ImGui] flipFrame hook: window=0x{}", Long.toHexString(windowPtr));
                ImGuiRenderer.getInstance().initFromMixin(windowPtr);
                imGuiWindowInitialized = true;
            } catch (Exception e) {
                LogUtils.getLogger().error("[ImGui] Init from flipFrame failed: {}", e.toString());
            }
        }
        
        // Render ImGui every frame
        ImGuiRenderer.getInstance().render();
    }
}
