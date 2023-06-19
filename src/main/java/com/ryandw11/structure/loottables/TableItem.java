package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loot item that is another loot table.
 */
public class TableItem extends LootItem {

    private final LootTable table;

    public TableItem(
            int weight,
            @NotNull String amount,
            @NotNull LootTable table
    ) {
        super(weight, amount);
        this.table = table;
    }

    @Override
    public @NotNull List<ItemStack> getItemStack(@NotNull Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < getAmount(); i++) {
            stacks.addAll(table.populateLoot(player));
        }
        return stacks;
    }

    /**
     * Gets the loot table contained in this loot item
     *
     * @return the loot table contained in this loot item
     */
    public LootTable getTable() {
        return table;
    }

    public static class TableItemBuilder {
        private int weight;
        private String amount;
        private final LootTable table;

        protected TableItemBuilder(String tableKey) {
            weight = 1;
            amount = "1";
            table = CustomStructures.getInstance().getLootTableHandler().getLootTableByName(tableKey);
        }

        public TableItem.TableItemBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public TableItem.TableItemBuilder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public @NotNull TableItem build() {
            return new TableItem(weight, amount, table);
        }
    }

}
