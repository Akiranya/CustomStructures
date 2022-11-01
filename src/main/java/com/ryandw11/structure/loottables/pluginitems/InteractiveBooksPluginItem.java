package com.ryandw11.structure.loottables.pluginitems;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.loottables.PluginItem;
import net.leonardo_dgs.interactivebooks.IBook;
import net.leonardo_dgs.interactivebooks.InteractiveBooks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InteractiveBooksPluginItem extends PluginItem<IBook> {

    @Override
    public @NotNull ItemStack getItemStack() {
        return getPluginItem().getItem();
    }

    @Override
    public @NotNull ItemStack getItemStack(Player player) {
        return getPluginItem().getItem(player);
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.WRITABLE_BOOK;
    }

    @Override
    public boolean matches(ItemStack other) {
        return false;
    }

    @Override
    public IBook getPluginItem() {
        if (getPlugin() == null || getItemId() == null)
            throw new LootTableException("InteractiveBooks integration is not properly registered");
        IBook book = InteractiveBooks.getBook(getItemId());
        if (book == null) {
            throw new LootTableException("[%s] Cannot found item with ID: %s".formatted(getPlugin(), getItemId()));
        }
        return book;
    }
    
}
