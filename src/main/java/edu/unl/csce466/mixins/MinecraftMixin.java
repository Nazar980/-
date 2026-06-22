package edu.unl.csce466.mixins;

import edu.unl.csce466.ExampleMod;
import edu.unl.csce466.client.ModMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * All Minecraft field/method access happens HERE, inside the mixin.
 * The mixin processor remaps screen, setScreen etc. correctly.
 * External code calls via IMinecraftAccessor interface — no SRG issues.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements IMinecraftAccessor {

    @Shadow public Screen screen;
    @Shadow public abstract void setScreen(Screen screen);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        ExampleMod.mcInstance = this;
    }

    @Override
    public void examplemod$toggleScreen() {
        if (this.screen instanceof ModMenuScreen) {
            this.setScreen(null);
        } else if (this.screen == null) {
            this.setScreen(new ModMenuScreen());
        }
    }
}
