package com.ryandw11.structure.loottables;

import com.ryandw11.structure.utils.InventorySerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a loot item that contains complex NBT tags which cannot be expressed by
 * {@link com.ryandw11.structure.loottables.CustomItem} or any subclass of
 * {@link com.ryandw11.structure.loottables.PluginItem}.
 * <p>
 * <b>Side Note:</b> The goal of CustomStructures is to handle any item in loot generation. This class serves as the
 * last resort when a loot item cannot be properly serialized by CustomStructures. For better compatibility, it is
 * encouraged to implement a dedicated classes to deal with complex items, especially if the complex items are from
 * external plugins.
 */
public class ComplexItem extends LootItem implements Matchable {

    private final String itemAsBase64;

    public ComplexItem(int weight, @NotNull String amount, @NotNull String itemAsBase64) {
        super(weight, amount);
        this.itemAsBase64 = itemAsBase64;
    }

    @Override
    public @NotNull List<ItemStack> getItemStack() {
        ItemStack itemStack = InventorySerialization.decodeItemStack(itemAsBase64);
        itemStack.setAmount(getAmount());
        return Collections.singletonList(itemStack);
    }

    @Override
    public @NotNull List<ItemStack> getItemStack(@NotNull Player player) {
        return getItemStack();
    }

    @Override
    public boolean matches(ItemStack other) {
        ItemStack thisItem = InventorySerialization.decodeItemStack(itemAsBase64);
        return thisItem.isSimilar(other);
    }

    public static class ComplexItemBuilder {
        private int weight;
        private String amount;
        private final String itemAsBase64;

        protected ComplexItemBuilder(String itemAsBase64) {
            weight = 1;
            amount = "1";
            this.itemAsBase64 = itemAsBase64;
        }

        protected ComplexItemBuilder(ItemStack itemStack) {
            this(InventorySerialization.encodeItemStackToString(itemStack));
        }

        public ComplexItemBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public ComplexItemBuilder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public @NotNull ComplexItem build() {
            return new ComplexItem(
                    weight,
                    amount,
                    itemAsBase64
            );
        }
    }

}
