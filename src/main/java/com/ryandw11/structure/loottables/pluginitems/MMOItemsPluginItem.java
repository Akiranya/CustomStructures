package com.ryandw11.structure.loottables.pluginitems;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.loottables.Matchable;
import com.ryandw11.structure.loottables.PluginItem;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * When implementing this, put CustomStructures as softdepend in your plugin.yml!
 *
 * We are calling this at server startup in the method CustomStructures#registerPluginItems():
 *      PluginItemRegistry.registerForConfig("itemsadder", ItemsAdderPluginItem::new);
 *      PluginItemRegistry.registerForConfig("mmoitems", MMOItemsPluginItem::new);
 * */

public class MMOItemsPluginItem extends PluginItem<MMOItemTemplate> implements Matchable {

    public MMOItemsPluginItem() {
    }

    @Override
    public @NotNull List<ItemStack> getItemStack(@NotNull Player player) {
        // This may generate a different version on each call depending on the given player
        ItemStack itemStack = getPluginItem().newBuilder(player).build().newBuilder().build();
        Objects.requireNonNull(itemStack).setAmount(getAmount());
        return Collections.singletonList(itemStack);
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

    @Override
    public MMOItemTemplate getPluginItem() {
        if (getPlugin() == null || getItemId() == null)
            throw new LootTableException("MMOItems integration is not properly registered");
        String[] itemId = getItemId().trim().toUpperCase(Locale.ROOT).split(":");
        Type type = MMOItems.plugin.getTypes().get(itemId[0]);
        if (type == null) {
            throw new LootTableException("[MMOItems] Could not found item type: %s".formatted(itemId[0]));
        }
        MMOItemTemplate mmoItemTemplate = MMOItems.plugin.getTemplates().getTemplate(type, itemId[1]);
        if (mmoItemTemplate != null) {
            return mmoItemTemplate;
        } else {
            throw new LootTableException("[MMOItems] Could not found item: %s (type: %s)".formatted(itemId[1], itemId[0]));
        }
    }

}
