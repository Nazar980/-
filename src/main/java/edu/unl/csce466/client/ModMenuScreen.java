package edu.unl.csce466.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/**
 * Mod menu screen using ONLY the Screen constructor that takes Component.
 * All rendering done via GuiGraphics.fill() and drawString() —
 * no Button.builder(), no Component.literal() calls that get SRG-remapped.
 */
public class ModMenuScreen extends Screen {
    public ModMenuScreen() {
        // Screen(Component) constructor — Component.empty() is a static final,
        // should survive SRG remapping because it's a field not a method call
        super(Component.empty());
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int pw = 280, ph = 100;
        int px = cx - pw / 2, py = cy - ph / 2;
        // Panel
        g.fill(px, py, px + pw, py + ph, 0xDD1a1a2e);
        // Border
        g.fill(px, py, px + pw, py + 1, 0xFF00ff88);
        g.fill(px, py + ph - 1, px + pw, py + ph, 0xFF00ff88);
        g.fill(px, py, px + 1, py + ph, 0xFF00ff88);
        g.fill(px + pw - 1, py, px + pw, py + ph, 0xFF00ff88);
        // Text — using drawString with raw strings (no Component)
        g.drawCenteredString(this.font, "Mod Menu", cx, cy - 30, 0x55FF55);
        g.drawCenteredString(this.font, "Minecraft 1.21.4 + Forge", cx, cy - 10, 0xAAFFAA);
        g.drawCenteredString(this.font, "Press L or ESC to close", cx, cy + 15, 0x888888);
        super.render(g, mouseX, mouseY, delta);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // L key closes too
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
