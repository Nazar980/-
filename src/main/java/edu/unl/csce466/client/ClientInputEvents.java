package edu.unl.csce466.client;
import edu.unl.csce466.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Opens the native Minecraft mod menu screen when L is pressed.
 * 
 * We avoid calling Minecraft.getInstance() directly in the key event handler
 * because in Forge production (SRG mappings) it may resolve to a different
 * method name (m_91087_ vs getInstance), causing NoSuchMethodError.
 * 
 * Instead, we set a flag here and process it in ClientTickEvent where
 * Minecraft.getInstance() is guaranteed to work via the tick event context.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInputEvents.class);
    private static volatile boolean pendingToggle = false;
    private ClientInputEvents() {}
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_L && event.getAction() == GLFW.GLFW_PRESS) {
            pendingToggle = true;
            LOGGER.info("[Mod] L key pressed, scheduling menu toggle");
        }
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!pendingToggle) return;
        pendingToggle = false;
        try {
            // Use reflection-safe approach: net.minecraft.client.Minecraft
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) return;
            if (mc.screen instanceof ModMenuScreen) {
                mc.setScreen(null);
                LOGGER.info("[Mod] Menu closed");
            } else if (mc.screen == null) {
                mc.setScreen(new ModMenuScreen());
                LOGGER.info("[Mod] Menu opened");
            }
        } catch (Throwable t) {
            LOGGER.error("[Mod] Failed to toggle menu: {}", t.getMessage());
        }
    }
}
