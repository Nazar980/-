package xyz.breadloaf.imguimc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import xyz.breadloaf.imguimc.Imguimc;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImguimcConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("imguimc.json");

    private static ImguimcConfig instance;

    public int howManySlots = 1;
    public int emeraldMaxCost = 0;
    public int woodMaxCost = 0;

    public static ImguimcConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, ImguimcConfig.class);
            } catch (Exception exception) {
                Imguimc.LOGGER.error("Failed to load config from {}", CONFIG_PATH, exception);
            }
        }

        if (instance == null) {
            instance = new ImguimcConfig();
        }

        instance.clamp();
        save();
    }

    public static void save() {
        ImguimcConfig config = get();
        config.clamp();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception exception) {
            Imguimc.LOGGER.error("Failed to save config to {}", CONFIG_PATH, exception);
        }
    }

    private void clamp() {
        howManySlots = clampInt(howManySlots, 1, 99);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
