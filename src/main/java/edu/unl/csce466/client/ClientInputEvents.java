package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.mixins.IMinecraftAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Key handler: L toggles mod menu.
 * 
 * We NEVER call Minecraft.getInstance(), mc.screen, or mc.setScreen() here.
 * All those methods have SRG name issues in production (m_91087_ etc).
 * 
 * Instead, MinecraftMixin captures the instance at init and implements
 * IMinecraftAccessor.examplemod$toggleScreen() where all Minecraft
 * field/method references are correctly remapped by the mixin processor.
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
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingToggle) return;
        pendingToggle = false;

        try {
            Object mcObj = ExampleMod.mcInstance;
            if (mcObj == null) {
                LOGGER.warn("[Mod] Minecraft instance not yet captured by mixin");
                return;
            }

            // Cast to our interface — this calls into the mixin where
            // screen/setScreen are properly remapped
            ((IMinecraftAccessor) mcObj).examplemod$toggleScreen();
            LOGGER.info("[Mod] Menu toggled via mixin");
        } catch (Throwable t) {
            LOGGER.error("[Mod] Failed to toggle menu", t);
        }
    }
}
