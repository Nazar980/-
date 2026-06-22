package xyz.breadloaf.imguimc.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.breadloaf.imguimc.Imguimc;
import xyz.breadloaf.imguimc.imgui.ImguiLoader;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    public void setup(long window, CallbackInfo ci) {
        ImguiLoader.onGlfwInit(window);
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void keyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (key == GLFW_KEY_RIGHT_SHIFT && action == GLFW_PRESS && Imguimc.toggleClientMenu()) {
            ci.cancel();
            return;
        }

        if (Imguimc.shouldCancelGameKeyboardInputs()) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    public void charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (Imguimc.shouldCancelGameKeyboardInputs()) {
            ci.cancel();
        }
    }
}
