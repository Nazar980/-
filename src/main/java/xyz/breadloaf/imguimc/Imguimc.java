package xyz.breadloaf.imguimc;

import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.breadloaf.imguimc.debug.DebugRenderable;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.screen.RightShiftMenuScreen;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class Imguimc implements ClientModInitializer {
    public static final String MODID = "imgui-mc";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final Minecraft MINECRAFT = Minecraft.getInstance();
    public static final ArrayList<Renderable> renderstack = new ArrayList<>();
    public static final ArrayList<Renderable> toRemove = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        if ("true".equals(System.getProperty("imgui-mc.debug"))) {
            LOGGER.info("In development environment, pushing debug renderable.");
            pushRenderable(new DebugRenderable());
        }
    }

    public static Renderable pushRenderable(Renderable renderable) {
        renderstack.add(renderable);
        return renderable;
    }

    public static Renderable pullRenderable(Renderable renderable) {
        renderstack.remove(renderable);
        return renderable;
    }

    public static Renderable pullRenderableAfterRender(Renderable renderable) {
        toRemove.add(renderable);
        return renderable;
    }

    public static boolean isClientMenuOpen() {
        return MINECRAFT.screen instanceof RightShiftMenuScreen;
    }

    public static boolean toggleClientMenu() {
        if (isClientMenuOpen()) {
            MINECRAFT.setScreen(null);
            return true;
        }

        if (MINECRAFT.screen == null) {
            MINECRAFT.setScreen(new RightShiftMenuScreen());
            return true;
        }

        return false;
    }

    public static boolean shouldCancelGameKeyboardInputs() {
        try {
            return ImGui.getIO().getWantCaptureKeyboard()
                    || ImGui.getIO().getWantTextInput()
                    || ImGui.isAnyItemActive()
                    || ImGui.isAnyItemFocused();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getDockId() {
        return ImGui.getID("imgui-mc dockspace");
    }
}
