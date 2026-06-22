package edu.unl.csce466.imgui;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.util.ArrayList;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImGuiRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImGuiRenderer.class);
    private static ImGuiRenderer instance;

    public static ImGuiRenderer getInstance() {
        if (instance == null) {
            instance = new ImGuiRenderer();
        }
        return instance;
    }

    private final ArrayList<ImGuiCall> drawCalls = new ArrayList<>();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();
    private boolean initialized;
    private boolean menuVisible;

    private ImGuiRenderer() {
    }

    public void init(long windowHandle, ImGuiCall config) {
        if (initialized) {
            return;
        }

        if (windowHandle == 0L) {
            LOGGER.error("[ImGui] Window handle is 0, init skipped");
            return;
        }

        try {
            ImGui.createContext();

            if (config != null) {
                config.execute();
            }

            imGuiGlfw.init(windowHandle, false);
            imGuiGl.init("#version 330 core");

            initialized = true;
            LOGGER.info("[ImGui] Initialized");
        } catch (Throwable throwable) {
            LOGGER.error("[ImGui] Failed to initialize", throwable);
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void showMenu() {
        menuVisible = true;
    }

    public void hideMenu() {
        menuVisible = false;
    }

    public boolean isMenuVisible() {
        return menuVisible;
    }

    public void toggleMenu() {
        menuVisible = !menuVisible;
    }

    public void draw(ImGuiCall drawCall) {
        if (drawCall != null) {
            drawCalls.add(drawCall);
        }
    }

    public void render() {
        if (!initialized) {
            return;
        }

        try {
            imGuiGlfw.newFrame();
            ImGui.newFrame();

            if (menuVisible) {
                ImGui.begin("CSCE466 ImGui Menu");
                ImGui.text("ImGui works on Minecraft Forge 1.21.4");
                ImGui.text("Press L to toggle, ESC to close");
                ImGui.end();
            }

            for (ImGuiCall drawCall : drawCalls) {
                try {
                    drawCall.execute();
                } catch (Throwable throwable) {
                    LOGGER.error("[ImGui] Draw call error", throwable);
                }
            }
            drawCalls.clear();

            ImGui.render();
            imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
        } catch (Throwable throwable) {
            LOGGER.error("[ImGui] Render error", throwable);
        }
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            imGuiGl.shutdown();
            imGuiGlfw.shutdown();
            ImGui.destroyContext();
        } catch (Throwable throwable) {
            LOGGER.error("[ImGui] Shutdown error", throwable);
        } finally {
            initialized = false;
        }
    }
}