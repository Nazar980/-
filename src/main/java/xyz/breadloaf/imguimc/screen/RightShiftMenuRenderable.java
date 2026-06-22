package xyz.breadloaf.imguimc.screen;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImString;
import xyz.breadloaf.imguimc.config.ImguimcConfig;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import xyz.breadloaf.imguimc.theme.ImGuiDarkTheme;

public class RightShiftMenuRenderable implements Renderable {

    private static final Theme THEME = new ImGuiDarkTheme();

    private final ImBoolean open = new ImBoolean(true);
    private final ImguimcConfig config = ImguimcConfig.get();

    private final ImFloat menuScale = new ImFloat(config.menuScale);
    private final ImString howManySlotsInput = new ImString(3);
    private final ImString emeraldMaxCostInput = new ImString(16);
    private final ImString woodMaxCostInput = new ImString(16);

    public RightShiftMenuRenderable() {
        syncInputsFromConfig();
    }

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
        ImGui.setNextWindowSize(320, 0, ImGuiCond.FirstUseEver);

        int windowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.AlwaysAutoResize;

        ImGui.begin("Client Menu", open, windowFlags);
        ImGui.setWindowFontScale(menuScale.get());

        if (ImGui.sliderFloat("Menu Scale", menuScale, 0.75f, 2.00f)) {
            config.menuScale = menuScale.get();
            ImguimcConfig.save();
        }

        ImGui.spacing();

        ImGui.inputText("How many slots", howManySlotsInput, ImGuiInputTextFlags.CharsDecimal);
        handleHowManySlots();

        ImGui.inputText("Emerald Max Cost", emeraldMaxCostInput);
        handleSignedIntegerInput(emeraldMaxCostInput, true);

        ImGui.inputText("Wood Max Cost", woodMaxCostInput);
        handleSignedIntegerInput(woodMaxCostInput, false);

        ImGui.spacing();
        ImGui.button("Start");

        ImGui.end();
    }

    private void syncInputsFromConfig() {
        menuScale.set(config.menuScale);
        howManySlotsInput.set(String.valueOf(config.howManySlots));
        emeraldMaxCostInput.set(String.valueOf(config.emeraldMaxCost));
        woodMaxCostInput.set(String.valueOf(config.woodMaxCost));
    }

    private void handleHowManySlots() {
        String sanitized = digitsOnly(howManySlotsInput.get());
        if (sanitized.length() > 2) {
            sanitized = sanitized.substring(0, 2);
        }

        if (!sanitized.equals(howManySlotsInput.get())) {
            howManySlotsInput.set(sanitized);
        }

        if (!sanitized.isEmpty()) {
            int value = clampInt(parseIntSafe(sanitized, config.howManySlots), 1, 99);
            config.howManySlots = value;
            String normalized = String.valueOf(value);
            if (!normalized.equals(howManySlotsInput.get())) {
                howManySlotsInput.set(normalized);
            }
            ImguimcConfig.save();
            return;
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            howManySlotsInput.set(String.valueOf(config.howManySlots));
        }
    }

    private void handleSignedIntegerInput(ImString input, boolean emerald) {
        String sanitized = signedIntegerOnly(input.get());
        if (!sanitized.equals(input.get())) {
            input.set(sanitized);
        }

        if (!sanitized.isEmpty() && !"-".equals(sanitized)) {
            int value = parseClampedInt(sanitized);
            if (emerald) {
                config.emeraldMaxCost = value;
            } else {
                config.woodMaxCost = value;
            }
            ImguimcConfig.save();
            return;
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            if (emerald) {
                input.set(String.valueOf(config.emeraldMaxCost));
            } else {
                input.set(String.valueOf(config.woodMaxCost));
            }
        }
    }

    private String digitsOnly(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    private String signedIntegerOnly(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '-' && builder.isEmpty()) {
                builder.append(character);
                continue;
            }
            if (Character.isDigit(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseClampedInt(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (parsed < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
