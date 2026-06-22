package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.imgui.ImGuiRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInputEvents.class);

    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_L || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        ImGuiRenderer renderer = ImGuiRenderer.getInstance();
        renderer.toggleMenu();
        LOGGER.info("[ImGui] Menu toggled with L: {}", renderer.isMenuVisible() ? "visible" : "hidden");
    }
}