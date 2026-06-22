package edu.unl.csce466.imgui;
import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImFontAtlas;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;
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
            imGuiGlfw.init(windowHandle, true);
            imGuiGl.init("#version 150 core");
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
    public boolean isInitialized() { return initialized; }
    public void showMenu() { menuVisible = true; }
    public void hideMenu() { menuVisible = false; }
    public boolean isMenuVisible() { return menuVisible; }
    public void toggleMenu() { menuVisible = !menuVisible; }
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    /**
     * Called from RenderSystemMixin at TAIL of flipFrame — i.e. AFTER
     * glfwSwapBuffers.  At this point Minecraft's GpuDevice pipeline
     * has finished and GL state is clean.
     *
     * If there is nothing to draw we skip entirely (zero overhead).
     *
     * After drawing we call glfwSwapBuffers ourselves so the ImGui
     * overlay becomes visible on screen (otherwise it would only
     * appear in the back-buffer and never be shown).
     */
    public void render(long windowHandle) {
        if (!initialized || imGuiGlfw == null || imGuiGl == null || !fontAtlasBuilt) {
            return;
        }
        // Skip entirely when nothing to render — no perf hit.
        if (!menuVisible && _drawCalls.isEmpty()) {
            return;
        }
        // Drain any stale GL errors
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* discard */ }
        // Save minimal GL state
        int prevProgram  = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao      = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuf = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int prevElemBuf  = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int prevTex      = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevBlend    = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepth    = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevCull     = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean prevScissor  = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
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
            // Swap buffers so the ImGui overlay is actually visible
            if (windowHandle != 0) {
                GLFW.glfwSwapBuffers(windowHandle);
            }
        } catch (Throwable t) {
            LOGGER.error("[ImGui] Render error: " + t.getMessage(), t);
        } finally {
            // Restore GL state
            try {
                GL20.glUseProgram(prevProgram);
                GL30.glBindVertexArray(prevVao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuf);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, prevElemBuf);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
                if (prevBlend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
                if (prevDepth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
                if (prevCull) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
                if (prevScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            } catch (Throwable ignored) {}
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
