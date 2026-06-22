package edu.unl.csce466.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Native Minecraft GUI screen that replaces the broken ImGui GL3 overlay.
 *
 * ImGui-Java 1.87's OpenGL 3.2 renderer is incompatible with Minecraft 1.21.4's
 * new GpuDevice abstraction — calling ImGui's renderDrawData crashes NVIDIA's
 * driver (nvoglv64.dll EXCEPTION_ACCESS_VIOLATION). No amount of GL state
 * save/restore fixes this because the crash is inside the native draw call.
 *
 * This screen uses Minecraft's own rendering pipeline (GuiGraphics) which is
 * guaranteed to work and never conflicts with the driver.
 *
 * Toggle with L key (see ClientInputEvents).
 */
public class ModMenuScreen extends Screen {

    public ModMenuScreen() {
        super(Component.literal("Mod Menu"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Close button
        this.addRenderableWidget(
            Button.builder(Component.literal("Close"), btn -> this.onClose())
                .bounds(centerX - 50, centerY + 40, 100, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darkened background
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Panel background
        int panelW = 260;
        int panelH = 120;
        int px = centerX - panelW / 2;
        int py = centerY - panelH / 2;
        graphics.fill(px, py, px + panelW, py + panelH, 0xCC222222);

        // Border
        graphics.fill(px, py, px + panelW, py + 1, 0xFF55FF55);           // top
        graphics.fill(px, py + panelH - 1, px + panelW, py + panelH, 0xFF55FF55); // bottom
        graphics.fill(px, py, px + 1, py + panelH, 0xFF55FF55);           // left
        graphics.fill(px + panelW - 1, py, px + panelW, py + panelH, 0xFF55FF55); // right

        // Title
        graphics.drawCenteredString(this.font, "§a§lMod Menu", centerX, centerY - 35, 0xFFFFFF);

        // Info text
        graphics.drawCenteredString(this.font, "Mod is working on Minecraft 1.21.4!", centerX, centerY - 15, 0xAAFFAA);
        graphics.drawCenteredString(this.font, "Toggle: press L | Close: ESC or button", centerX, centerY + 5, 0x999999);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
