package xyz.breadloaf.imguimc.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import xyz.breadloaf.imguimc.config.ImguimcConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomationController {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3}(?:,\\d{3})+|\\d+)(?!\\d)");
    private static final AutomationController INSTANCE = new AutomationController();

    private final ImguimcConfig config = ImguimcConfig.get();

    private static final int AUCTION_PURCHASE_DELAY_TICKS = 200;
    
    private int maxActiveSales = 5; 
    private int currentActiveSales = 0;

    private boolean enabled = false;
    private boolean waitingForBind = false;
    private Stage stage = Stage.IDLE;
    private String status = "Disabled";
    private int cooldownTicks = 0;
    private int stageTicks = 0;
    private String lastCraftedPickaxeName = "";
    private int observedAuctionContainerId = -1;
    private int observedAuctionMenuTicks = 0;
    
    // Переменные быстрого крафта
    private int craftingPlacementPhase = 0;
    private int craftingSourceSlot = -1;
    private int craftingTargetSlot = -1;
    private Item craftingExpectedItem = Items.AIR;

    public static AutomationController get() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        if (client == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (!enabled) return;

        if (client.player == null || client.gameMode == null || client.level == null) {
            setStage(Stage.IDLE, "Waiting for world", 10);
            return;
        }

        stageTicks++;

        switch (stage) {
            case IDLE, EVALUATE -> evaluateNextStage(client);
            case SEARCH_WOOD -> sendCommand(client, "ah search дерево", Stage.WAIT_WOOD_SCREEN, "Searching wood logs", 14);
            case WAIT_WOOD_SCREEN -> waitForWoodAuctionScreen(client);
            case BUY_WOOD -> buyBestWoodAuction(client);
            
            case SEARCH_EMERALDS -> sendCommand(client, "ah search изумруд", Stage.WAIT_EMERALDS_SCREEN, "Searching emeralds", 14);
            case WAIT_EMERALDS_SCREEN -> waitForAuctionScreen(client, Items.EMERALD, config.emeraldMaxCost, Stage.BUY_EMERALDS, Stage.SEARCH_EMERALDS, "Waiting emerald auction", "Retrying emerald search");
            case BUY_EMERALDS -> buyBestAuctionItem(client, Items.EMERALD, config.emeraldMaxCost, Stage.EVALUATE, Stage.SEARCH_EMERALDS, "Buying emeralds", "No valid emeralds found");
            
            case OPEN_CRAFTING -> openCraftingTable(client);
            case WAIT_CRAFTING -> waitForCraftingMenu(client);
            case CRAFT_PICKAXE -> handleCraftingCycle(client);
            
            case EQUIP_PICKAXE -> equipEmeraldPickaxe(client);
            case SELL_PICKAXE -> sellEmeraldPickaxe(client);
            
            case OPEN_AH_EXPIRED -> sendCommand(client, "ah", Stage.WAIT_AH_MAIN, "Opening AH main menu", 12);
            case WAIT_AH_MAIN -> waitForAhMainMenu(client);
            case CLICK_ENDER_CHEST -> clickEnderChestSlot(client);
            case WAIT_STORAGE_MENU -> waitForStorageMenu(client);
            case COLLECT_EXPIRED -> collectExpiredItems(client);
        }
    }

    public boolean handleKeyPress(int key, int action) {
        if (action != GLFW.GLFW_PRESS) return false;

        if (waitingForBind) {
            config.toggleAutomationKey = key;
            ImguimcConfig.save();
            waitingForBind = false;
            status = "Bind changed to " + getToggleBindName();
            return true;
        }

        if (key == config.toggleAutomationKey) {
            toggleEnabled();
            return true;
        }

        return false;
    }

    public void toggleEnabled() {
        enabled = !enabled;
        if (enabled) {
            setStage(Stage.EVALUATE, "Automation enabled", 2);
        } else {
            setStage(Stage.IDLE, "Automation disabled", 0);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getStatus() {
        return status;
    }

    public void beginBindCapture() {
        waitingForBind = true;
        status = "Press a key for toggle bind";
    }

    public boolean isWaitingForBind() {
        return waitingForBind;
    }

    public String getToggleBindName() {
        return keyName(config.toggleAutomationKey);
    }

    private void evaluateNextStage(Minecraft client) {
        if (currentActiveSales >= maxActiveSales) {
            setStage(Stage.OPEN_AH_EXPIRED, "Sales limit reached. Clearing expired items", 3);
            return;
        }

        if (hasEmeraldPickaxe(client)) {
            setStage(Stage.EQUIP_PICKAXE, "Preparing to sell emerald pickaxe", 1);
            return;
        }

        int sticks = countInventoryItem(client, Items.STICK);
        int planks = countAnyPlanks(client);
        int logs = countAnyLogs(client);

        if (sticks < 2) {
            if (logs > 0 || planks > 0) {
                setStage(Stage.OPEN_CRAFTING, "Have logs/planks, moving to processing", 1);
                return;
            }
            setStage(Stage.SEARCH_WOOD, "Need wood logs", 2);
            return;
        }

        if (countInventoryItem(client, Items.EMERALD) < 3) {
            setStage(Stage.SEARCH_EMERALDS, "Need emeralds", 2);
            return;
        }

        setStage(Stage.OPEN_CRAFTING, "Ready to craft emerald pickaxe", 1);
    }

    private void sendCommand(Minecraft client, String command, Stage nextStage, String nextStatus, int delay) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        client.player.connection.sendCommand(normalized);
        setStage(nextStage, nextStatus, delay);
    }

    private void waitForWoodAuctionScreen(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);

        if (menu != client.player.inventoryMenu && upperSlots > 10) {
            if (observedAuctionContainerId != menu.containerId) {
                observedAuctionContainerId = menu.containerId;
                observedAuctionMenuTicks = 0;
            } else {
                observedAuctionMenuTicks++;
            }
        }

        AuctionCandidate candidate = findBestWoodCandidate(client);
        if (candidate != null) {
            if (observedAuctionMenuTicks >= AUCTION_PURCHASE_DELAY_TICKS) {
                setStage(Stage.BUY_WOOD, "Wood auction found best listing", 1);
                return;
            }
            int secondsLeft = Math.max(1, (AUCTION_PURCHASE_DELAY_TICKS - observedAuctionMenuTicks + 19) / 20);
            status = "Waiting anti-buy delay (Wood): " + secondsLeft + "s";
            return;
        }

        if (stageTicks > 40) {
            setStage(Stage.SEARCH_WOOD, "Retrying wood search", 10);
            return;
        }
        status = "Waiting wood auction";
    }

    private void buyBestWoodAuction(Minecraft client) {
        AuctionCandidate candidate = findBestWoodCandidate(client);
        if (candidate == null) {
            setStage(Stage.SEARCH_WOOD, "No valid wood found, retrying", 5);
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        client.gameMode.handleInventoryMouseClick(menu.containerId, candidate.slotId, 0, ClickType.QUICK_MOVE, client.player);
        setStage(Stage.EVALUATE, "Buying logs | per unit: " + (long) candidate.pricePerItem, 6);
    }

    private void waitForAuctionScreen(Minecraft client, Item item, int maxCostPerItem, Stage successStage, Stage retryStage, String waitingStatus, String retryStatus) {
        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);

        if (menu != client.player.inventoryMenu && upperSlots > 10) {
            if (observedAuctionContainerId != menu.containerId) {
                observedAuctionContainerId = menu.containerId;
                observedAuctionMenuTicks = 0;
            } else {
                observedAuctionMenuTicks++;
            }
        }

        AuctionCandidate candidate = findBestAuctionCandidate(client, item, maxCostPerItem);
        if (candidate != null) {
            if (observedAuctionMenuTicks >= AUCTION_PURCHASE_DELAY_TICKS) {
                setStage(successStage, waitingStatus + " found best listing", 1);
                return;
            }
            int secondsLeft = Math.max(1, (AUCTION_PURCHASE_DELAY_TICKS - observedAuctionMenuTicks + 19) / 20);
            status = "Waiting anti-buy delay: " + secondsLeft + "s";
            return;
        }

        if (stageTicks > 40) {
            setStage(retryStage, retryStatus, 10);
            return;
        }
        status = waitingStatus;
    }

    private void buyBestAuctionItem(Minecraft client, Item item, int maxCostPerItem, Stage successStage, Stage failStage, String successStatus, String failStatus) {
        AuctionCandidate candidate = findBestAuctionCandidate(client, item, maxCostPerItem);
        if (candidate == null) {
            setStage(failStage, failStatus, 5);
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        client.gameMode.handleInventoryMouseClick(menu.containerId, candidate.slotId, 0, ClickType.QUICK_MOVE, client.player);
        setStage(successStage, successStatus + " | per item: " + (long) candidate.pricePerItem, 6);
    }

    private void openCraftingTable(Minecraft client) {
        if (client.player.containerMenu instanceof CraftingMenu) {
            setStage(Stage.CRAFT_PICKAXE, "Crafting table opened", 1);
            return;
        }

        if (client.player.containerMenu != client.player.inventoryMenu) {
            client.player.closeContainer();
            status = "Closing container before crafting";
            cooldownTicks = 3;
            return;
        }

        if (!(client.hitResult instanceof BlockHitResult blockHitResult)) {
            status = "Look directly at a crafting table";
            return;
        }

        if (client.level.getBlockState(blockHitResult.getBlockPos()).getBlock() != Blocks.CRAFTING_TABLE) {
            status = "Look directly at a crafting table";
            return;
        }

        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHitResult);
        setStage(Stage.WAIT_CRAFTING, "Opening crafting table", 4);
    }

    private void waitForCraftingMenu(Minecraft client) {
        if (client.player.containerMenu instanceof CraftingMenu) {
            setStage(Stage.CRAFT_PICKAXE, "Crafting table ready", 1);
            return;
        }

        if (stageTicks > 30) {
            setStage(Stage.OPEN_CRAFTING, "Retry crafting table", 4);
            return;
        }
        status = "Waiting crafting table";
    }

