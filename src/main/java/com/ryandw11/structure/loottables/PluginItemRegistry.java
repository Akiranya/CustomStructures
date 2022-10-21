package com.ryandw11.structure.loottables;

import com.ryandw11.structure.exceptions.LootTableException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PluginItemRegistry {

    private static final Map<String, Supplier<PluginItem>> constructors = new HashMap<>();

    public static void registerForConfig(String pluginId, Supplier<PluginItem> constructor) {
        constructors.put(pluginId.toLowerCase(), constructor);
    }

    public static void unRegisterForConfig(String pluginId) {
        constructors.remove(pluginId.toLowerCase());
    }

    public static @NotNull PluginItem fromConfig(String plugin, String itemId) {
        plugin = plugin.toLowerCase();
        itemId = itemId.toLowerCase();
        if (constructors.containsKey(plugin)) {
            PluginItem item = constructors.get(plugin).get();
            item.setPlugin(plugin);
            item.setItemId(itemId);
            item.onConstruct();
            return item;
        }
        throw new LootTableException("Unsupported plugin item \"" + itemId + "\" from the plugin \"" + plugin + "\".");
    }

}
