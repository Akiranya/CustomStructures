package com.ryandw11.structure.loottables;

import com.ryandw11.structure.utils.NumberStylizer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a loot item within a loot table.
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
     * Get the item stack of this loot item.
     * <p>
     * The implementation may return a different version of the loot item on each call in the perspective of amount,
     * lore, display name, enchantment level and attribute modifiers, particularly for the items that should vary
     * between different players (such as items with random RPG stats).
     *
     * @return a newly generated item stack of this loot item
     */
    abstract public @NotNull ItemStack getItemStack();

    /**
     * Get the item stack of this loot item with the consideration of the given player.
     * <p>
     * This method is preferred over {@link #getItemStack()} because it generates the item stack with the consideration
     * of the player's data. It may be useful for certain implementation of the
     * {@link com.ryandw11.structure.loottables.LootItem}, which can generate unique items for different players.
     * Navigate the subclasses of {@link com.ryandw11.structure.loottables.LootItem} using your IDE for the details of
     * implementation.
     *
     * @param player the player who triggers the loot generation
     * @return a newly generated item stack of this loot item
     * @see #getItemStack()
     */
    abstract public @NotNull ItemStack getItemStack(Player player);

    /**
     * Get the base material of this loot item. The returned material only makes sense if this is an instance of
     * CustomItem or SimpleItem. When it is PluginItem, the returned material is generally non-meaningful.
     *
     * @return the material of this loot item
     */
    abstract public @NotNull Material getMaterial();

    /**
     * Check whether the given item stack is the same as this loot item.
     * <p>
     * The exact behaviour is implementation-defined. Check the implementation for details.
     *
     * @param other the item stack to match with
     * @return true if given item stack is the same as this loot item
     */
    abstract public boolean matches(ItemStack other);

}
