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

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

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
                replaceFurnaceContent(lootTable, furnaceInventory); // This also applies to Smoker/BlastFurnace
            } else if (container.getInventory() instanceof BrewerInventory brewerInventory) {
                replaceBrewerContent(lootTable, brewerInventory);
            } else {
                replaceChestContent(lootTable, new Random(), container.getInventory());
            }
        }
    }

    /**
     * Replace the chest content.
     *
     * @param lootTable          The loot table.
     * @param random             The value of random.
     * @param containerInventory The container inventory
     */
    private void replaceChestContent(LootTable lootTable, Random random, Inventory containerInventory) {
        ItemStack[] containerContent = containerInventory.getContents();
        ItemStack randomItem = lootTable.getRandomWeightedItem();
        for (int j = 0; j < randomItem.getAmount(); j++) {
            boolean done = false;
            int attempts = 0;
            while (!done) {
                int randomPos = random.nextInt(containerContent.length);
                ItemStack randomPosItem = containerInventory.getItem(randomPos);
                if (randomPosItem != null) {
                    if (this.isSameItem(randomPosItem, randomItem)) {
                        if (randomPosItem.getAmount() < randomItem.getMaxStackSize()) {
                            ItemStack randomItemCopy = randomItem.clone();
                            int newAmount = randomPosItem.getAmount() + 1;
                            randomItemCopy.setAmount(newAmount);
                            containerContent[randomPos] = randomItemCopy;
                            containerInventory.setContents(containerContent);
                            done = true;
                        }
                    }
                } else {
                    ItemStack randomItemCopy = randomItem.clone();
                    randomItemCopy.setAmount(1);
                    containerContent[randomPos] = randomItemCopy;
                    containerInventory.setContents(containerContent);
                    done = true;
                }
                attempts++;
                if (attempts >= containerContent.length) {
                    done = true;
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
    private void replaceBrewerContent(LootTable lootTable, BrewerInventory containerInventory) {
        ItemStack item = lootTable.getRandomWeightedItem();
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
    private void replaceFurnaceContent(LootTable lootTable, FurnaceInventory containerInventory) {
        ItemStack item = lootTable.getRandomWeightedItem();
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
    private boolean isSameItem(ItemStack first, ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        ItemMeta secondMeta = second.getItemMeta();
        return first.getType().equals(second.getType()) && Objects.equals(firstMeta, secondMeta);
    }

}
