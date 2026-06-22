package edu.unl.csce466;

import com.mojang.logging.LogUtils;
import edu.unl.csce466.event.ModEvents;
import edu.unl.csce466.screens.ImGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F)));

    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
            () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final ImGuiScreen IMGUI_SCREEN = ImGuiScreen.getInstance();

    @SuppressWarnings("removal")
    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(IMGUI_SCREEN);
        modEventBus.addListener(this::addCreative);
    }

    public static class Zeus {
        public void Init() {
            System.out.println("Zeus Activated");
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.literal("You feel a surge of electricity course through your veins..."), false);
            }
            ModEvents.ForgeEvents.start = true;
        }

        public void LevelUp() {
            Player player = Minecraft.getInstance().player;
            if (player != null) player.giveExperienceLevels(50);
        }

        public void Health() {
            Player player = Minecraft.getInstance().player;
            if (player != null) player.setAbsorptionAmount(100);
        }

        public void GiveDiamonds() {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                ItemStack i = new ItemStack(Items.DIAMOND, 64);
                ItemHandlerHelper.giveItemToPlayer(player, i);
            }
        }

        public void Stick() {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                ItemStack i = new ItemStack(Items.STICK, 1);
                i.enchant(Enchantments.SHARPNESS, 100);
                ItemHandlerHelper.giveItemToPlayer(player, i);
            }
        }
    }

    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            IMGUI_SCREEN.init();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().player == null) return;
        if (Minecraft.getInstance().screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_L) {
            LOGGER.info("L key pressed - opening ImGui");
            Minecraft.getInstance().setScreen(IMGUI_SCREEN);
        }
    }
}