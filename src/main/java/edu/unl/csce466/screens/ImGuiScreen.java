package edu.unl.csce466.screens;

import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import edu.unl.csce466.ExampleMod;

public class ImGuiScreen extends Screen {
    
    private static ImGuiScreen _INSTANCE = null;
    private boolean _buttonClicked = false;
    
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        ImGuiRenderer.getInstance().draw(() -> {
            ShowModMenu();
        });
        
        ImGuiRenderer.getInstance().draw(() -> {
            ImGui.begin("Custom Window");
            ImGui.text("Example Window (Forge 1.21.4)");
            if (ImGui.button("Click Me!")) {
                _buttonClicked = true;
            }
            if (_buttonClicked) {
                ImGui.text("Button has been clicked!");
            }
            ImGui.end();
        });
    }

    private void ShowModMenu() {
        ExampleMod.Zeus zeus = new ExampleMod.Zeus();

        if (ImGui.beginMenu("Level Up")) {
            if (ImGui.button("Level Up!")) zeus.LevelUp();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Health")) {
            if (ImGui.button("Health boost")) zeus.Health();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Diamonds")) {
            if (ImGui.button("Give Diamonds")) zeus.GiveDiamonds();
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Stick")) {
            if (ImGui.button("Stick!")) zeus.Stick();
            ImGui.endMenu();
        }
    }
}