package edu.unl.csce466;

import com.mojang.logging.LogUtils;
import edu.unl.csce466.imgui.ImGuiRenderer;
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

    public ExampleMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("ExampleMod initialized for Minecraft 1.21.4");
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // Безопасно проверяем наличие игрока
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.screen != null) return;
        } catch (Exception e) {
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_L) {
            // Lazy init + open screen
            ImGuiRenderer.getInstance().initIfNeeded();
            Minecraft.getInstance().setScreen(ImGuiScreen.getInstance());
        }
    }
}
