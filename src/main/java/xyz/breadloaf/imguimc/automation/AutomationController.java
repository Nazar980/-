package xyz.breadloaf.imguimc.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
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

    private static final int[] EMERALD_PICKAXE_EMERALD_SLOTS = {1, 2, 3};
    private static final int[] EMERALD_PICKAXE_STICK_SLOTS = {5, 8};

    private final ImguimcConfig config = ImguimcConfig.get();

    private boolean enabled = false;
    private boolean waitingForBind = false;
    private Stage stage = Stage.IDLE;
    private String status = "Disabled";
    private int cooldownTicks = 0;
    private int stageTicks = 0;
    private String lastCraftedPickaxeName = "";

    public static AutomationController get() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        if (client == null) {
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (!enabled) {
            return;
        }

        if (client.player == null || client.gameMode == null || client.level == null) {
            setStage(Stage.IDLE, "Waiting for world", 10);
            return;
        }

        stageTicks++;

        switch (stage) {
            case IDLE, EVALUATE -> evaluateNextStage(client);
            case SEARCH_STICKS -> sendCommand(client, "ah search Палка", Stage.WAIT_STICKS_SCREEN, "Searching sticks", 14);
            case WAIT_STICKS_SCREEN -> waitForAuctionScreen(client, Items.STICK, config.woodMaxCost, Stage.BUY_STICKS, Stage.SEARCH_STICKS, "Waiting stick auction", "Retrying stick search");
            case BUY_STICKS -> buyBestAuctionItem(client, Items.STICK, config.woodMaxCost, Stage.EVALUATE, Stage.SEARCH_STICKS, "Buying sticks", "No valid sticks found");
            case SEARCH_EMERALDS -> sendCommand(client, "ah search изумруд", Stage.WAIT_EMERALDS_SCREEN, "Searching emeralds", 14);
            case WAIT_EMERALDS_SCREEN -> waitForAuctionScreen(client, Items.EMERALD, config.emeraldMaxCost, Stage.BUY_EMERALDS, Stage.SEARCH_EMERALDS, "Waiting emerald auction", "Retrying emerald search");
            case BUY_EMERALDS -> buyBestAuctionItem(client, Items.EMERALD, config.emeraldMaxCost, Stage.EVALUATE, Stage.SEARCH_EMERALDS, "Buying emeralds", "No valid emeralds found");
            case OPEN_CRAFTING -> openCraftingTable(client);
            case WAIT_CRAFTING -> waitForCraftingMenu(client);
            case CRAFT_PICKAXE -> craftEmeraldPickaxe(client);
            case EQUIP_PICKAXE -> equipEmeraldPickaxe(client);
            case SELL_PICKAXE -> sellEmeraldPickaxe(client);
        }
    }

    public boolean handleKeyPress(int key, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }

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
        if (hasEmeraldPickaxe(client)) {
            setStage(Stage.EQUIP_PICKAXE, "Preparing to sell emerald pickaxe", 2);
            return;
        }

        if (countInventoryItem(client, Items.STICK) < 2) {
            setStage(Stage.SEARCH_STICKS, "Need sticks", 2);
            return;
        }

        if (countInventoryItem(client, Items.EMERALD) < 3) {
            setStage(Stage.SEARCH_EMERALDS, "Need emeralds", 2);
            return;
        }

        setStage(Stage.OPEN_CRAFTING, "Ready to craft emerald pickaxe", 2);
    }

    private void sendCommand(Minecraft client, String command, Stage nextStage, String nextStatus, int delay) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        client.player.connection.sendCommand(normalized);
        setStage(nextStage, nextStatus, delay);
    }

    private void waitForAuctionScreen(Minecraft client, net.minecraft.world.item.Item item, int maxCostPerItem, Stage successStage, Stage retryStage, String waitingStatus, String retryStatus) {
        AuctionCandidate candidate = findBestAuctionCandidate(client, item, maxCostPerItem);
        if (candidate != null) {
            setStage(successStage, waitingStatus + " found best listing", 2);
            return;
        }

        if (stageTicks > 40) {
            setStage(retryStage, retryStatus, 10);
            return;
        }

        status = waitingStatus;
    }

    private void buyBestAuctionItem(Minecraft client, net.minecraft.world.item.Item item, int maxCostPerItem, Stage successStage, Stage failStage, String successStatus, String failStatus) {
        AuctionCandidate candidate = findBestAuctionCandidate(client, item, maxCostPerItem);
        if (candidate == null) {
            setStage(failStage, failStatus, 10);
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        client.gameMode.handleInventoryMouseClick(menu.containerId, candidate.slotId, 0, ClickType.QUICK_MOVE, client.player);
        setStage(successStage, successStatus + " | per item: " + (long) candidate.pricePerItem, 12);
    }

    private void openCraftingTable(Minecraft client) {
        if (client.player.containerMenu instanceof CraftingMenu) {
            setStage(Stage.CRAFT_PICKAXE, "Crafting table opened", 2);
            return;
        }

        if (client.player.containerMenu != client.player.inventoryMenu) {
            client.player.closeContainer();
            status = "Closing container before crafting";
            cooldownTicks = 6;
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
        setStage(Stage.WAIT_CRAFTING, "Opening crafting table", 8);
    }

    private void waitForCraftingMenu(Minecraft client) {
        if (client.player.containerMenu instanceof CraftingMenu) {
            setStage(Stage.CRAFT_PICKAXE, "Crafting table ready", 2);
            return;
        }

        if (stageTicks > 30) {
            setStage(Stage.OPEN_CRAFTING, "Retry crafting table", 8);
            return;
        }

        status = "Waiting crafting table";
    }

    private void craftEmeraldPickaxe(Minecraft client) {
        if (!(client.player.containerMenu instanceof CraftingMenu menu)) {
            setStage(Stage.OPEN_CRAFTING, "Crafting menu closed", 4);
            return;
        }

        if (!placeRecipeItem(client, menu, Items.EMERALD, EMERALD_PICKAXE_EMERALD_SLOTS)) {
            setStage(Stage.EVALUATE, "Not enough emeralds to craft", 6);
            return;
        }

        if (!placeRecipeItem(client, menu, Items.STICK, EMERALD_PICKAXE_STICK_SLOTS)) {
            setStage(Stage.EVALUATE, "Not enough sticks to craft", 6);
            return;
        }

        ItemStack result = menu.slots.get(0).getItem();
        if (result.isEmpty()) {
            status = "Waiting craft result";
            return;
        }

        lastCraftedPickaxeName = normalizedItemName(result);
        client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
        setStage(Stage.EQUIP_PICKAXE, "Craft result taken: " + lastCraftedPickaxeName, 8);
    }

    private boolean placeRecipeItem(Minecraft client, CraftingMenu menu, net.minecraft.world.item.Item item, int[] targetSlots) {
        for (int targetSlot : targetSlots) {
            ItemStack targetStack = menu.slots.get(targetSlot).getItem();
            if (!targetStack.isEmpty() && targetStack.getItem() == item) {
                continue;
            }

            int sourceSlot = findInventorySlot(menu, item);
            if (sourceSlot == -1) {
                return false;
            }

            client.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, 0, ClickType.PICKUP, client.player);
            client.gameMode.handleInventoryMouseClick(menu.containerId, targetSlot, 1, ClickType.PICKUP, client.player);
            client.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, 0, ClickType.PICKUP, client.player);
        }

        return true;
    }

    private void equipEmeraldPickaxe(Minecraft client) {
        if (isTargetPickaxe(client.player.getMainHandItem())) {
            setStage(Stage.SELL_PICKAXE, "Target pickaxe equipped", 2);
            return;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        int pickaxeSlot = findTargetPickaxeSlot(menu);
        if (pickaxeSlot == -1) {
            setStage(Stage.EVALUATE, "No crafted pickaxe to equip", 4);
            return;
        }

        int selectedHotbarSlot = client.player.getInventory().selected;
        client.gameMode.handleInventoryMouseClick(menu.containerId, pickaxeSlot, selectedHotbarSlot, ClickType.SWAP, client.player);
        setStage(Stage.SELL_PICKAXE, "Equipping target pickaxe", 6);
    }

    private void sellEmeraldPickaxe(Minecraft client) {
        if (!isTargetPickaxe(client.player.getMainHandItem())) {
            setStage(Stage.EQUIP_PICKAXE, "Target pickaxe is not in hand", 4);
            return;
        }

        if (client.player.containerMenu != client.player.inventoryMenu) {
            client.player.closeContainer();
            status = "Closing container before selling";
            cooldownTicks = 6;
            return;
        }

        if (config.emeraldPickaxeCost <= 0) {
            status = "Emerald Pickaxe Cost must be greater than 0";
            enabled = false;
            stage = Stage.IDLE;
            return;
        }

        client.player.connection.sendCommand("ah sell " + config.emeraldPickaxeCost);
        setStage(Stage.EVALUATE, "Selling emerald pickaxe for " + config.emeraldPickaxeCost, 20);
    }

    private AuctionCandidate findBestAuctionCandidate(Minecraft client, net.minecraft.world.item.Item item, int maxCostPerItem) {
        if (client.player == null || client.screen == null) {
            return null;
        }

        AbstractContainerMenu menu = client.player.containerMenu;
        int upperSlots = Math.max(0, menu.slots.size() - 36);
        if (menu == client.player.inventoryMenu || upperSlots <= 10) {
            return null;
        }

        int scanLimit = Math.min(upperSlots, Math.max(1, config.howManySlots));

        AuctionCandidate best = null;
        for (int slotId = 0; slotId < scanLimit; slotId++) {
            Slot slot = menu.slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }

            long totalPrice = extractPriceFromLore(stack);
            if (totalPrice <= 0) {
                continue;
            }

            double pricePerItem = totalPrice / (double) Math.max(1, stack.getCount());
            if (maxCostPerItem > 0 && pricePerItem > maxCostPerItem) {
                continue;
            }

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
            if (isTimeLine(line)) {
                continue;
            }

            List<Long> numbers = extractNumbers(line);
            if (numbers.isEmpty()) {
                continue;
            }

            long candidate = numbers.stream().mapToLong(Long::longValue).max().orElse(-1);
            if (candidate <= 0) {
                continue;
            }

            if (containsPriceKeyword(line)) {
                keywordPrice = Math.max(keywordPrice, candidate);
            } else if (!containsCountKeyword(line)) {
                fallbackPrice = Math.max(fallbackPrice, candidate);
            }
        }

        if (keywordPrice > 0) {
            return keywordPrice;
        }

        return fallbackPrice;
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
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    private boolean isTimeLine(String line) {
        return line.contains("остал")
                || line.contains("пропад")
                || line.contains("expire")
                || line.contains("remaining")
                || line.contains("сек")
                || line.contains("мин")
                || line.contains("час")
                || line.contains("дн")
                || line.contains("day")
                || line.contains("hour")
                || line.contains("minute")
                || line.contains("second");
    }

    private boolean containsPriceKeyword(String line) {
        return line.contains("цен")
                || line.contains("стоим")
                || line.contains("монет")
                || line.contains("coins")
                || line.contains("price")
                || line.contains("cost")
                || line.contains("$");
    }

    private boolean containsCountKeyword(String line) {
        return line.contains("шт")
                || line.contains("stack")
                || line.contains("count")
                || line.contains("колич")
                || line.contains("amount")
                || line.contains("x");
    }

    private int countInventoryItem(Minecraft client, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : client.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean hasEmeraldPickaxe(Minecraft client) {
        if (isTargetPickaxe(client.player.getMainHandItem())) {
            return true;
        }

        for (ItemStack stack : client.player.getInventory().items) {
            if (isTargetPickaxe(stack)) {
                return true;
            }
        }
        return false;
    }

    private int findTargetPickaxeSlot(AbstractContainerMenu menu) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (isTargetPickaxe(stack)) {
                return slotId;
            }
        }
        return -1;
    }

    private boolean isTargetPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String normalizedName = normalizedItemName(stack);
        if (!lastCraftedPickaxeName.isEmpty() && normalizedName.equals(lastCraftedPickaxeName)) {
            return true;
        }

        String descriptionId = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return descriptionId.contains("pickaxe")
                || normalizedName.contains("кирка")
                || normalizedName.contains("pickaxe");
    }

    private String normalizedItemName(ItemStack stack) {
        return stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
    }

    private int findInventorySlot(AbstractContainerMenu menu, net.minecraft.world.item.Item item) {
        int playerInventoryStart = Math.max(0, menu.slots.size() - 36);
        for (int slotId = playerInventoryStart; slotId < menu.slots.size(); slotId++) {
            ItemStack stack = menu.slots.get(slotId).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) {
                return slotId;
            }
        }
        return -1;
    }

    private void setStage(Stage newStage, String newStatus, int delayTicks) {
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
        SEARCH_STICKS,
        WAIT_STICKS_SCREEN,
        BUY_STICKS,
        SEARCH_EMERALDS,
        WAIT_EMERALDS_SCREEN,
        BUY_EMERALDS,
        OPEN_CRAFTING,
        WAIT_CRAFTING,
        CRAFT_PICKAXE,
        EQUIP_PICKAXE,
        SELL_PICKAXE
    }

    private record AuctionCandidate(int slotId, long totalPrice, double pricePerItem, int stackCount) {
    }
}
