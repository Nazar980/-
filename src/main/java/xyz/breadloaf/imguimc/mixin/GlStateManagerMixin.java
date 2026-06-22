package xyz.breadloaf.imguimc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.GlStateManager;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import xyz.breadloaf.imguimc.WindowScaling;

@Mixin(value = GlStateManager.class, remap = false)
public class GlStateManagerMixin {

    @WrapMethod(method = "_viewport")
    private static void viewport(int x, int y, int width, int height, Operation<Void> original) {
        if (!WindowScaling.isChanged()) {
            original.call(x, y, width, height);
            return;
        }
        original.call(WindowScaling.X_OFFSET, WindowScaling.Y_TOP_OFFSET, WindowScaling.WIDTH, WindowScaling.HEIGHT);
    }

    @WrapMethod(method = "_scissorBox")
    private static void scissorBox(int x, int y, int width, int height, Operation<Void> original) {
        Vector2d xy = WindowScaling.scalePoint(x, y);
        Vector2d wh = WindowScaling.scaleWidthHeight(width, height);
        original.call((int) xy.x, (int) xy.y, (int) wh.x, (int) wh.y);
    }
}
