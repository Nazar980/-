package xyz.breadloaf.imguimc.screen;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import xyz.breadloaf.imguimc.automation.AutomationController;
import xyz.breadloaf.imguimc.config.ImguimcConfig;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import xyz.breadloaf.imguimc.theme.ImGuiDarkTheme;

public class RightShiftMenuRenderable implements Renderable {

    private static final Theme THEME = new ImGuiDarkTheme();

    private final ImBoolean open = new ImBoolean(true);
    private final ImguimcConfig config = ImguimcConfig.get();
    private final AutomationController automationController = AutomationController.get();

    private final ImString howManySlotsInput = new ImString(3);
    private final ImString maxActiveSalesInput = new ImString(3);
    private final ImString emeraldMaxCostInput = new ImString(16);
    private final ImString woodMaxCostInput = new ImString(16);
    private final ImString emeraldPickaxeCostInput = new ImString(16);

    public RightShiftMenuRenderable() {
        syncInputsFromConfig();
    }

    @Override
    public String getName() {
        return "Menu";
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
        ImGui.setNextWindowSize(520, 390, ImGuiCond.FirstUseEver);

        int windowFlags = ImGuiWindowFlags.NoCollapse;

        ImGui.begin("Client Menu", open, windowFlags);

        ImGui.text("Automation: " + (automationController.isEnabled() ? "ON" : "OFF"));
        ImGui.text("Status: " + automationController.getStatus());
        ImGui.spacing();

        ImGui.text("Toggle Bind: " + automationController.getToggleBindName());
        ImGui.sameLine();
        if (ImGui.button(automationController.isWaitingForBind() ? "Press any key..." : "Change Bind")) {
            automationController.beginBindCapture();
        }

        if (ImGui.button(automationController.isEnabled() ? "Stop Logic" : "Start Logic")) {
            automationController.toggleEnabled();
        }

        ImGui.spacing();
        renderInputWithBlink("How many slots", howManySlotsInput, ImGuiInputTextFlags.CharsDecimal);
        handleClampedInput(howManySlotsInput, 1, 99, value -> config.howManySlots = value, config.howManySlots);

        renderInputWithBlink("Max Active Sales", maxActiveSalesInput, ImGuiInputTextFlags.CharsDecimal);
        handleClampedInput(maxActiveSalesInput, 1, 100, value -> config.maxActiveSales = value, config.maxActiveSales);

        renderInputWithBlink("Emerald Max Cost", emeraldMaxCostInput, ImGuiInputTextFlags.CharsDecimal);
        handleMinimumInput(emeraldMaxCostInput, value -> config.emeraldMaxCost = value, config.emeraldMaxCost);

        renderInputWithBlink("Wood Max Cost", woodMaxCostInput, ImGuiInputTextFlags.CharsDecimal);
        handleMinimumInput(woodMaxCostInput, value -> config.woodMaxCost = value, config.woodMaxCost);

        renderInputWithBlink("Emerald Pickaxe Cost", emeraldPickaxeCostInput, ImGuiInputTextFlags.CharsDecimal);
        handleMinimumInput(emeraldPickaxeCostInput, value -> config.emeraldPickaxeCost = value, config.emeraldPickaxeCost);

        ImGui.spacing();

        ImGui.end();
    }

    private void renderInputWithBlink(String label, ImString buffer, int flags) {
        ImGui.inputText(label, buffer, flags);
        ImGui.sameLine();
        ImGui.textDisabled(isBlinkVisible() ? "_" : " ");
    }

    private boolean isBlinkVisible() {
        return (System.currentTimeMillis() / 450L) % 2L == 0L;
    }

    private void syncInputsFromConfig() {
        howManySlotsInput.set(String.valueOf(config.howManySlots));
        maxActiveSalesInput.set(String.valueOf(config.maxActiveSales)); // Синхронизация лимита лотов
        emeraldMaxCostInput.set(String.valueOf(config.emeraldMaxCost));
        woodMaxCostInput.set(String.valueOf(config.woodMaxCost));
        emeraldPickaxeCostInput.set(String.valueOf(config.emeraldPickaxeCost));
    }

    private void handleClampedInput(ImString input, int min, int max, IntSetter setter, int currentValue) {
        String sanitized = digitsOnly(input.get());
        if (!sanitized.equals(input.get())) {
            input.set(sanitized);
        }

        if (!sanitized.isEmpty()) {
            int value = clampInt(parseIntSafe(sanitized, currentValue), min, max);
            setter.set(value);
            String normalized = String.valueOf(value);
            if (!normalized.equals(input.get())) {
                input.set(normalized);
            }
            ImguimcConfig.save();
            return;
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            input.set(String.valueOf(currentValue));
        }
    }

    private void handleMinimumInput(ImString input, IntSetter setter, int currentValue) {
        String sanitized = digitsOnly(input.get());
        if (!sanitized.equals(input.get())) {
            input.set(sanitized);
        }

        if (!sanitized.isEmpty()) {
            int value = Math.max(0, parseIntSafe(sanitized, currentValue));
            setter.set(value);
            String normalized = String.valueOf(value);
            if (!normalized.equals(input.get())) {
                input.set(normalized);
            }
            ImguimcConfig.save();
            return;
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            input.set(String.valueOf(currentValue));
        }
    }

    private String digitsOnly(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }
}
