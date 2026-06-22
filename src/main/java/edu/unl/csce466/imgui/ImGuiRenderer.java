package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImFontAtlas;
import imgui.flag.ImGuiConfigFlags;
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

            // Do not install ImGui GLFW callbacks here. Minecraft owns the input callbacks,
            // and we toggle the menu via Forge events instead.
            imGuiGlfw.init(windowHandle, false);

            // Minecraft 1.21.4 uses an OpenGL 3.2 core profile context. GLSL 150 is the
            // matching shader version for that profile and avoids native NVIDIA crashes.
            imGuiGl.init("#version 150");

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

            GlStateBackup state = GlStateBackup.capture();
            try {
                imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
            } finally {
                state.restore();
            }
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

    private static final class GlStateBackup {
        private final int program;
        private final int activeTexture;
        private final int textureBinding;
        private final int arrayBuffer;
        private final int vertexArray;
        private final boolean blend;
        private final boolean cullFace;
        private final boolean depthTest;
        private final boolean scissorTest;

        private GlStateBackup(
            int program,
            int activeTexture,
            int textureBinding,
            int arrayBuffer,
            int vertexArray,
            boolean blend,
            boolean cullFace,
            boolean depthTest,
            boolean scissorTest
        ) {
            this.program = program;
            this.activeTexture = activeTexture;
            this.textureBinding = textureBinding;
            this.arrayBuffer = arrayBuffer;
            this.vertexArray = vertexArray;
            this.blend = blend;
            this.cullFace = cullFace;
            this.depthTest = depthTest;
            this.scissorTest = scissorTest;
        }

        private static GlStateBackup capture() {
            return new GlStateBackup(
                GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
                GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_CULL_FACE),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            );
        }

        private void restore() {
            GL20.glUseProgram(program);
            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBinding);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            GL30.glBindVertexArray(vertexArray);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_CULL_FACE, cullFace);
            setEnabled(GL11.GL_DEPTH_TEST, depthTest);
            setEnabled(GL11.GL_SCISSOR_TEST, scissorTest);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }
}
