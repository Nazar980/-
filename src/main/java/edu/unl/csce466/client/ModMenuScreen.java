package edu.unl.csce466.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuScreen extends Screen {

    public ModMenuScreen() {
        super(Component.literal("Mod Menu"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.addRenderableWidget(
            Button.builder(Component.literal("Close"), btn -> this.onClose())
                .bounds(cx - 50, cy + 40, 100, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int pw = 280, ph = 120;
        int px = cx - pw / 2, py = cy - ph / 2;

        g.fill(px, py, px + pw, py + ph, 0xCC222222);
        g.fill(px, py, px + pw, py + 1, 0xFF55FF55);
        g.fill(px, py + ph - 1, px + pw, py + ph, 0xFF55FF55);
        g.fill(px, py, px + 1, py + ph, 0xFF55FF55);
        g.fill(px + pw - 1, py, px + pw, py + ph, 0xFF55FF55);

        g.drawCenteredString(this.font, "Mod Menu", cx, cy - 35, 0x55FF55);
        g.drawCenteredString(this.font, "Minecraft 1.21.4 + Forge", cx, cy - 15, 0xAAFFAA);
        g.drawCenteredString(this.font, "Press L or ESC to close", cx, cy + 5, 0x999999);

        super.render(g, mouseX, mouseY, delta);
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
