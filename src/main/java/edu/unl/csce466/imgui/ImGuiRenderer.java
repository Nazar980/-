package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
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
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();
    private boolean initialized = false;
    
    private ImGuiRenderer() {}
    
    /**
     * Инициализация ImGui с GLFW window handle
     * @param windowHandle GLFWwindow* от GLFW.glfwGetCurrentContext()
     * @param config коллбэк для настройки ImGui (флаги, шрифты и т.д.)
     */
    public void init(long windowHandle, ImGuiCall config) {
        if (initialized) {
            LOGGER.info("[ImGui] Already initialized, skipping.");
            return;
        }
        
        LOGGER.info("[ImGui] Initializing with window handle: 0x" + Long.toHexString(windowHandle));
        
        try {
            // 1. Создаём ImGui контекст
            ImGui.createContext();
            
            // 2. Конфигурация (докинг, шрифты и т.д.)
            if (config != null) {
                config.execute();
            }
            
            // 3. Инициализация GLFW бэкенда
            // installCallbacks = false — Minecraft сам обрабатывает ввод
            imGuiGlfw.init(windowHandle, false);
            
            // 4. Инициализация OpenGL бэкенда
            imGuiGl.init("#version 330 core");
            
            initialized = true;
            LOGGER.info("[ImGui] Successfully initialized!");
            
        } catch (Exception e) {
            LOGGER.error("[ImGui] Failed to initialize: " + e.getMessage(), e);
            initialized = false;
        }
    }
    
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    
    public void render() {
        if (!initialized) {
            return;
        }
        
        try {
            imGuiGlfw.newFrame();
            ImGui.newFrame();
            
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
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void shutdown() {
        if (!initialized) return;
        try {
            imGuiGl.shutdown();
            imGuiGlfw.shutdown();
            ImGui.destroyContext();
            initialized = false;
        } catch (Exception e) {
            LOGGER.error("[ImGui] Shutdown error: " + e.getMessage(), e);
        }
    }
}
