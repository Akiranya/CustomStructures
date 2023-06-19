package com.ryandw11.structure.loottables.pluginitems;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.loottables.Matchable;
import com.ryandw11.structure.loottables.PluginItem;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/*
 * When implementing this, put CustomStructures as softdepend in your plugin.yml!
 *
 * We are calling this at server startup in the method CustomStructures#registerPluginItems():
 *      PluginItemRegistry.registerForConfig("itemsadder", ItemsAdderPluginItem::new);
 *      PluginItemRegistry.registerForConfig("mmoitems", MMOItemsPluginItem::new);
 * */

public class ItemsAdderPluginItem extends PluginItem<CustomStack> implements Matchable {

    public ItemsAdderPluginItem() {
    }

    @Override
    public @NotNull List<ItemStack> getItemStack(@NotNull Player player) {
        ItemStack itemStack = getPluginItem().getItemStack();
        itemStack.setAmount(getAmount());
        return Collections.singletonList(itemStack);
    }

    @Override
    public boolean matches(ItemStack other) {
        CustomStack otherCustomStack = CustomStack.byItemStack(other);
        if (otherCustomStack == null) {
            return false;
        } else {
            return getItemId().equalsIgnoreCase(otherCustomStack.getNamespacedID());
        }
    }

    @Override
    public CustomStack getPluginItem() {
        if (getPlugin() == null || getItemId() == null)
            throw new LootTableException("ItemsAdder integration is not properly registered");
        CustomStack customStack = CustomStack.getInstance(getItemId());
        if (customStack == null) {
            throw new LootTableException("[%s] Cannot found item with ID: %s".formatted(getPlugin(), getItemId()));
        }
        return customStack;
    }

}
