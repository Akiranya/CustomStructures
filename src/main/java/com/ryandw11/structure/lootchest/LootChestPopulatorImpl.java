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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        // Otherwise the changes would be somewhat not applied (be undone...?)

        List<ItemStack> loots = lootTable.drawAll(player);
        if (container.getInventory() instanceof FurnaceInventory furnaceInventory) {
            LootContentPlacer.replaceFurnaceContent(loots, furnaceInventory); // This also applies to Smoker/BlastFurnace
        } else if (container.getInventory() instanceof BrewerInventory brewerInventory) {
            LootContentPlacer.replaceBrewerContent(loots, brewerInventory);
        } else {
            LootContentPlacer.replaceChestContent(loots, container.getInventory());
        }
    }

}
