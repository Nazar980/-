package edu.unl.csce466.imgui;

import java.util.ArrayList;
import java.util.Objects;

import org.lwjgl.glfw.GLFW;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;

public class ImGuiRenderer {
    private static ImGuiRenderer _INSTANCE = null;
    
    public static ImGuiRenderer getInstance() {
        if (_INSTANCE == null) _INSTANCE = new ImGuiRenderer();
        return _INSTANCE;
    }
    
    private final ArrayList<ImGuiCall> _preDrawCalls = new ArrayList<>();
    private final ArrayList<ImGuiCall> _drawCalls = new ArrayList<>();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();
    
    public void init() {
        init(() -> {});
    }
    
    public void init(ImGuiCall config) {
        ImGui.createContext();
        config.execute();
        imGuiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), false);
        initGl3Renderer();
    }

    private void initGl3Renderer() {
        String[] versions = {"#version 410 core", "#version 150 core", "#version 330 core"};
        for (String glslVersion : versions) {
            try {
                imGuiGl.getClass().getMethod("init", String.class).invoke(imGuiGl, glslVersion);
                return;
            } catch (Exception ignored) {}
            try {
                imGuiGl.getClass().getMethod("init").invoke(imGuiGl);
                return;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Failed to initialize ImGuiImplGl3");
    }
    
    private void newFrameGl3Renderer() {
        try {
            imGuiGl.getClass().getMethod("newFrame").invoke(imGuiGl);
        } catch (Exception ignored) {}
    }
    
    public void preDraw(ImGuiCall drawCall) { _preDrawCalls.add(drawCall); }
    public void draw(ImGuiCall drawCall) { _drawCalls.add(drawCall); }
    
    public void render() {
        for (ImGuiCall pre : _preDrawCalls) pre.execute();
        _preDrawCalls.clear();
        
        imGuiGlfw.newFrame();
        newFrameGl3Renderer();
        ImGui.newFrame();
        
        for (ImGuiCall draw : _drawCalls) draw.execute();
        _drawCalls.clear();
        
        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
        
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backup = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backup);
        }
    }
}