package com.ryandw11.structure.loottables;

import com.ryandw11.structure.exceptions.LootTableException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a loot item without any item meta (no display name, lore, enchantments, etc).
 */
public class SimpleItem extends LootItem {

    @NotNull private final Material mat;

    protected SimpleItem(
            int weight,
            @NotNull String amount,
            @NotNull Material mat) {
        super(weight, amount);
        this.mat = mat;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        ItemStack itemStack = new ItemStack(mat);
        itemStack.setAmount(getAmount());
        return itemStack;
    }

    @Override
    public @NotNull ItemStack getItemStack(Player player) {
        return getItemStack();
    }

    @Override
    public @NotNull Material getMaterial() {
        return mat;
    }

    @Override
    public boolean matches(ItemStack other) {
        return other.getType() == mat && !other.hasItemMeta();
    }

    public static class SimpleItemBuilder {
        private int weight;
        private String amount;
        private final Material mat;

        protected SimpleItemBuilder(Material type) {
            weight = 1;
            amount = "1";
            mat = type;
        }

        protected SimpleItemBuilder(String type) {
            this(Material.matchMaterial(type));
            if (mat == null) {
                throw new LootTableException("Unknown material type: " + type);
            }
        }

        public SimpleItemBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public SimpleItemBuilder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public @NotNull SimpleItem build() {
            return new SimpleItem(
                    weight,
                    amount,
                    mat
            );
        }
    }

}
