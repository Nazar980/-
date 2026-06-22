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

            // Find screen field
            for (Field f : mcClass.getFields()) {
                if (f.getType() == screenClass) {
                    SCREEN_FIELD = f;
                    break;
                }
            }

            // Find ALL void(Screen) methods and log them
            List<Method> candidates = new ArrayList<>();
            for (Method m : mcClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == screenClass
                    && m.getReturnType() == void.class) {
                    candidates.add(m);
                    LOGGER.info("[Mod] Candidate: {} modifiers={} public={}",
                        m.getName(), m.getModifiers(), Modifier.isPublic(m.getModifiers()));
                }
            }

            // Strategy: setScreen is public, disconnect is also public but takes
            // Screen as a parameter for a different purpose.
            // In 1.21.4 SRG, setScreen and disconnect both accept Screen.
            // Key difference: disconnect internally calls runTick/polls events,
            // setScreen just assigns the field. We need the SHORTER one.
            // Best heuristic: pick the one that is NOT the last candidate
            // (disconnect is usually defined after setScreen in the class)
            
            // First: try to find one that's NOT named disconnect (works if not obfuscated)
            for (Method m : candidates) {
                String name = m.getName();
                if (!name.equals("disconnect") && !name.contains("isconnect")) {
                    m.setAccessible(true);
                    SET_SCREEN_METHOD = m;
                    LOGGER.info("[Mod] Selected (not-disconnect): {}", name);
                    break;
                }
            }

            // If all are obfuscated (m_XXXXX_), pick the FIRST public one
            // In bytecode order, setScreen comes before disconnect
            if (SET_SCREEN_METHOD == null) {
                for (Method m : candidates) {
                    if (Modifier.isPublic(m.getModifiers())) {
                        m.setAccessible(true);
                        SET_SCREEN_METHOD = m;
                        LOGGER.info("[Mod] Selected (first public): {}", m.getName());
                        break;
                    }
                }
            }

            LOGGER.info("[Mod] Final: screen={}, setScreen={}",
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
        if (SET_SCREEN_METHOD == null) {
            // Ultimate fallback: directly set the field
            if (SCREEN_FIELD != null) {
                try {
                    SCREEN_FIELD.set(mc, screen);
                    LOGGER.info("[Mod] Used field fallback");
                } catch (Throwable t) {
                    LOGGER.error("[Mod] Field fallback failed", t);
                }
            }
            return;
        }
        try { SET_SCREEN_METHOD.invoke(mc, screen); } catch (Throwable t) { LOGGER.error("[Mod] setScreen failed", t); }
    }
}
