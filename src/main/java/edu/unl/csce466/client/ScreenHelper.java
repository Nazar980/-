package edu.unl.csce466.client;

import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class ScreenHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenHelper.class);
    private static Method SET_SCREEN_METHOD;
    private static Field SCREEN_FIELD;
    private static boolean initialized = false;

    private ScreenHelper() {}

    private static void init(Object mc) {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> mcClass = mc.getClass();
            Class<?> screenClass = Screen.class;

            for (Field f : mcClass.getFields()) {
                if (f.getType() == screenClass) {
                    SCREEN_FIELD = f;
                    break;
                }
            }

            List<Method> candidates = new ArrayList<>();
            for (Method m : mcClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == screenClass
                    && m.getReturnType() == void.class) {
                    candidates.add(m);
                }
            }
            for (Method m : candidates) {
                if (Modifier.isPublic(m.getModifiers())) {
                    m.setAccessible(true);
                    SET_SCREEN_METHOD = m;
                    break;
                }
            }
            if (SET_SCREEN_METHOD == null && !candidates.isEmpty()) {
                Method m = candidates.get(candidates.size() - 1);
                m.setAccessible(true);
                SET_SCREEN_METHOD = m;
            }

            LOGGER.info("[Mod] screen={}, setScreen={}", 
                SCREEN_FIELD != null ? SCREEN_FIELD.getName() : "NULL",
                SET_SCREEN_METHOD != null ? SET_SCREEN_METHOD.getName() : "NULL");
        } catch (Throwable t) {
            LOGGER.error("[Mod] ScreenHelper init failed", t);
        }
    }

    public static Screen getScreen(Object mc) {
        init(mc);
        if (SCREEN_FIELD == null) return null;
        try { return (Screen) SCREEN_FIELD.get(mc); } catch (Throwable t) { return null; }
    }

    public static void setScreen(Object mc, Screen screen) {
        init(mc);
        if (SET_SCREEN_METHOD == null) return;
        try { SET_SCREEN_METHOD.invoke(mc, screen); } catch (Throwable t) { LOGGER.error("[Mod] setScreen failed", t); }
    }
}
