package edu.unl.csce466.client;

import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Accesses Minecraft's screen/setScreen via MethodHandles.
 * Found by type signature, not by name — immune to SRG remapping.
 */
public final class ScreenHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenHelper.class);

    private static MethodHandle SET_SCREEN;
    private static Field SCREEN_FIELD;
    private static boolean initialized = false;

    private ScreenHelper() {}

    private static void init(Object mc) {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> mcClass = mc.getClass();
            // Walk up to find the actual Minecraft class (not a subclass from mixin)
            while (mcClass != null && !mcClass.getName().contains("Minecraft")) {
                mcClass = mcClass.getSuperclass();
            }
            if (mcClass == null) mcClass = mc.getClass();

            Class<?> screenClass = Screen.class;

            // Find 'screen' field (type = Screen, public)
            for (Field f : mcClass.getDeclaredFields()) {
                if (screenClass.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    SCREEN_FIELD = f;
                    LOGGER.info("[Mod] Found screen field: {}", f.getName());
                    break;
                }
            }

            // Find setScreen method: void xxx(Screen)
            for (java.lang.reflect.Method m : mcClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 
                    && screenClass.isAssignableFrom(m.getParameterTypes()[0])
                    && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    SET_SCREEN = MethodHandles.lookup().unreflect(m);
                    LOGGER.info("[Mod] Found setScreen method: {}", m.getName());
                    break;
                }
            }

            if (SCREEN_FIELD == null) LOGGER.error("[Mod] screen field NOT FOUND");
            if (SET_SCREEN == null) LOGGER.error("[Mod] setScreen method NOT FOUND");

        } catch (Throwable t) {
            LOGGER.error("[Mod] ScreenHelper init failed", t);
        }
    }

    public static Screen getScreen(Object mc) {
        init(mc);
        if (SCREEN_FIELD == null) return null;
        try {
            return (Screen) SCREEN_FIELD.get(mc);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setScreen(Object mc, Screen screen) {
        init(mc);
        if (SET_SCREEN == null) return;
        try {
            SET_SCREEN.invoke(mc, screen);
        } catch (Throwable t) {
            LOGGER.error("[Mod] setScreen failed", t);
        }
    }
}
