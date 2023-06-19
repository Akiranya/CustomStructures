package com.ryandw11.structure.loottables;

import com.ryandw11.structure.exceptions.LootTableException;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;

public class MinecraftLootTable implements LootTable {

    private final org.bukkit.loot.LootTable lootTable;

    public MinecraftLootTable(String nameSpaceString) {
        if (nameSpaceString.startsWith("minecraft:"))
            this.lootTable = Bukkit.getLootTable(NamespacedKey.minecraft(nameSpaceString.replace("minecraft:", "")));
        else {
            String[] keys = nameSpaceString.split(":");
            if (keys.length != 2) {
                throw new LootTableException("Specified minecraft loot table not found! (" + nameSpaceString + ")");
            }
            // Using internal use only NamespacedKey constructor.
            this.lootTable = Bukkit.getLootTable(new NamespacedKey(keys[0], keys[1]));
        }

        if (this.lootTable == null) {
            throw new LootTableException("Specified minecraft loot table not found! (" + nameSpaceString + ")");
        }
    }

    @Override public @NotNull String name() {
        return lootTable.key().asString();
    }

    @Override public @NotNull Collection<ItemStack> populateLoot(final @NotNull Player player) {
        Objects.requireNonNull(player);

        LootContext context = new LootContext.Builder(player.getLocation()).build();
        return lootTable.populateLoot(new Random(), context);
    }

    @Override public void fillInventory(final @NotNull Inventory inventory, final @NotNull Player player) {
        Objects.requireNonNull(inventory);
        Objects.requireNonNull(player);

        LootContext context = new LootContext.Builder(player.getLocation()).build();
        lootTable.fillInventory(inventory, new Random(), context);
    }
}
