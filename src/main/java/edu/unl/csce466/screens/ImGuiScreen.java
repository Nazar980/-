package edu.unl.csce466.screens;

import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImGuiScreen extends Screen {
    
    private static ImGuiScreen _INSTANCE = null;

    public static ImGuiScreen getInstance() {
        if (_INSTANCE == null) _INSTANCE = new ImGuiScreen();
        return _INSTANCE;
    }

    private ImGuiScreen() {
        super(Component.literal("ImGui"));
    }
    
    public void init() {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Не вызываем super.render для прозрачного фона (опционально)
        // super.render(guiGraphics, mouseX, mouseY, partialTick);

        ImGuiRenderer.getInstance().draw(() -> {
            ImGui.begin("ImGui Menu");
            ImGui.text("ImGui is working on Minecraft 1.21.4!");
            ImGui.text("Press ESC to close");
            ImGui.end();
        });
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
