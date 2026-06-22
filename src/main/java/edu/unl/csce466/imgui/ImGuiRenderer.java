package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;

public class ImGuiRenderer {
    private static ImGuiRenderer _INSTANCE = null;
    
    public static ImGuiRenderer getInstance() {
        if(_INSTANCE == null) _INSTANCE = new ImGuiRenderer();
        return _INSTANCE;
    }
    
    private final ArrayList<ImGuiCall> _drawCalls = new ArrayList<>();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();
    
    private ImGuiRenderer() {}
    
    public void init(ImGuiCall config) {
        ImGui.createContext();
        config.execute();
        
        // Исправлено: прямой доступ к окну через Minecraft.getInstance().getWindow().getWindow()
        // Это стандартный способ в 1.21.4
        long windowPtr = Minecraft.getInstance().getWindow().getWindow();
        
        imGuiGlfw.init(windowPtr, true);
        imGuiGl.init("#version 410 core");
    }
    
    public void draw(ImGuiCall drawCall) {
        _drawCalls.add(drawCall);
    }
    
    public void render() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        
        for(ImGuiCall drawCall : _drawCalls) {
            drawCall.execute();
        }
        _drawCalls.clear();
        
        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
    }
}
