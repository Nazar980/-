package edu.unl.csce466.mixins;

/**
 * Interface injected into Minecraft class by MinecraftMixin.
 * Allows calling toggleScreen() without any SRG mapping issues.
 */
public interface IMinecraftAccessor {
    void examplemod$toggleScreen();
}
