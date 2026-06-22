package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the native Minecraft mod menu screen when L is pressed.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInputEvents.class);

    private ClientInputEvents() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_L || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // If menu is already open, close it
        if (mc.screen instanceof ModMenuScreen) {
            mc.setScreen(null);
            LOGGER.info("[Mod] Menu closed with L");
            return;
        }

        // If no screen is open (player is in game), open our menu
        if (mc.screen == null) {
            mc.setScreen(new ModMenuScreen());
            LOGGER.info("[Mod] Menu opened with L");
        }
    }
}
