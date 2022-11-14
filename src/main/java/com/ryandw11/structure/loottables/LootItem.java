package com.ryandw11.structure.loottables;

import com.ryandw11.structure.utils.NumberStylizer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a loot item within a loot table.
 * <p>
 * The loot item is abstracted in a way that, it can either be a {@link org.bukkit.inventory.ItemStack}, or an abstract
 * form such as multiple item stacks from another {@link com.ryandw11.structure.loottables.LootTable} (i.e., a loot
 * table can contain another loot table - the nested loot tables are support).
 */
public abstract class LootItem {

    private int weight = 1;
    private String amount = "1";

    protected LootItem() {
        // Take default values for weight and amount
    }

    protected LootItem(int weight, @NotNull String amount) {
        this.weight = weight;
        this.amount = amount;
    }

    /**
     * Get the weight of this loot item.
     *
     * @return the weight of this loot item
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Get the amount of this loot item. The return value may vary on each call.
     *
     * @return the amount of this loot item
     */
    public int getAmount() {
        return NumberStylizer.getStylizedInt(amount);
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setAmount(@NotNull String amount) {
        this.amount = amount;
    }

    /**
     * Get the list of item stacks of this loot item.
     * <p>
     * In most cases, the returned list is singleton. Caller can safely get the single item by simply calling
     * List#get(0). However, the returned list may contain multiple items if the implementation is
     * {@link TableItem#getItemStack()} which returns all the drawn items from a loot table.
     *
     * @return a newly generated item stack of this loot item
     */
    abstract public @NotNull List<ItemStack> getItemStack();

    /**
     * Get the list of item stacks of this loot item with the consideration of given player.
     * <p>
     * In most cases, the returned list is singleton. Caller can safely get the single item by simply calling
     * List#get(0). However, the returned list may contain multiple items if the implementation is
     * {@link TableItem#getItemStack()} which returns all the drawn items from a loot table.
     * <p>
     * This method is preferred over {@link #getItemStack()} because it has the access to the player who triggers the
     * loot generation. It may be useful for certain implementation of
     * {@link com.ryandw11.structure.loottables.LootItem}, which can generate unique items for different players.
     * Navigate the subclasses of {@link com.ryandw11.structure.loottables.LootItem} using your IDE for the details of
     * implementation.
     *
     * @param player the player who triggers the loot generation
     * @return a newly generated item stack of this loot item
     * @see #getItemStack()
     */
    abstract public @NotNull List<ItemStack> getItemStack(@NotNull Player player);

}
