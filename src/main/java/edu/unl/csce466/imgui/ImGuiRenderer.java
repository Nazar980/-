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
    private boolean _initialized = false;
    
    private ImGuiRenderer() {}
    
    /**
     * Initialize ImGui with given GLFW window handle
     */
    public void init(long windowPtr, ImGuiCall config) {
        if (_initialized) {
            LOGGER.info("[ImGui] Already initialized, skipping.");
            return;
        }
        
        LOGGER.info("[ImGui] Initializing ImGui. Window: 0x{}", Long.toHexString(windowPtr));
        
        try {
            // Step 1: Create ImGui context
            ImGui.createContext();
            
            // Step 2: Configure
            if (config != null) {
                config.execute();
            } else {
                ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
            }
            
            // Step 3: Init GLFW backend (false = don't install callbacks, Minecraft handles input)
            imGuiGlfw.init(windowPtr, false);
            
            // Step 4: Init OpenGL backend with basic GLSL
            imGuiGl.init("#version 150");
            
            _initialized = true;
            LOGGER.info("[ImGui] Initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("[ImGui] Init failed, trying fallback...", e);
            tryFallback(windowPtr, config);
        }
    }
    
    private void tryFallback(long windowPtr, ImGuiCall config) {
        try {
            ImGui.createContext();
            if (config != null) config.execute();
            imGuiGlfw.init(windowPtr, false);
            imGuiGl.init(); // No explicit GLSL version
            _initialized = true;
            LOGGER.info("[ImGui] Initialized with fallback!");
        } catch (Exception e2) {
            LOGGER.error("[ImGui] Fallback also failed!", e2);
        }
    }
    
    /**
     * Lazy init using Minecraft's current window. Call from main thread only.
     */
    public void initIfNeeded() {
        if (_initialized) return;
        
        // Use GLFW to get native window directly - no Minecraft API needed!
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) {
            LOGGER.warn("[ImGui] No GLFW context available yet");
            return;
        }
        
        // Actually, glfwGetCurrentContext returns GL context, not window.
        // We need the WINDOW. Since we can't rely on Minecraft API (TLauncher issues),
        // we use the hack: get any existing window from GLFW
        // This is safe because Minecraft always has exactly one window
        
        // Alternative: walk through GLFW windows
        // GLFW.glfwGetWindows() is not in LWJGL 3.3.3
        
        // So we need to store it from the mixin
        LOGGER.warn("[ImGui] Lazy init called but window handle not yet set. Use mixin init.");
    }
    
    /**
     * Called from the mixin that has access to the real window handle
     */
    public void initFromMixin(long windowHandle) {
        if (_initialized) return;
        init(windowHandle, () -> {
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        });
    }
    
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    
    public void render() {
        if (!_initialized) return;
        
        try {
            imGuiGlfw.newFrame();
            ImGui.newFrame();
            
            for (ImGuiCall drawCall : _drawCalls) {
                try {
                    drawCall.execute();
                } catch (Exception e) {
                    LOGGER.error("[ImGui] Draw call error", e);
                }
            }
            _drawCalls.clear();
            
            ImGui.render();
            imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
        } catch (Exception e) {
            LOGGER.error("[ImGui] Render error", e);
        }
    }
    
    public void shutdown() {
        if (!_initialized) return;
        try {
            imGuiGl.shutdown();
            imGuiGlfw.shutdown();
            ImGui.destroyContext();
            _initialized = false;
        } catch (Exception ignored) {}
    }
}
