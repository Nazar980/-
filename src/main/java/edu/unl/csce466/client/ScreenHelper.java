package edu.unl.csce466.client;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
                    LOGGER.info("[Mod] Found screen field: {}", f.getName());
                    break;
                }
            }
            if (SCREEN_FIELD == null) {
                for (Field f : mcClass.getDeclaredFields()) {
                    if (screenClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        SCREEN_FIELD = f;
                        LOGGER.info("[Mod] Found screen field (declared): {}", f.getName());
                        break;
                    }
                }
            }
            // Find setScreen: void method with exactly 1 param of type Screen
            // Filter: must NOT be named "disconnect" and must accept Screen (not a subclass)
            for (Method m : mcClass.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == screenClass
                    && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    SET_SCREEN_METHOD = m;
                    LOGGER.info("[Mod] Found setScreen method: {}", m.getName());
                    break;
                }
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
        try { return (Screen) SCREEN_FIELD.get(mc); } catch (Throwable t) { return null; }
    }
    public static void setScreen(Object mc, Screen screen) {
        init(mc);
        if (SET_SCREEN_METHOD == null) return;
        try { SET_SCREEN_METHOD.invoke(mc, screen); } catch (Throwable t) { LOGGER.error("[Mod] setScreen failed", t); }
    }
}
