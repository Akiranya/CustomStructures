package com.ryandw11.structure.lootchest;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.LootPopulateEvent;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.loottables.LootTableType;
import com.ryandw11.structure.structure.Structure;
import com.ryandw11.structure.utils.RandomCollection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LootChestPopulatorImpl implements LootChestPopulator {

    @Override
    public void writeTags(@NotNull Structure structure, @NotNull Container container) {
        final int firstItemIndex = 0;
        final ItemStack firstItem = container.getInventory().getItem(firstItemIndex);
        String lootTableName = null; // If this is given some value, that means this container is explicitly set to a loot table
        if (firstItem != null && firstItem.getType() == Material.PAPER) {
            if (firstItem.hasItemMeta()) {
                @NotNull final ItemMeta meta = Objects.requireNonNull(firstItem.getItemMeta());
                if (meta.hasDisplayName()) {
                    @NotNull final String displayName = meta.getDisplayName().trim();
                    if (displayName.startsWith("%${") && displayName.endsWith("}$%")) {
                        lootTableName = displayName.replace("%${", "").replace("}$%", "").trim();
                    }
                }
            }
        }
        container.getPersistentDataContainer().set(
                LootChestConstant.LOOT_CHEST,
                LootChestTagType.INSTANCE,
                LootChestTag.of(structure.getName(), lootTableName)
        );
        container.update();
        container.getInventory().clear(firstItemIndex); // This must execute lastly, otherwise container.update() would somewhat undo it
    }

    @Override
    public void populateContents(@Nullable Player player, @NotNull Container container) {
        final LootChestTag lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
        if (lootChestTag == null) return;

        // ---- Reconstruct information from the tags ----

        final Structure structure = CustomStructures.getInstance().getStructureHandler().getStructure(lootChestTag.getStructureName());
        final Optional<String> explicitLootTableName = lootChestTag.getExplicitLootTableName();
        if (structure.getLootTables().isEmpty()) return; // Returns if this structure has no loot tables at all

        // ---- Get correct loot table from the tags ----

        LootTable lootTable;
        if (explicitLootTableName.isPresent()) {
            // This container has been set an explicit loot table, use it
            lootTable = CustomStructures.getInstance().getLootTableHandler().getLootTableByName(explicitLootTableName.get());
        } else {
            // No explicit loot table set, so we need to choose one depending on the type of this container
            final LootTableType containerType = LootTableType.valueOf(container.getBlock().getType());
            final RandomCollection<LootTable> tables = structure.getLootTables(containerType);
            if (tables == null) return; // Returns if this structure has no loot tables of this container type
            lootTable = tables.next();
        }

        // ---- Trigger the loot populate event ----

        final LootPopulateEvent event = new LootPopulateEvent(player, structure, container, lootTable, lootChestTag);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // ---- Update tags stored in the container ----

        lootChestTag.processRefill(player);
        container.getPersistentDataContainer().set(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE, lootChestTag);
        container.update(); // This is necessary for PDC to be actually updated

        // ---- Populate the loots ----
        // Note that changes to the inventory must be done AFTER container.update()
        // Otherwise, the changes would be somewhat not applied (undone?)

        for (int i = 0; i < lootTable.getRolls(); i++) {
            if (container.getInventory() instanceof FurnaceInventory furnaceInventory) {
                replaceFurnaceContent(player, lootTable, furnaceInventory); // This also applies to Smoker/BlastFurnace
            } else if (container.getInventory() instanceof BrewerInventory brewerInventory) {
                replaceBrewerContent(player, lootTable, brewerInventory);
            } else {
                replaceChestContent(player, lootTable, container.getInventory());
            }
        }
    }

    /**
     * Replace the chest content and make the loot items sparsely put in the chest.
     *
     * @param lootTable          The loot table.
     * @param containerInventory The container inventory
     */
    private void replaceChestContent(@Nullable Player player, @NotNull LootTable lootTable, @NotNull Inventory containerInventory) {
        // Generate a list of shuffled indices of inventory to
        // avoid items being put too dense in the inventory
        Deque<Integer> randomPosQueue = IntStream
                .range(0, containerInventory.getSize())
                .boxed()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedList::new),
                        list -> {
                            Collections.shuffle(list);
                            return list;
                        }));

        ItemStack lootItem = lootTable.getRandomWeightedItem(player);

        for (int j = 0; j < lootItem.getAmount(); j++) {

            // This while loop tries to add a loot item with amount being 1
            while (!randomPosQueue.isEmpty()) {
                int randomPos = randomPosQueue.pollFirst();
                ItemStack randomPosItem = containerInventory.getItem(randomPos);
                if (randomPosItem != null) { // The random pos item is the same as loot item we are adding
                    if (isSameItem(randomPosItem, lootItem) && randomPosItem.getAmount() < lootItem.getMaxStackSize()) {
                        ItemStack lootItemCopy = lootItem.clone();
                        lootItemCopy.setAmount(randomPosItem.getAmount() + 1);
                        containerInventory.setItem(randomPos, lootItemCopy);
                        break;
                    }
                } else { // The random pos item is air
                    ItemStack lootItemCopy = lootItem.clone();
                    lootItemCopy.setAmount(1);
                    containerInventory.setItem(randomPos, lootItemCopy);
                    break;
                }
            }
        }
    }

    /**
     * Replace the contents of a brewer with the loot table.
     *
     * @param lootTable          The loot table to populate the brewer with.
     * @param containerInventory The inventory of the brewer.
     */
    private void replaceBrewerContent(@Nullable Player player, @NotNull LootTable lootTable, @NotNull BrewerInventory containerInventory) {
        ItemStack item = lootTable.getRandomWeightedItem(player);
        ItemStack ingredient = containerInventory.getIngredient();
        ItemStack fuel = containerInventory.getFuel();

        if (ingredient == null || ingredient.equals(item)) {
            containerInventory.setIngredient(item);
        } else if (fuel == null || fuel.equals(item)) {
            containerInventory.setFuel(item);
        }
    }

    /**
     * Replace the content of the furnace (or smoker) with loot table items.
     *
     * @param lootTable          The loot table selected for the furnace.
     * @param containerInventory The inventory of the furnace.
     */
    private void replaceFurnaceContent(@Nullable Player player, @NotNull LootTable lootTable, @NotNull FurnaceInventory containerInventory) {
        ItemStack item = lootTable.getRandomWeightedItem(player);
        ItemStack result = containerInventory.getResult();
        ItemStack fuel = containerInventory.getFuel();
        ItemStack smelting = containerInventory.getSmelting();

        if (result == null || result.equals(item)) {
            containerInventory.setResult(item);
        } else if (fuel == null || fuel.equals(item)) {
            containerInventory.setFuel(item);
        } else if (smelting == null || smelting.equals(item)) {
            containerInventory.setSmelting(item);
        }
    }


    /**
     * Check if two items are the same.
     *
     * @param first  The first item.
     * @param second The second item.
     * @return If the two items have the same metadata and type.
     */
    private boolean isSameItem(@NotNull ItemStack first, @NotNull ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        ItemMeta secondMeta = second.getItemMeta();
        return first.getType().equals(second.getType()) && Objects.equals(firstMeta, secondMeta);
    }

}
