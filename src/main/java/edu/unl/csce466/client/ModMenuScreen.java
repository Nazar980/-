package edu.unl.csce466.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ModMenuScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMenuScreen.class);
    public ModMenuScreen() {
        super(makeTitle());
    }
    private static Component makeTitle() {
        try {
            // Find Component.empty() or Component.literal() by reflection
            for (java.lang.reflect.Method m : Component.class.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 0
                    && Component.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return (Component) m.invoke(null);
                }
            }
            // Fallback: find literal(String)
            for (java.lang.reflect.Method m : Component.class.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == String.class
                    && Component.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return (Component) m.invoke(null, "Mod Menu");
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[Mod] makeTitle failed", t);
        }
        // Last resort — return a direct implementation
        return new net.minecraft.network.chat.CommonComponents() {
            // This won't work but prevents NPE
        } instanceof Component c ? c : null;
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        try {
            this.renderBackground(g, mouseX, mouseY, delta);
        } catch (Throwable ignored) {}
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pw = 280, ph = 100;
        int px = cx - pw / 2, py = cy - ph / 2;
        g.fill(px, py, px + pw, py + ph, 0xDD1a1a2e);
        g.fill(px, py, px + pw, py + 1, 0xFF00ff88);
        g.fill(px, py + ph - 1, px + pw, py + ph, 0xFF00ff88);
        g.fill(px, py, px + 1, py + ph, 0xFF00ff88);
        g.fill(px + pw - 1, py, px + pw, py + ph, 0xFF00ff88);
        g.drawCenteredString(this.font, "Mod Menu", cx, cy - 30, 0x55FF55);
        g.drawCenteredString(this.font, "Minecraft 1.21.4 + Forge", cx, cy - 10, 0xAAFFAA);
        g.drawCenteredString(this.font, "Press L or ESC to close", cx, cy + 15, 0x888888);
        try {
            super.render(g, mouseX, mouseY, delta);
        } catch (Throwable ignored) {}
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_L) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
