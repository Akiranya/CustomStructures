package com.ryandw11.structure.loottables;

import org.bukkit.inventory.ItemStack;

public interface Matchable {

    /**
     * Check whether the given item stack is the same as this loot item.
     * <p>
     * The exact behaviour is implementation-defined. Check the implementation for details.
     *
     * @param other the item stack to match with
     * @return true if given item stack is the same as this loot item
     */
    boolean matches(ItemStack other);

}
