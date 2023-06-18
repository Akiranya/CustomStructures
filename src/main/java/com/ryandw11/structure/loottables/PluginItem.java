package com.ryandw11.structure.loottables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a loot item from an external plugin.
 *
 * @param <T> the type of plugin item in the external plugin codebase
 */
public abstract class PluginItem<T> extends LootItem { // TODO depends on MewCore

    @Nullable private String plugin;
    @Nullable private String itemId;

    protected PluginItem() {
        super();
    }

    protected void onConstruct() {
    }

    /**
     * Gets the Plugin ID of this Plugin Item (always lowercase).
     *
     * @return the Plugin ID
     */
    public @Nullable String getPlugin() {
        return plugin;
    }

    /**
     * Gets the Item ID of this Plugin Item (always lowercase).
     * <p>
     * The ID format is implementation-defined.
     *
     * @return the Item ID
     */
    public @Nullable String getItemId() {
        return itemId;
    }

    /**
     * Sets the Plugin ID of this Plugin Item.
     *
     * @param plugin the Plugin ID
     */
    public void setPlugin(@Nullable String plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the Item ID of this Plugin Item.
     *
     * @param itemId the Item ID
     */
    public void setItemId(@Nullable String itemId) {
        this.itemId = itemId;
    }

    /**
     * Gets an instance of the plugin item from the external plugin codebase. The implementation is expected to use
     * {@link #getPlugin()} and {@link #getItemId()} to get the specific plugin item instance from the database of the
     * external plugin.
     *
     * @return the plugin item instance
     */
    abstract public T getPluginItem();

    public static class PluginLootItemBuilder {
        private int weight;
        private String amount;
        private final String plugin;
        private final String itemId;

        protected PluginLootItemBuilder(@NotNull String plugin, @NotNull String itemId) {
            weight = 1;
            amount = "1";
            this.plugin = plugin;
            this.itemId = itemId;
        }

        public PluginLootItemBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public PluginLootItemBuilder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public PluginItem<?> build() {
            PluginItem<?> pluginItem = PluginItemRegistry.fromConfig(plugin, itemId);
            pluginItem.setWeight(weight);
            pluginItem.setAmount(amount);
            return pluginItem;
        }
    }

}
