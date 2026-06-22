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
    private boolean contextCreated = false;
    
    private ImGuiRenderer() {}
    
    /**
     * Инициализация ImGui с правильным GLFW window handle от Minecraft
     */
    public void init(long windowHandle, ImGuiCall config) {
        if (initialized) {
            LOGGER.info("[ImGui] Already initialized, skipping.");
            return;
        }
        
        LOGGER.info("[ImGui] Initializing with window handle: 0x" + Long.toHexString(windowHandle));
        
        try {
            // 1. Создаём ImGui контекст
            if (!contextCreated) {
                ImGui.createContext();
                contextCreated = true;
            }
            
            // 2. Конфигурация
            if (config != null) {
                config.execute();
            } else {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
                // Выключаем Viewports — они вызывают проблемы в Minecraft
                // ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            }
            
            // 3. Инициализация GLFW бэкенда (FALSE = не устанавливаем GLFW коллбэки,
            //    потому что Minecraft сам обрабатывает ввод)
            imGuiGlfw.init(windowHandle, false);
            
            // 4. Инициализация OpenGL бэкенда
            imGuiGl.init("#version 330 core");
            
            initialized = true;
            LOGGER.info("[ImGui] Successfully initialized!");
            
        } catch (Exception e) {
            LOGGER.error("[ImGui] Failed to initialize: " + e.getMessage(), e);
            // Пробуем с другой версией GLSL
            tryFallbackInit(windowHandle, config);
        }
    }
    
    /**
     * Фоллбэк инициализация если main init провалилась
     */
    private void tryFallbackInit(long windowHandle, ImGuiCall config) {
        try {
            if (!contextCreated) {
                ImGui.createContext();
                contextCreated = true;
            }
            
            if (config != null) {
                config.execute();
            }
            
            imGuiGlfw.init(windowHandle, false);
            // Пробуем без явной версии GLSL
            imGuiGl.init();
            
            initialized = true;
            LOGGER.info("[ImGui] Initialized with fallback GLSL version!");
        } catch (Exception e) {
            LOGGER.error("[ImGui] Fallback init also failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lazy init — вызывается если ImGui ещё не инициализирован
     */
    public void initIfNeeded() {
        if (initialized) return;
        
        try {
            // Получаем window handle от Minecraft
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                LOGGER.warn("[ImGui] Cannot init: Minecraft or Window is null");
                return;
            }
            
            long window = mc.getWindow().getWindow();
            init(window, () -> {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
            });
        } catch (Exception e) {
            LOGGER.error("[ImGui] Lazy init failed: " + e.getMessage(), e);
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
    
    public void shutdown() {
        if (!initialized) return;
        try {
            imGuiGl.shutdown();
            imGuiGlfw.shutdown();
            ImGui.destroyContext();
            initialized = false;
            contextCreated = false;
        } catch (Exception e) {
            LOGGER.error("[ImGui] Shutdown error: " + e.getMessage(), e);
        }
    }
}
