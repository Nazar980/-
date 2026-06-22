package edu.unl.csce466.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ModMenuScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMenuScreen.class);

    public ModMenuScreen() {
        super(findComponent());
    }

    private static Component findComponent() {
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 0
                    && Component.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return (Component) m.invoke(null);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Don't call renderBackground — it causes blur in 1.21.4
        // Instead draw our own dark overlay + panel using fill()
        // fill() is a GuiGraphics method that takes (x1,y1,x2,y2,color)
        try {
            // Dark semi-transparent overlay over entire screen
            callFill(g, 0, 0, this.width, this.height, 0x88000000);

            int cx = this.width / 2, cy = this.height / 2;
            int pw = 280, ph = 100, px = cx - pw / 2, py = cy - ph / 2;

            // Panel background
            callFill(g, px, py, px + pw, py + ph, 0xEE1a1a2e);
            // Green border
            callFill(g, px, py, px + pw, py + 1, 0xFF00ff88);
            callFill(g, px, py + ph - 1, px + pw, py + ph, 0xFF00ff88);
            callFill(g, px, py, px + 1, py + ph, 0xFF00ff88);
            callFill(g, px + pw - 1, py, px + pw, py + ph, 0xFF00ff88);

            // Text - use drawString via reflection to handle SRG
            callDrawCenteredString(g, "Mod Menu", cx, cy - 30, 0x55FF55);
            callDrawCenteredString(g, "Minecraft 1.21.4 + Forge", cx, cy - 10, 0xAAFFAA);
            callDrawCenteredString(g, "Press L or ESC to close", cx, cy + 15, 0x888888);
        } catch (Throwable t) {
            LOGGER.error("[Mod] render failed", t);
        }

        // Don't call super.render — it may also be remapped
    }

    private void callFill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        try {
            // Try direct call first
            g.fill(x1, y1, x2, y2, color);
        } catch (Throwable t1) {
            // Fallback: find fill method by reflection
            try {
                for (Method m : g.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 5) {
                        Class<?>[] p = m.getParameterTypes();
                        if (p[0] == int.class && p[1] == int.class && p[2] == int.class
                            && p[3] == int.class && p[4] == int.class) {
                            m.setAccessible(true);
                            m.invoke(g, x1, y1, x2, y2, color);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private void callDrawCenteredString(GuiGraphics g, String text, int x, int y, int color) {
        try {
            g.drawCenteredString(this.font, text, x, y, color);
        } catch (Throwable t1) {
            try {
                // Fallback: find any method that takes (Font, String, int, int, int)
                for (Method m : g.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 5) {
                        Class<?>[] p = m.getParameterTypes();
                        if (p[1] == String.class && p[2] == int.class
                            && p[3] == int.class && p[4] == int.class) {
                            m.setAccessible(true);
                            m.invoke(g, this.font, text, x, y, color);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_L || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            ScreenHelper.setScreen(edu.unl.csce466.ExampleMod.mcInstance, null);
            return true;
        }
        return false; // Don't call super — it's remapped too
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
