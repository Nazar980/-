package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImFontAtlas;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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

            // true = install GLFW callbacks, chain Minecraft's existing callbacks
            imGuiGlfw.init(windowHandle, true);

            // Use GLSL 150 core to match Minecraft's pipeline (avoids driver-level
            // mismatches when Minecraft 1.21.4's new GpuDevice keeps state around).
            imGuiGl.init("#version 150 core");

            // Force-build the font atlas texture so the very first newFrame() call
            // doesn't assert "Font Atlas not built!".
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
        if (!initialized || imGuiGlfw == null || imGuiGl == null || !fontAtlasBuilt) {
            return;
        }

        // ---------------------------------------------------------------
        // SAVE Minecraft 1.21.4 GpuDevice OpenGL state.
        // The new render pipeline leaves GL state in an undefined shape
        // after flipFrame, and NVIDIA's driver crashes (nvoglv64.dll)
        // if ImGui draws with the wrong program/VAO/buffer bound.
        // ---------------------------------------------------------------
        int prevProgram       = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao           = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuf      = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int prevElementBuf    = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevTexture2D     = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevBlend     = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevCullFace  = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevScissor   = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        // Drain any pending GL errors so they don't get attributed to ImGui
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* discard */ }

        try {
            imGuiGlfw.newFrame();
            ImGui.newFrame();

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
        } catch (Throwable t) {
            LOGGER.error("[ImGui] Render error: " + t.getMessage(), t);
        } finally {
            // ---------------------------------------------------------------
            // RESTORE Minecraft's OpenGL state so its next frame doesn't
            // start with ImGui's leftover program/VAO/buffers bound.
            // ---------------------------------------------------------------
            try {
                GL20.glUseProgram(prevProgram);
                GL30.glBindVertexArray(prevVao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, prevElementBuf);
                GL13.glActiveTexture(prevActiveTexture);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture2D);
                setGlState(GL11.GL_BLEND,        prevBlend);
                setGlState(GL11.GL_CULL_FACE,    prevCullFace);
                setGlState(GL11.GL_DEPTH_TEST,   prevDepthTest);
                setGlState(GL11.GL_SCISSOR_TEST, prevScissor);
            } catch (Throwable ignored) {
                // never let restore code crash the game
            }
        }
    }

    private static void setGlState(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap); else GL11.glDisable(cap);
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
