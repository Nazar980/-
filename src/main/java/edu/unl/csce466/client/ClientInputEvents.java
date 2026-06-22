package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInputEvents.class);
    private static volatile boolean pendingToggle = false;

    private ClientInputEvents() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_L && event.getAction() == GLFW.GLFW_PRESS) {
            pendingToggle = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingToggle) return;
        pendingToggle = false;

        try {
            Object mc = ExampleMod.mcInstance;
            if (mc == null) return;

            Screen cur = ScreenHelper.getScreen(mc);
            if (cur instanceof ModMenuScreen) {
                ScreenHelper.setScreen(mc, null);
                LOGGER.info("[Mod] Menu closed");
            } else if (cur == null) {
                ScreenHelper.setScreen(mc, new ModMenuScreen());
                LOGGER.info("[Mod] Menu opened");
            }
        } catch (Throwable t) {
            LOGGER.error("[Mod] Toggle failed", t);
        }
    }
}
