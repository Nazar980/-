package xyz.breadloaf.imguimc.screen;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import xyz.breadloaf.imguimc.theme.ImGuiLightTheme;

public class RightShiftMenuRenderable implements Renderable {

    private static final Theme THEME = new ImGuiLightTheme();

    private final ImBoolean open = new ImBoolean(true);

    private boolean showAboutWindow = false;
    private boolean showDemoWindow = false;
    private boolean showMetricsWindow = false;

    @Override
    public String getName() {
        return "Right Shift Menu";
    }

    @Override
    public Theme getTheme() {
        return THEME;
    }

    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void render() {
        if (!open.get()) {
            return;
        }

        ImGui.setNextWindowPos(20, 20, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(290, 0, ImGuiCond.FirstUseEver);

        int windowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.AlwaysAutoResize;

        ImGui.begin("Client Menu", open, windowFlags);
        ImGui.text("Лёгкое клиентское ImGui меню");
        ImGui.separator();
        ImGui.text("Открытие / закрытие: Right Shift");
        ImGui.text("Мод работает только на клиенте");
        if (Minecraft.getInstance().player != null) {
            ImGui.text("Игрок: " + Minecraft.getInstance().player.getName().getString());
        }
        ImGui.text("FPS: " + Minecraft.getInstance().getFps());
        ImGui.spacing();

        if (ImGui.checkbox("Показать About Window", showAboutWindow)) {
            showAboutWindow = !showAboutWindow;
        }
        if (ImGui.checkbox("Показать Demo Window", showDemoWindow)) {
            showDemoWindow = !showDemoWindow;
        }
        if (ImGui.checkbox("Показать Metrics Window", showMetricsWindow)) {
            showMetricsWindow = !showMetricsWindow;
        }

        ImGui.spacing();
        if (ImGui.button("Закрыть меню")) {
            open.set(false);
        }
        ImGui.end();

        if (showAboutWindow) {
            ImGui.showAboutWindow();
        }
        if (showDemoWindow) {
            ImGui.showDemoWindow();
        }
        if (showMetricsWindow) {
            ImGui.showMetricsWindow();
        }
    }
}
