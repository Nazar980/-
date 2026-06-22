package edu.unl.csce466;

import com.mojang.logging.LogUtils;
import edu.unl.csce466.screens.ImGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ImGuiScreen IMGUI_SCREEN = ImGuiScreen.getInstance();

    public ExampleMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(IMGUI_SCREEN);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("ImGui initialized for Minecraft 1.21.4");
            IMGUI_SCREEN.init();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null) return;
        if (Minecraft.getInstance().screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_L) {
            Minecraft.getInstance().setScreen(IMGUI_SCREEN);
        }
    }
}
