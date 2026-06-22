package xyz.breadloaf.imguimc.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.breadloaf.imguimc.WindowScaling;

import java.util.Map;

@Mixin(PostPass.class)
public class PostPassMixin {

    @Inject(method = "addToFrame", at = @At("HEAD"), cancellable = true)
    public void addToFrame(FrameGraphBuilder frameGraphBuilder, Map<ResourceLocation, ResourceHandle<RenderTarget>> map, Matrix4f matrix4f, CallbackInfo ci) {
        if (WindowScaling.DISABLE_POST_PROCESSORS) {
            ci.cancel();
        }
    }
}
