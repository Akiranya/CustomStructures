package com.ryandw11.structure.loottables;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class LootItemBuilder {

    /**
     * Gets a builder of {@link com.ryandw11.structure.loottables.SimpleItem}.
     *
     * @param type the material
     * @return a builder of {@link com.ryandw11.structure.loottables.SimpleItem}
     */
    public static @NotNull SimpleItem.SimpleItemBuilder simpleItem(@NotNull String type) {
        return new SimpleItem.SimpleItemBuilder(type);
    }

    /**
     * Gets a builder of {@link com.ryandw11.structure.loottables.CustomItem}.
     *
     * @param type the base material
     * @return a builder of {@link com.ryandw11.structure.loottables.CustomItem}
     */
    public static @NotNull CustomItem.CustomLootItemBuilder customItem(@NotNull String type) {
        return new CustomItem.CustomLootItemBuilder(type);
    }

    /**
     * Gets a builder of {@link com.ryandw11.structure.loottables.PluginItem}.
     *
     * @param plugin the plugin name where the item is from
     * @param itemId the item ID (implementation-defined)
     * @return a builder of {@link com.ryandw11.structure.loottables.PluginItem}
     */
    public static @NotNull PluginItem.PluginLootItemBuilder pluginItem(@NotNull String plugin, @NotNull String itemId) {
        return new PluginItem.PluginLootItemBuilder(plugin, itemId);
    }

    /**
     * @param itemAsBase64 the base64 string of the item stack
     * @return a builder of {@link com.ryandw11.structure.loottables.ComplexItem}
     * @see #complexItem(org.bukkit.inventory.ItemStack)
     */
    public static @NotNull ComplexItem.ComplexItemBuilder complexItem(@NotNull String itemAsBase64) {
        return new ComplexItem.ComplexItemBuilder(itemAsBase64);
    }

    /**
     * Gets a builder of {@link com.ryandw11.structure.loottables.ComplexItem}.
     *
     * @param itemStack the item stack
     * @return a builder of {@link com.ryandw11.structure.loottables.ComplexItem}
     */
    public static @NotNull ComplexItem.ComplexItemBuilder complexItem(@NotNull ItemStack itemStack) {
        return new ComplexItem.ComplexItemBuilder(itemStack);
    }

    /**
     * Gets a builder of {@link com.ryandw11.structure.loottables.TableItem}.
     *
     * @param tableKey the name of a loot table
     * @return a builder of {@link com.ryandw11.structure.loottables.TableItem}
     */
    public static @NotNull TableItem.TableItemBuilder tableItem(@NotNull String tableKey) {
        return new TableItem.TableItemBuilder(tableKey);
    }

}
