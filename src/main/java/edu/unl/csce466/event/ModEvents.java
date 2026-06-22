package edu.unl.csce466.event;

import edu.unl.csce466.ExampleMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID)
    public static class ForgeEvents {
        public static boolean start = false;

        @SubscribeEvent
        public static void onPlayerInteract(PlayerInteractEvent event) {
            if (event instanceof PlayerInteractEvent.RightClickEmpty && start) {
                LivingEntity player = event.getEntity();
                Vec3 playerPos = player.getEyePosition(0);
                Vec3 spawnPos = new Vec3(playerPos.x + 10, playerPos.y, playerPos.z);
                System.out.println("Player looking at: " + spawnPos);
            }
        }
    }
}