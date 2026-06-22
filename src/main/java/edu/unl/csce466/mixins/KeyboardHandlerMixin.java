package edu.unl.csce466.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import edu.unl.csce466.imgui.ImGuiRenderer;
import org.lwjgl.glfw.GLFW;

/**
 * Миксин для обработки нажатий клавиш.
 * Используем миксин вместо Forge событий, чтобы избежать
 * NoSuchMethodError с SRG маппингами на TLauncher.
 * 
 * НИКАКИХ вызовов Minecraft API — только GLFW и наш ImGuiRenderer!
 */
@Mixin(net.minecraft.client.KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // Обрабатываем только нажатие (не отпускание и не повтор)
        if (action != GLFW.GLFW_PRESS) return;
        
        ImGuiRenderer renderer = ImGuiRenderer.getInstance();
        
        // L — открыть ImGui меню
        if (key == GLFW.GLFW_KEY_L && !renderer.isMenuVisible()) {
            renderer.showMenu();
        }
        
        // ESC — закрыть ImGui меню (и отменить паузу Minecraft)
        if (key == GLFW.GLFW_KEY_ESCAPE && renderer.isMenuVisible()) {
            renderer.hideMenu();
            ci.cancel(); // Предотвращаем открытие меню паузы Minecraft
        }
    }
}