private void handleCraftingCycle(Minecraft client) {
        if (!(client.player.containerMenu instanceof CraftingMenu menu)) {
            resetCraftingPlacement();
            setStage(Stage.OPEN_CRAFTING, "Crafting menu closed", 2);
            return;
        }

        // Если мы в процессе поштучной раскладки КИРКИ — продолжаем её
        if (craftingPlacementPhase != 0) {
            continueCraftPlacement(client, menu);
            return;
        }

        int sticks = countInventoryItem(client, Items.STICK);

        // ШАГ 1: ПЕРЕРАБОТКА БРЁВЕН В ДОСКИ (Через Shift-клик)
        int logSlot = findAnyLogSlot(menu);
        if (logSlot != -1 && sticks < 2) {
            if (!menu.slots.get(0).getItem().isEmpty()) {
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                status = "Collecting remaining items from result slot";
                cooldownTicks = 3;
                return;
            }
            if (!menu.getCarried().isEmpty()) {
                returnCarriedStackToInventory(client, menu);
                cooldownTicks = 2;
                return;
            }
            client.gameMode.handleInventoryMouseClick(menu.containerId, logSlot, 0, ClickType.QUICK_MOVE, client.player);
            status = "Shift-clicking logs into crafting grid";
            cooldownTicks = 4;
            return;
        }

// ШАГ 2: МАССОВЫЙ КРАФТ ПАЛОК (Поочередная выгрузка двух разных стаков через ЛКМ)
        int plankSlot = findAnyPlankSlot(menu);
        if (plankSlot != -1 && sticks < 2) {
            ItemStack slot1Item = menu.slots.get(1).getItem();
            ItemStack slot4Item = menu.slots.get(4).getItem();
            
            boolean slot1Ok = slot1Item.is(net.minecraft.tags.ItemTags.PLANKS);
            boolean slot4Ok = slot4Item.is(net.minecraft.tags.ItemTags.PLANKS);
            
            if (!slot1Ok || !slot4Ok) {
                ItemStack carried = menu.getCarried();
                
                // Если в руке ничего нет — берем стак досок из инвентаря ЛКМ (button = 0)
                if (carried.isEmpty() || !carried.is(net.minecraft.tags.ItemTags.PLANKS)) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, plankSlot, 0, ClickType.PICKUP, client.player);
                    status = "Picking up planks stack with LMB";
                    cooldownTicks = 2;
                    return;
                }
                
                // Если стак в руке и слот 1 еще пустой — отдаем весь стак туда через ЛКМ (button = 0)
                if (!slot1Ok) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.PICKUP, client.player);
                    status = "Placing entire first stack into slot 1 via LMB";
                    cooldownTicks = 2;
                    return;
                }
                
                // Если слот 1 уже заполнен, а слот 4 пустой — отдаем весь удерживаемый (второй) стак туда через ЛКМ (button = 0)
                if (!slot4Ok) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 4, 0, ClickType.PICKUP, client.player);
                    status = "Placing entire second stack into slot 4 via LMB";
                    cooldownTicks = 2;
                    return;
                }
            }
        }

        // Если палок всё ещё мало, но доски уже разложены в сетке — курсор должен быть пустой, чтобы забрать результат
        if (sticks < 2 && !menu.getCarried().isEmpty()) {
            returnCarriedStackToInventory(client, menu);
            status = "Clearing cursor before collecting sticks";
            cooldownTicks = 3;
            return;
        }

        // ШАГ 3: СБОРКА КИРКИ (Строго когда палок уже хватает!)
        int nextTargetSlot = findNextMissingRecipeSlot(menu);
        if (nextTargetSlot != -1 && sticks >= 2) {
            // Очищаем сетку от возможных остатков досок в слотах 1 и 4 перед выкладыванием кирки
            if (menu.slots.get(1).getItem().is(net.minecraft.tags.ItemTags.PLANKS) || menu.slots.get(4).getItem().is(net.minecraft.tags.ItemTags.PLANKS)) {
                ItemStack res = menu.slots.get(0).getItem();
                if (!res.isEmpty() && !isTargetPickaxe(res)) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                } else {
                    if (!menu.slots.get(1).getItem().isEmpty()) client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.QUICK_MOVE, client.player);
                    if (!menu.slots.get(4).getItem().isEmpty()) client.gameMode.handleInventoryMouseClick(menu.containerId, 4, 0, ClickType.QUICK_MOVE, client.player);
                }
                cooldownTicks = 3;
                return;
            }

            if (!menu.getCarried().isEmpty()) {
                returnCarriedStackToInventory(client, menu);
                cooldownTicks = 2;
                return;
            }

            Item expectedItem = expectedRecipeItemForSlot(nextTargetSlot);
            int sourceSlot = findInventorySlot(menu, expectedItem);
            if (sourceSlot == -1) {
                setStage(Stage.EVALUATE, "Missing ingredient for pickaxe slot " + nextTargetSlot, 2);
                return;
            }

            client.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, 0, ClickType.PICKUP, client.player);
            craftingPlacementPhase = 1;
            craftingSourceSlot = sourceSlot;
            craftingTargetSlot = nextTargetSlot;
            craftingExpectedItem = expectedItem;
            status = "Placing pickaxe component into slot " + nextTargetSlot;
            cooldownTicks = 2;
            return;
        }

        // ЗАБИРАЕМ ГОТОВЫЙ ПРЕДМЕТ ИЗ СЛОТА РЕЗУЛЬТАТА (0)
        ItemStack result = menu.slots.get(0).getItem();
        if (!result.isEmpty()) {
            if (isTargetPickaxe(result)) {
                lastCraftedPickaxeName = normalizedItemName(result);
                resetCraftingPlacement();
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                setStage(Stage.EQUIP_PICKAXE, "Crafted: " + lastCraftedPickaxeName, 3);
            } else {
                resetCraftingPlacement();
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                status = "Collected sub-resource from result slot";
                cooldownTicks = 3;
            }
        }
    }

    private void continueCraftPlacement(Minecraft client, CraftingMenu menu) {
        if (craftingPlacementPhase == 1) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty() || carried.getItem() != craftingExpectedItem) {
                resetCraftingPlacement();
                status = "Failed to secure component";
                cooldownTicks = 2;
                return;
            }

            client.gameMode.handleInventoryMouseClick(menu.containerId, craftingTargetSlot, 1, ClickType.PICKUP, client.player);
            craftingPlacementPhase = 2;
            cooldownTicks = 1;
            return;
        }

        if (craftingPlacementPhase == 2) {
            int nextTargetSlot = findNextMissingRecipeSlot(menu);
            if (nextTargetSlot != -1 && expectedRecipeItemForSlot(nextTargetSlot) == craftingExpectedItem) {
                craftingTargetSlot = nextTargetSlot;
                client.gameMode.handleInventoryMouseClick(menu.containerId, craftingTargetSlot, 1, ClickType.PICKUP, client.player);
                cooldownTicks = 1;
                return;
            }

            if (!menu.getCarried().isEmpty()) {
                client.gameMode.handleInventoryMouseClick(menu.containerId, craftingSourceSlot, 0, ClickType.PICKUP, client.player);
                craftingPlacementPhase = 3;
                cooldownTicks = 1;
                return;
            }

            resetCraftingPlacement();
            cooldownTicks = 1;
            return;
        }

        if (craftingPlacementPhase == 3) {
            resetCraftingPlacement();
            cooldownTicks = 1;
        }
    }

    private int findNextMissingRecipeSlot(CraftingMenu menu) {
        int[] targetSlots = {1, 2, 3, 5, 8};
        for (int targetSlot : targetSlots) {
            Item expectedItem = expectedRecipeItemForSlot(targetSlot);
            ItemStack targetStack = menu.slots.get(targetSlot).getItem();
            if (targetStack.isEmpty() || targetStack.getItem() != expectedItem) {
                return targetSlot;
            }
        }
        return -1;
    }

    private Item expectedRecipeItemForSlot(int slot) {
        return switch (slot) {
            case 1, 2, 3 -> Items.EMERALD;
            case 5, 8 -> Items.STICK;
            default -> Items.AIR;
        };
    }

    private void returnCarriedStackToInventory(Minecraft client, AbstractContainerMenu menu) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            resetCraftingPlacement();
            return;
        }
        int targetSlot = findMergeableInventorySlot(menu, carried.getItem());
        if (targetSlot == -1) targetSlot = findEmptyInventorySlot(menu);
        if (targetSlot != -1) {
            client.gameMode.handleInventoryMouseClick(menu.containerId, targetSlot, 0, ClickType.PICKUP, client.player);
        }
        resetCraftingPlacement();
    }

    private void resetCraftingPlacement() {
        craftingPlacementPhase = 0;
        craftingSourceSlot = -1;
        craftingTargetSlot = -1;
        craftingExpectedItem = Items.AIR;
    }

    private void equipEmeraldPickaxe(Minecraft client) {
        if (isTargetPickaxe(client.player.getMainHandItem())) {
            setStage(Stage.SELL_PICKAXE, "Target pickaxe equipped", 1);
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        int pickaxeSlot = findTargetPickaxeSlot(menu);
        if (pickaxeSlot == -1) {
            setStage(Stage.EVALUATE, "No crafted pickaxe to equip", 2);
            return;
        }

        int selectedHotbarSlot = client.player.getInventory().selected;
        client.gameMode.handleInventoryMouseClick(menu.containerId, pickaxeSlot, selectedHotbarSlot, ClickType.SWAP, client.player);
        setStage(Stage.SELL_PICKAXE, "Equipping target pickaxe", 3);
    }

    private void sellEmeraldPickaxe(Minecraft client) {
        if (!isTargetPickaxe(client.player.getMainHandItem())) {
            setStage(Stage.EQUIP_PICKAXE, "Target pickaxe is not in hand", 2);
            return;
        }

        if (client.player.containerMenu != client.player.inventoryMenu) {
            client.player.closeContainer();
            status = "Closing container before selling";
            cooldownTicks = 3;
            return;
        }

        if (config.emeraldPickaxeCost <= 0) {
            status = "Emerald Pickaxe Cost must be greater than 0";
            enabled = false;
            stage = Stage.IDLE;
            return;
        }

        client.player.connection.sendCommand("ah sell " + config.emeraldPickaxeCost);
        currentActiveSales++; 
        setStage(Stage.EVALUATE, "Selling emerald pickaxe for " + config.emeraldPickaxeCost, 15);
    }

    private void waitForAhMainMenu(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);

        if (menu != client.player.inventoryMenu && upperSlots > 10) {
            setStage(Stage.CLICK_ENDER_CHEST, "AH Main Menu loaded, searching for storage button", 2);
        } else if (stageTicks > 40) {
            setStage(Stage.OPEN_AH_EXPIRED, "AH timeout, retrying command", 10);
        }
    }

    private void clickEnderChestSlot(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            setStage(Stage.OPEN_AH_EXPIRED, "Menu closed unexpectedly, restarting", 5);
            return;
        }

        int upperSlots = Math.max(0, menu.slots.size() - 36);
        int targetEnderChestSlot = -1;

        for (int i = 0; i < upperSlots; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_CHEST) {
                targetEnderChestSlot = i;
                break;
            }
        }

        if (targetEnderChestSlot != -1) {
            client.gameMode.handleInventoryMouseClick(menu.containerId, targetEnderChestSlot, 0, ClickType.PICKUP, client.player);
            setStage(Stage.WAIT_STORAGE_MENU, "Clicked Ender Chest at slot " + targetEnderChestSlot + ", waiting storage", 6);
        } else {
            if (stageTicks > 20) {
                client.player.closeContainer();
                setStage(Stage.EVALUATE, "Ender chest button not found in AH menu, retrying cycle", 5);
            }
            status = "Searching for Ender Chest button...";
        }
    }

    private void waitForStorageMenu(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            setStage(Stage.OPEN_AH_EXPIRED, "Storage closed, reopening", 5);
            return;
        }
        setStage(Stage.COLLECT_EXPIRED, "Storage menu ready, start collecting", 4);
    }

    private void collectExpiredItems(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            setStage(Stage.EVALUATE, "Menu closed during collection, evaluation state", 3);
            return;
        }

        int upperSlots = Math.max(0, menu.slots.size() - 36);
        boolean foundItem = false;

        for (int i = 0; i < upperSlots; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty() && isTargetPickaxe(stack)) {
                client.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, client.player);
                foundItem = true;
                cooldownTicks = 4;
                status = "Collecting expired item from slot " + i;
                break;
            }
        }

        if (!foundItem && stageTicks > 15) {
            client.player.closeContainer();
            currentActiveSales = 0;
            setStage(Stage.EVALUATE, "All expired items collected, restarting cycle", 5);
        }
    }

    private AuctionCandidate findBestWoodCandidate(Minecraft client) {
        if (client.player == null || client.screen == null) return null;

        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);
        if (menu == client.player.inventoryMenu || upperSlots <= 10) return null;

        int scanLimit = Math.min(upperSlots, Math.max(1, config.howManySlots));
        AuctionCandidate best = null;

        for (int slotId = 0; slotId < scanLimit; slotId++) {
            Slot slot = menu.slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            boolean isLog = stack.is(net.minecraft.tags.ItemTags.LOGS);
            if (!isLog) continue;

            long totalPrice = extractPriceFromLore(stack);
            if (totalPrice <= 0) continue;

            double pricePerItem = totalPrice / (double) Math.max(1, stack.getCount());
            if (config.woodMaxCost > 0 && pricePerItem > config.woodMaxCost) continue;

            if (best == null || pricePerItem < best.pricePerItem || (pricePerItem == best.pricePerItem && stack.getCount() > best.stackCount)) {
                best = new AuctionCandidate(slotId, totalPrice, pricePerItem, stack.getCount());
            }
        }
        return best;
    }

    private AuctionCandidate findBestAuctionCandidate(Minecraft client, Item item, int maxCostPerItem) {
        if (client.player == null || client.screen == null) return null;

        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);
        if (menu == client.player.inventoryMenu || upperSlots <= 10) return null;

        int scanLimit = Math.min(upperSlots, Math.max(1, config.howManySlots));
        AuctionCandidate best = null;

        for (int slotId = 0; slotId < scanLimit; slotId++) {
            Slot slot = menu.slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || stack.getItem() != item) continue;

            long totalPrice = extractPriceFromLore(stack);
            if (totalPrice <= 0) continue;

            double pricePerItem = totalPrice / (double) Math.max(1, stack.getCount());
            if (maxCostPerItem > 0 && pricePerItem > maxCostPerItem) continue;

            if (best == null || pricePerItem < best.pricePerItem || (pricePerItem == best.pricePerItem && stack.getCount() > best.stackCount)) {
                best = new AuctionCandidate(slotId, totalPrice, pricePerItem, stack.getCount());
            }
        }
        return best;
    }

    private long extractPriceFromLore(ItemStack stack) {
        List<String> lines = collectRelevantLines(stack);
        long keywordPrice = -1;
        long fallbackPrice = -1;

        for (String originalLine : lines) {
            String line = originalLine.toLowerCase(Locale.ROOT);
            if (isTimeLine(line)) continue;

            List<Long> numbers = extractNumbers(line);
            if (numbers.isEmpty()) continue;

            long candidate = numbers.stream().mapToLong(Long::longValue).max().orElse(-1);
            if (candidate <= 0) continue;

            if (containsPriceKeyword(line)) {
                keywordPrice = Math.max(keywordPrice, candidate);
            } else if (!containsCountKeyword(line)) {
                fallbackPrice = Math.max(fallbackPrice, candidate);
            }
        }
        return keywordPrice > 0 ? keywordPrice : fallbackPrice;
    }

    private List<String> collectRelevantLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                lines.add(line.getString());
            }
        }
        lines.add(stack.getHoverName().getString());
        return lines;
    }

    private List<Long> extractNumbers(String line) {
        List<Long> values = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(line);
        while (matcher.find()) {
            String value = matcher.group(1).replace(",", "");
            try {
                values.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {}
        }
        return values;
    }

    private boolean isTimeLine(String line) {
        return line.contains("остал") || line.contains("пропад") || line.contains("expire")
                || line.contains("remaining") || line.contains("сек") || line.contains("мин")
                || line.contains("час") || line.contains("дн") || line.contains("day")
                || line.contains("hour") || line.contains("minute") || line.contains("second");
    }

    private boolean containsPriceKeyword(String line) {
        return line.contains("цен") || line.contains("стоим") || line.contains("монет")
                || line.contains("coins") || line.contains("price") || line.contains("cost") || line.contains("$");
    }

    private boolean containsCountKeyword(String line) {
        return line.contains("шт") || line.contains("stack") || line.contains("count")
                || line.contains("колич") || line.contains("amount") || line.contains("x");
    }

    private int countInventoryItem(Minecraft client, Item item) {
        int count = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countAnyLogs(Minecraft client) {
        int count = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.LOGS)) {
                count += stack.getCount();
            }
        }
        if (client.player.containerMenu instanceof CraftingMenu menu) {
            for (int i = 1; i <= 9; i++) {
                ItemStack stack = menu.slots.get(i).getItem();
                if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.LOGS)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private int countAnyPlanks(Minecraft client) {
        int count = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PLANKS)) {
                count += stack.getCount();
            }
        }
        if (client.player.containerMenu instanceof CraftingMenu menu) {
            for (int i = 1; i <= 9; i++) {
                ItemStack stack = menu.slots.get(i).getItem();
                if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PLANKS)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private int findAnyLogSlot(AbstractContainerMenu menu) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.LOGS)) return slotId;
        }
        return -1;
    }

    private int findAnyPlankSlot(AbstractContainerMenu menu) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PLANKS)) return slotId;
        }
        return -1;
    }

    private boolean hasEmeraldPickaxe(Minecraft client) {
        if (isTargetPickaxe(client.player.getMainHandItem())) return true;
        for (ItemStack stack : client.player.getInventory().items) {
            if (isTargetPickaxe(stack)) return true;
        }
        return false;
    }

    private int findTargetPickaxeSlot(AbstractContainerMenu menu) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (isTargetPickaxe(stack)) return slotId;
        }
        return -1;
    }

    private boolean isTargetPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String normalizedName = normalizedItemName(stack);
        if (!lastCraftedPickaxeName.isEmpty() && normalizedName.equals(lastCraftedPickaxeName)) return true;

        String descriptionId = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return descriptionId.contains("pickaxe") || normalizedName.contains("кирка") || normalizedName.contains("pickaxe");
    }

    private String normalizedItemName(ItemStack stack) {
        return stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
    }

    private int findInventorySlot(AbstractContainerMenu menu, Item item) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) return slotId;
        }
        return -1;
    }

    private int findMergeableInventorySlot(AbstractContainerMenu menu, Item item) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) return slotId;
        }
        return -1;
    }

    private int findEmptyInventorySlot(AbstractContainerMenu menu) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            if (menu.slots.get(slotId).getItem().isEmpty()) return slotId;
        }
        return -1;
    }

    private void setStage(Stage newStage, String newStatus, int delayTicks) {
        if (newStage == Stage.SEARCH_WOOD || newStage == Stage.SEARCH_EMERALDS || newStage == Stage.IDLE || newStage == Stage.EVALUATE) {
            observedAuctionContainerId = -1;
            observedAuctionMenuTicks = 0;
        }
        if (newStage != Stage.CRAFT_PICKAXE) {
            resetCraftingPlacement();
        }
        stage = newStage;
        status = newStatus;
        cooldownTicks = delayTicks;
        stageTicks = 0;
    }

    private String keyName(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_UNKNOWN -> "UNKNOWN";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            default -> {
                String glfwName = GLFW.glfwGetKeyName(key, 0);
                yield glfwName == null ? "KEY_" + key : glfwName.toUpperCase(Locale.ROOT);
            }
        };
    }

    private enum Stage {
        IDLE,
        EVALUATE,
        SEARCH_WOOD,
        WAIT_WOOD_SCREEN,
        BUY_WOOD,
        SEARCH_EMERALDS,
        WAIT_EMERALDS_SCREEN,
        BUY_EMERALDS,
        OPEN_CRAFTING,
        WAIT_CRAFTING,
        CRAFT_PICKAXE,
        EQUIP_PICKAXE,
        SELL_PICKAXE,
        OPEN_AH_EXPIRED,
        WAIT_AH_MAIN,
        CLICK_ENDER_CHEST,
        WAIT_STORAGE_MENU,
        COLLECT_EXPIRED
    }

    private record AuctionCandidate(int slotId, long totalPrice, double pricePerItem, int stackCount) {
    }
}
