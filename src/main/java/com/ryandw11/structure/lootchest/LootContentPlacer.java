package com.ryandw11.structure.lootchest;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class contains some utility methods to nicely put loot items in a loot chest.
 */
public final class LootContentPlacer {

    /**
     * Put loot items sparsely in the chest.
     *
     * @param items     the loot items
     * @param inventory the inventory of the chest
     */
    public static void replaceContent(List<ItemStack> items, Inventory inventory) {
        if (inventory instanceof FurnaceInventory furnaceInventory) {
            replaceFurnaceContent(items, furnaceInventory); // This also applies to Smoker/BlastFurnace
        } else if (inventory instanceof BrewerInventory brewerInventory) {
            replaceBrewerContent(items, brewerInventory);
        } else {
            replaceChestContent(items, inventory);
        }
    }

    /**
     * Put loot items sparsely in the chest.
     *
     * @param items     the loot items
     * @param inventory the inventory of the chest
     */
    public static void replaceChestContent(List<ItemStack> items, Inventory inventory) {
        for (ItemStack lootItem : items) {
            if (inventory.firstEmpty() < 0) return;
            for (int j = 0; j < lootItem.getAmount(); j++) {

                int attemptCount = 0;

                // This while-loop attempts to add a loot item x1 in a random slot:
                //      If selected slot is AIR, then set the AIR to be the loot item
                //      If selected slot already has the loot item, then increase the amount by 1
                while (attemptCount++ <= inventory.getSize()) {
                    int randomPos = ThreadLocalRandom.current().nextInt(inventory.getSize());
                    ItemStack randomPosItem = inventory.getItem(randomPos);
                    if (randomPosItem != null) {
                        if (isSameItem(randomPosItem, lootItem) && randomPosItem.getAmount() < lootItem.getMaxStackSize()) {
                            ItemStack lootItemCopy = lootItem.clone();
                            lootItemCopy.setAmount(randomPosItem.getAmount() + 1);
                            inventory.setItem(randomPos, lootItemCopy);
                            break;
                        }
                    } else {
                        ItemStack lootItemCopy = lootItem.clone();
                        lootItemCopy.setAmount(1);
                        inventory.setItem(randomPos, lootItemCopy);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Replace the contents of a brewer with the loot table items.
     *
     * @param items     the loot items to populate the brewer with
     * @param inventory the inventory of the brewer
     */
    static void replaceFurnaceContent(List<ItemStack> items, FurnaceInventory inventory) {
        ItemStack loot = items.get(0);

        ItemStack fuel = inventory.getFuel();
        ItemStack result = inventory.getResult();
        ItemStack smelting = inventory.getSmelting();

        if (result == null) {
            inventory.setResult(loot);
        } else if (fuel == null) {
            inventory.setFuel(loot);
        } else if (smelting == null) {
            inventory.setSmelting(loot);
        }
    }

    /**
     * Replace the content of the furnace (or smoker) with loot table items.
     *
     * @param items     the loot items to populate the furnace with
     * @param inventory the inventory of the furnace
     */
    static void replaceBrewerContent(List<ItemStack> items, BrewerInventory inventory) {
        ItemStack loot = items.get(0);

        ItemStack ingredient = inventory.getIngredient();
        ItemStack fuel = inventory.getFuel();

        if (ingredient == null) {
            inventory.setIngredient(loot);
        } else if (fuel == null) {
            inventory.setFuel(loot);
        }
    }

    /**
     * Check if two items are the same.
     *
     * @param first  the first item
     * @param second the second item
     * @return true if the two items have the same metadata and type, otherwise false
     */
    static boolean isSameItem(@NotNull ItemStack first, @NotNull ItemStack second) {
        ItemMeta firstMeta = first.getItemMeta();
        ItemMeta secondMeta = second.getItemMeta();
        return first.getType().equals(second.getType()) && Objects.equals(firstMeta, secondMeta);
    }

}
