package xyz.breadloaf.imguimc.imgui;

import com.mojang.blaze3d.platform.Window;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiDockNode;
import net.minecraft.util.profiling.Profiler;
import xyz.breadloaf.imguimc.Imguimc;
import xyz.breadloaf.imguimc.WindowScaling;
import xyz.breadloaf.imguimc.interfaces.Renderable;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

public class ImguiLoader {
    private static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private static long windowHandle;

    public static void onGlfwInit(long handle) {
        initializeImGui(handle);
        imGuiGlfw.init(handle, true);
        imGuiGl3.init();
        windowHandle = handle;
    }

    public static void onFrameRender() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        setupDocking();

        for (Renderable renderable : Imguimc.renderstack) {
            Profiler.get().push("ImGui Render/" + renderable.getName());
            renderable.getTheme().preRender();
            renderable.render();
            renderable.getTheme().postRender();
            Profiler.get().pop();
        }

        for (Renderable renderable : Imguimc.toRemove) {
            Imguimc.pullRenderable(renderable);
        }
        Imguimc.toRemove.clear();

        finishDocking();

        ImGui.render();
        endFrame(windowHandle);
    }

    private static void setupDocking() {
        int windowFlags = ImGuiWindowFlags.NoDocking;

        Window window = Imguimc.MINECRAFT.getWindow();

        ImGui.setNextWindowPos(window.getX(), window.getY(), ImGuiCond.Always);
        ImGui.setNextWindowSize(window.getWidth(), window.getHeight());
        windowFlags |= ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoNavInputs;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
        ImGui.begin("imgui-mc docking host window", windowFlags);
        ImGui.popStyleVar(2);

        int id = ImGui.dockSpace(Imguimc.getDockId(), 0, 0,
                imgui.flag.ImGuiDockNodeFlags.PassthruCentralNode
                        | imgui.flag.ImGuiDockNodeFlags.NoCentralNode
                        | imgui.flag.ImGuiDockNodeFlags.NoDockingInCentralNode);

        ImGuiDockNode centre = imgui.internal.ImGui.dockBuilderGetCentralNode(id);
        if (centre == null) {
            WindowScaling.X_OFFSET = 0;
            WindowScaling.Y_OFFSET = 0;
            WindowScaling.Y_TOP_OFFSET = 0;
            WindowScaling.WIDTH = window.getWidth();
            WindowScaling.HEIGHT = window.getHeight();
            WindowScaling.update();
            return;
        }

        WindowScaling.X_OFFSET = (int) centre.getPosX() - window.getX();
        WindowScaling.Y_OFFSET = (int) centre.getPosY() - window.getY();
        WindowScaling.Y_TOP_OFFSET = (int) (window.getHeight() - ((centre.getPosY() - window.getY()) + centre.getSizeY()));
        WindowScaling.WIDTH = (int) centre.getSizeX();
        WindowScaling.HEIGHT = (int) centre.getSizeY();
        WindowScaling.update();
    }

    private static void finishDocking() {
        ImGui.end();
    }

    private static void initializeImGui(long glHandle) {
        ImGui.createContext();

        final ImGuiIO io = ImGui.getIO();

        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setConfigViewportsNoTaskBarIcon(true);

        final ImFontAtlas fontAtlas = io.getFonts();
        final ImFontConfig fontConfig = new ImFontConfig();

        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic());
        fontAtlas.addFontDefault();
        fontConfig.setMergeMode(true);
        fontConfig.setPixelSnapH(true);
        fontConfig.destroy();

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final ImGuiStyle style = ImGui.getStyle();
            style.setWindowRounding(0.0f);
            style.setColor(ImGuiCol.WindowBg, ImGui.getColorU32(ImGuiCol.WindowBg, 1));
        }
    }

    private static void endFrame(long windowPtr) {
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }
    }
}
