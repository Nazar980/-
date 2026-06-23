private void handleCraftingCycle(Minecraft client) {
        if (!(client.player.containerMenu instanceof CraftingMenu menu)) {
            resetCraftingPlacement();
            setStage(Stage.OPEN_CRAFTING, "Crafting menu closed", 2);
            return;
        }

        // Если в руке курсора что-то зависло — возвращаем в инвентарь
        if (!menu.getCarried().isEmpty()) {
            returnCarriedStackToInventory(client, menu);
            status = "Cleaning cursor item";
            cooldownTicks = 3;
            return;
        }

        // Если мы уже находимся в процессе поштучной раскладки КИРКИ, продолжаем её
        if (craftingPlacementPhase != 0) {
            continueCraftPlacement(client, menu);
            return;
        }

        int sticks = countInventoryItem(client, Items.STICK);

        // ШАГ 1: ПЕРЕРАБОТКА БРЁВЕН В ДОСКИ (Через Shift-клик в инвентаре)
        int logSlot = findAnyLogSlot(menu);
        if (logSlot != -1 && sticks < 2) {
            // Если в слоте результата (0) уже что-то лежит, сначала забираем это
            if (!menu.slots.get(0).getItem().isEmpty()) {
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                status = "Collecting remaining items from result slot";
                cooldownTicks = 3;
                return;
            }
            // Прожимаем SHIFT + КЛИК по бревну в инвентаре. Оно само улетит в сетку крафта!
            client.gameMode.handleInventoryMouseClick(menu.containerId, logSlot, 0, ClickType.QUICK_MOVE, client.player);
            status = "Shift-clicking logs into crafting grid";
            cooldownTicks = 4; // Даем серверу подумать
            return;
        }

        // ШАГ 2: КРАФТ ПАЛОК ИЗ ДОСОК (Поштучно, строго друг под другом в слоты 1 и 4)
        int plankSlot = findAnyPlankSlot(menu);
        if (plankSlot != -1 && sticks < 2) {
            boolean slot1Ok = menu.slots.get(1).getItem().is(net.minecraft.tags.ItemTags.PLANKS);
            boolean slot4Ok = menu.slots.get(4).getItem().is(net.minecraft.tags.ItemTags.PLANKS);
            
            if (!slot1Ok || !slot4Ok) {
                // Если в сетке крафта лежит посторонний предмет (например, забытое бревно), очищаем сетку
                if (!menu.slots.get(1).getItem().isEmpty() && !slot1Ok) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.QUICK_MOVE, client.player);
                    cooldownTicks = 2;
                    return;
                }

                client.gameMode.handleInventoryMouseClick(menu.containerId, plankSlot, 0, ClickType.PICKUP, client.player);
                craftingPlacementPhase = 1;
                craftingSourceSlot = plankSlot;
                craftingTargetSlot = !slot1Ok ? 1 : 4;
                craftingExpectedItem = menu.slots.get(plankSlot).getItem().getItem();
                status = "Placing planks for sticks";
                cooldownTicks = 2;
                return;
            }
        }

        // ШАГ 3: СБОРКА КИРКИ (Только если палок уже хватает!)
        int nextTargetSlot = findNextMissingRecipeSlot(menu);
        if (nextTargetSlot != -1 && sticks >= 2) {
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

        // ЗАБИРАЕМ РЕЗУЛЬТАТ ИЗ СЛОТА 0
        ItemStack result = menu.slots.get(0).getItem();
        if (!result.isEmpty()) {
            if (isTargetPickaxe(result)) {
                lastCraftedPickaxeName = normalizedItemName(result);
                resetCraftingPlacement();
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                setStage(Stage.EQUIP_PICKAXE, "Crafted: " + lastCraftedPickaxeName, 3);
            } else {
                // Если скрафтили промежуточные доски или палки
                resetCraftingPlacement();
                client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                status = "Collected sub-resource from result slot";
                cooldownTicks = 3;
            }
        }
    }
