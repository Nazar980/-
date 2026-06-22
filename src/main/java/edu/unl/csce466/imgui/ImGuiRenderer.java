package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;

public class ImGuiRenderer {
    private static ImGuiRenderer _INSTANCE = null;
    
    public static ImGuiRenderer getInstance() {
        if(_INSTANCE == null) _INSTANCE = new ImGuiRenderer();
        return _INSTANCE;
    }
    
    private final ArrayList<ImGuiCall> _drawCalls = new ArrayList<>();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();
    private boolean initialized = false;
    
    private ImGuiRenderer() {}
    
    public void init(ImGuiCall config) {
        if (initialized) {
            return;
        }
        
        ImGui.createContext();
        
        if (config != null) {
            config.execute();
        }
        
        // Получаем текущий GLFW контекст окна Minecraft
        long windowPtr = GLFW.glfwGetCurrentContext();
        if (windowPtr == 0) {
            throw new IllegalStateException("Cannot get current GLFW context");
        }
        
        imGuiGlfw.init(windowPtr, true);
        imGuiGl.init("#version 330 core");
        
        // ВАЖНО: Собираем атлас шрифтов сразу после инициализации
        ImGui.getIO().getFonts().build();
        
        initialized = true;
    }
    
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    
    public void render() {
        if (!initialized) {
            return;
        }
        
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        
        // Выполняем все отложенные draw вызовы
        for(ImGuiCall drawCall : _drawCalls) {
            drawCall.execute();
        }
        _drawCalls.clear();
        
        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
        
        // Если включены Viewports, обновляем их
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
        }
    }
    
    public void shutdown() {
        if (!initialized) return;
        imGuiGl.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
        initialized = false;
    }
}
