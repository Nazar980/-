package edu.unl.csce466.event;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.imgui.ImGuiRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Обработка клавиш через стандартное Forge-событие InputEvent.Key.
 *
 * Раньше тут использовался миксин KeyboardHandlerMixin, который крашил игру,
 * потому что не мог найти целевой метод в классе Keyboard. Использование
 * Forge-события полностью избегает этой проблемы — никаких хрупких инъекций.
 *
 * Клавиша L — открыть/закрыть меню ImGui.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Открываем/закрываем меню по нажатию L
        if (event.getKey() == GLFW.GLFW_KEY_L && event.getAction() == GLFW.GLFW_PRESS) {
            ImGuiRenderer.getInstance().toggleMenu();
        }
    }
}
