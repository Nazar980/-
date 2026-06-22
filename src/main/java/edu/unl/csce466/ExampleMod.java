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
    
    // Safe static reference to Minecraft instance, set from mixin to avoid TLauncher remap issues
    public static Minecraft MINECRAFT;

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
        if (MINECRAFT == null) return;
        if (MINECRAFT.player == null) return;
        if (MINECRAFT.screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_L && event.getAction() == GLFW.GLFW_PRESS) {
            MINECRAFT.setScreen(ImGuiScreen.getInstance());
        }
    }
}
