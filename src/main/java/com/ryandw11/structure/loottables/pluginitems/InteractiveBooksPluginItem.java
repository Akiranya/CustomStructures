package com.ryandw11.structure.loottables.pluginitems;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.loottables.PluginItem;
import net.leonardo_dgs.interactivebooks.IBook;
import net.leonardo_dgs.interactivebooks.InteractiveBooks;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class InteractiveBooksPluginItem extends PluginItem<IBook> {

    @Override
    public @NotNull List<ItemStack> getItemStack(@NotNull Player player) {
        ItemStack item = getPluginItem().getItem(player);
        item.setAmount(getAmount());
        return Collections.singletonList(item);
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
