package edu.unl.csce466.imgui;
import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImFontAtlas;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
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
    
    public void init(long windowPtr, ImGuiCall config) {
        if (_initialized) {
            return;
        }
        
        LOGGER.info("[ImGui] Initializing with window handle: 0x{}", Long.toHexString(windowPtr));
        
        try {
            // Step 1: Create context
            ImGui.createContext();
            
            // Step 2: Configure IO
            if (config != null) {
                config.execute();
            }
            
            // Step 3: Build font atlas BEFORE backend init
            // This is the KEY FIX: fonts must be built before NewFrame() is ever called
            ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
            fontAtlas.build();
            
            // Step 4: Init GLFW backend (false = don't set callbacks)
            imGuiGlfw.init(windowPtr, false);
            
            // Step 5: Init OpenGL backend - this uploads font texture to GPU
            imGuiGl.init("#version 150");
            
            _initialized = true;
            LOGGER.info("[ImGui] Successfully initialized!");
            
        } catch (Exception e) {
            LOGGER.error("[ImGui] Init failed", e);
        }
    }
    
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
                    LOGGER.error("[ImGui] Draw error", e);
                }
            }
            _drawCalls.clear();
            
            ImGui.render();
            imGuiGl.renderDrawData(ImGui.getDrawData());
        } catch (Exception e) {
            LOGGER.error("[ImGui] Render error", e);
        }
    }
}
