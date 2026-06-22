package xyz.breadloaf.imguimc.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.breadloaf.imguimc.Imguimc;

public class RightShiftMenuScreen extends Screen {

    private final RightShiftMenuRenderable menuRenderable = new RightShiftMenuRenderable();
    private boolean registered = false;

    public RightShiftMenuScreen() {
        super(Component.literal("ImGui Client Menu"));
    }

    @Override
    protected void init() {
        super.init();
        if (!registered) {
            Imguimc.pushRenderable(menuRenderable);
            registered = true;
        }
    }

    @Override
    public void removed() {
        if (registered) {
            Imguimc.pullRenderable(menuRenderable);
            registered = false;
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (!menuRenderable.isOpen() && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
