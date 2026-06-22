package edu.unl.csce466.client;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.imgui.ImGuiRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class ClientRenderEvents {
    private ClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        ImGuiRenderer.getInstance().render();
    }
}