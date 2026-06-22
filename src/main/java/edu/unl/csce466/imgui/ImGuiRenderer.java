package edu.unl.csce466.imgui;
import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImFontAtlas;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ImGuiRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImGuiRenderer.class);
    private static ImGuiRenderer _INSTANCE = null;
    public static ImGuiRenderer getInstance() {
        if (_INSTANCE == null) _INSTANCE = new ImGuiRenderer();
        return _INSTANCE;
    }
    private final ArrayList<ImGuiCall> _drawCalls = new ArrayList<>();
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl;
    private boolean initialized = false;
    private boolean fontAtlasBuilt = false;
    private boolean menuVisible = false;
    private ImGuiRenderer() {}
    public void init(long windowHandle, ImGuiCall config) {
        if (initialized) {
            LOGGER.info("[ImGui] Already initialized, skipping.");
            return;
        }
        LOGGER.info("[ImGui] Initializing with window handle: 0x" + Long.toHexString(windowHandle));
        try {
            ImGui.createContext();
            if (config != null) {
                config.execute();
            }
            imGuiGlfw = new ImGuiImplGlfw();
            imGuiGl = new ImGuiImplGl3();
            // true = install GLFW callbacks, chain existing Minecraft callbacks
            imGuiGlfw.init(windowHandle, true);
            imGuiGl.init("#version 330 core");
            // Force-build the font atlas texture right now so that
            // the very first newFrame() call doesn't assert.
            // ImGuiImplGl3.init() should already do this, but we make sure.
            ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
            if (!fontAtlas.isBuilt()) {
                LOGGER.info("[ImGui] Building font atlas manually...");
                fontAtlas.build();
            }
            fontAtlasBuilt = true;
            initialized = true;
            LOGGER.info("[ImGui] Successfully initialized!");
        } catch (Exception e) {
            LOGGER.error("[ImGui] Failed to initialize: " + e.getMessage(), e);
            initialized = false;
        }
    }
    public boolean isInitialized() {
        return initialized;
    }
    public void showMenu() { menuVisible = true; }
    public void hideMenu() { menuVisible = false; }
    public boolean isMenuVisible() { return menuVisible; }
    public void toggleMenu() { menuVisible = !menuVisible; }
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    public void render() {
        if (!initialized || imGuiGlfw == null || imGuiGl == null) {
            return;
        }
        // Safety: don't render if font atlas hasn't been built yet
        if (!fontAtlasBuilt) {
            return;
        }
        try {
            // Double-check font atlas before calling newFrame
            ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
            if (!fontAtlas.isBuilt()) {
                fontAtlas.build();
            }
            imGuiGlfw.newFrame();
            ImGui.newFrame();
            // Built-in mod menu
            if (menuVisible) {
                ImGui.begin("ImGui Menu", ImGuiWindowFlags.AlwaysAutoResize);
                ImGui.text("ImGui is working on Minecraft 1.21.4!");
                ImGui.separator();
                ImGui.text("Toggle: press L");
                if (ImGui.button("Close")) {
                    menuVisible = false;
                }
                ImGui.end();
            }
            // External draw calls
            for (ImGuiCall drawCall : _drawCalls) {
                try {
                    drawCall.execute();
                } catch (Exception e) {
                    LOGGER.error("[ImGui] Draw call error: " + e.getMessage(), e);
                }
            }
            _drawCalls.clear();
            ImGui.render();
            imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
        } catch (Exception e) {
            LOGGER.error("[ImGui] Render error: " + e.getMessage(), e);
        }
    }
    public void shutdown() {
        if (!initialized) return;
        try {
            if (imGuiGl != null) imGuiGl.shutdown();
            if (imGuiGlfw != null) imGuiGlfw.shutdown();
            ImGui.destroyContext();
            initialized = false;
            fontAtlasBuilt = false;
        } catch (Exception e) {
            LOGGER.error("[ImGui] Shutdown error: " + e.getMessage(), e);
        }
    }
}
