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
import org.lwjgl.glfw.GLFW;
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(at = @At(value = "TAIL"), method = "initRenderer", remap = false)
    private static void onInitRenderer(CallbackInfo cbi) {
        RenderSystem.assertOnRenderThread();
        try {
            long windowHandle = GLFW.glfwGetCurrentContext();
            if (windowHandle == 0) {
                LogUtils.getLogger().error("[ImGui] GLFW window handle is 0, cannot init");
                return;
            }
            LogUtils.getLogger().info("[ImGui] Initializing with GLFW window: 0x" + Long.toHexString(windowHandle));
            ImGuiRenderer.getInstance().init(windowHandle, () -> {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
            });
        } catch (Exception e) {
            LogUtils.getLogger().error("[ImGui] Init failed: " + e.getMessage(), e);
        }
    }
    @Inject(
        method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
        at = @At("HEAD"),
        remap = false
    )
    private static void onFlipFrame(long window, com.mojang.blaze3d.TracyFrameCapture tracyCapture, CallbackInfo cbi) {
        try {
            ImGuiRenderer.getInstance().render();
        } catch (Exception e) {
            // Silently skip — don't crash the game over an ImGui render failure
        }
    }
}
