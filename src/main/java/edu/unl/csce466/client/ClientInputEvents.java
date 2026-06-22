package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
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
            // Use GLFW directly to get the window handle — no Minecraft class methods needed
            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) return;

            // Get Minecraft instance via reflection to avoid SRG mapping issues
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            
            // Try multiple possible method names for getInstance()
            Object mc = null;
            for (String name : new String[]{"getInstance", "m_91087_", "m_91105_"}) {
                try {
                    java.lang.reflect.Method method = mcClass.getDeclaredMethod(name);
                    method.setAccessible(true);
                    mc = method.invoke(null);
                    if (mc != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            
            if (mc == null) {
                // Last resort: try the field "instance" or similar
                for (java.lang.reflect.Field f : mcClass.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && mcClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        mc = f.get(null);
                        if (mc != null) break;
                    }
                }
            }
            
            if (mc == null) {
                LOGGER.error("[Mod] Cannot find Minecraft instance");
                return;
            }

            // Get current screen
            java.lang.reflect.Field screenField = null;
            for (java.lang.reflect.Field f : mcClass.getDeclaredFields()) {
                if (f.getType().getName().equals("net.minecraft.client.gui.screens.Screen")) {
                    screenField = f;
                    break;
                }
            }
            if (screenField == null) {
                // Try superclass
                for (java.lang.reflect.Field f : mcClass.getFields()) {
                    if (f.getType().getName().contains("Screen")) {
                        screenField = f;
                        break;
                    }
                }
            }

            Object currentScreen = null;
            if (screenField != null) {
                screenField.setAccessible(true);
                currentScreen = screenField.get(mc);
            }

            // Find setScreen method
            java.lang.reflect.Method setScreenMethod = null;
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            for (java.lang.reflect.Method m : mcClass.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && screenClass.isAssignableFrom(params[0]) && m.getReturnType() == void.class) {
                    setScreenMethod = m;
                    break;
                }
            }
            
            if (setScreenMethod == null) {
                LOGGER.error("[Mod] Cannot find setScreen method");
                return;
            }
            setScreenMethod.setAccessible(true);

            if (currentScreen instanceof ModMenuScreen) {
                setScreenMethod.invoke(mc, (Object) null);
                LOGGER.info("[Mod] Menu closed");
            } else if (currentScreen == null) {
                setScreenMethod.invoke(mc, new ModMenuScreen());
                LOGGER.info("[Mod] Menu opened");
            }
        } catch (Throwable t) {
            LOGGER.error("[Mod] Failed to toggle menu", t);
        }
    }
}
