package edu.unl.csce466.client;

import com.mojang.logging.LogUtils;
import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.imgui.ImGuiRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_L && event.getAction() == GLFW.GLFW_PRESS) {
            ImGuiRenderer.getInstance().toggleMenu();
            LogUtils.getLogger().info("[ImGui] Menu toggled with L");
        }

        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            ImGuiRenderer.getInstance().hideMenu();
        }
    }
}