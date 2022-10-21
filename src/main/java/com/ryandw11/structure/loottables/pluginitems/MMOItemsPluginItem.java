package com.ryandw11.structure.loottables.pluginitems;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.loottables.PluginItem;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/*
 * When implementing this, put CustomStructures as softdepend in your plugin.yml!
 *
 * We are calling this at server startup in the method CustomStructures#registerPluginItems():
 *      PluginItemRegistry.registerForConfig("itemsadder", ItemsAdderPluginItem::new);
 *      PluginItemRegistry.registerForConfig("mmoitems", MMOItemsPluginItem::new);
 * */

public class MMOItemsPluginItem extends PluginItem {

    public MMOItemsPluginItem() {
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        // This may generate a different version on each call if this item enabled RNG stats
        ItemStack itemStack = getMMOItemTemplate().newBuilder().build().newBuilder().getItemStack();
        itemStack.setAmount(getAmount());
        return itemStack;
    }

    @Override
    public @NotNull ItemStack getItemStack(Player player) {
        // This may generate a different version on each call depending on the given player
        ItemStack itemStack = getMMOItemTemplate().newBuilder(player).build().newBuilder().getItemStack();
        itemStack.setAmount(getAmount());
        return itemStack;
    }

    @Override
    public @NotNull Material getMaterial() {
        return getItemStack().getType();
    }

    @Override
    public boolean matches(ItemStack other) {
        if (getItemId() == null) return false;
        NBTItem nbtItem = NBTItem.get(other);
        if (!nbtItem.hasType()) {
            return false;
        } else {
            String type = nbtItem.getType();
            String id = nbtItem.getString("MMOITEMS_ITEM_ID");
            return getItemId().equalsIgnoreCase(type + ":" + id);
        }
    }

    private @NotNull MMOItemTemplate getMMOItemTemplate() {
        if (getPlugin() == null || getItemId() == null)
            throw new LootTableException("MMOItems integration is not properly registered");
        String[] itemId = getItemId().trim().toUpperCase(Locale.ROOT).split(":");
        Type type = MMOItems.plugin.getTypes().get(itemId[0]);
        if (type == null) {
            throw new LootTableException("[%s] Could not found item type: %s".formatted(getPlugin(), itemId[0]));
        }
        MMOItemTemplate mmoItemTemplate = MMOItems.plugin.getTemplates().getTemplate(type, itemId[1]);
        if (mmoItemTemplate != null) {
            return mmoItemTemplate;
        } else {
            throw new LootTableException("[%s] Could not found item: %s (type: %s)".formatted(getPlugin(), itemId[1], itemId[0]));
        }
    }
}
