package edu.unl.csce466;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Stored by MinecraftMixin at init time — guaranteed correct mapping
    public static Object mcInstance = null;

    public ExampleMod() {
        LOGGER.info("Example Mod loaded for Minecraft 1.21.4");
    }
}
