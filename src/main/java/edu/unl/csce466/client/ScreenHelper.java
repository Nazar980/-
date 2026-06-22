package edu.unl.csce466.client;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
            // Find screen field (public, type == Screen)
            for (Field f : mcClass.getFields()) {
                if (f.getType() == screenClass) {
                    SCREEN_FIELD = f;
                    LOGGER.info("[Mod] Found screen field: {} (type: {})", f.getName(), f.getType().getSimpleName());
                    break;
                }
            }
            // Find ALL void methods with exactly 1 Screen parameter
            List<Method> candidates = new ArrayList<>();
            for (Method m : mcClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == screenClass
                    && m.getReturnType() == void.class) {
                    candidates.add(m);
                    LOGGER.info("[Mod] setScreen candidate: {} ({})", m.getName(),
                        java.lang.reflect.Modifier.toString(m.getModifiers()));
                }
            }
            // Pick the right one:
            // - In SRG names, setScreen is typically the PUBLIC one
            // - disconnect is typically private or has different modifiers
            for (Method m : candidates) {
                if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                    m.setAccessible(true);
                    SET_SCREEN_METHOD = m;
                    LOGGER.info("[Mod] Selected setScreen: {} (public)", m.getName());
                    break;
                }
            }
            // Fallback: just take first that isn't named "disconnect"
            if (SET_SCREEN_METHOD == null) {
                for (Method m : candidates) {
                    if (!m.getName().contains("disconnect") && !m.getName().contains("Disconnect")) {
                        m.setAccessible(true);
                        SET_SCREEN_METHOD = m;
                        LOGGER.info("[Mod] Selected setScreen (fallback): {}", m.getName());
                        break;
                    }
                }
            }
            // Last fallback: just take first
            if (SET_SCREEN_METHOD == null && !candidates.isEmpty()) {
                Method m = candidates.get(0);
                m.setAccessible(true);
                SET_SCREEN_METHOD = m;
                LOGGER.info("[Mod] Selected setScreen (last resort): {}", m.getName());
            }
            if (SCREEN_FIELD == null) LOGGER.error("[Mod] screen field NOT FOUND");
            if (SET_SCREEN_METHOD == null) LOGGER.error("[Mod] setScreen method NOT FOUND");
        } catch (Throwable t) {
            LOGGER.error("[Mod] ScreenHelper init failed", t);
        }
    }
    public static Screen getScreen(Object mc) {
        init(mc);
        if (SCREEN_FIELD == null) return null;
        try { return (Screen) SCREEN_FIELD.get(mc); }
        catch (Throwable t) { return null; }
    }
    public static void setScreen(Object mc, Screen screen) {
        init(mc);
        if (SET_SCREEN_METHOD == null) return;
        try { SET_SCREEN_METHOD.invoke(mc, screen); }
        catch (Throwable t) { LOGGER.error("[Mod] setScreen invoke failed", t); }
    }
}
