package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.CustomStructuresAPI;
import com.ryandw11.structure.utils.InventorySerialization;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Manages the custom items defined for the loot tables.
 *
 * <p>Get this handler from {@link CustomStructuresAPI#getComplexItemManager()}</p>
 */
public class ComplexItemManager {
    private FileConfiguration config;
    private File file;
    private CustomStructures plugin;

    /**
     * This should only ever be constructed by the CustomStructures main class.
     *
     * <p>Use {@link CustomStructuresAPI#getComplexItemManager()} to access this class for the plugin.</p>
     *
     * @param plugin the plugin instance
     * @param file   the file of Complex Items
     * @param dir    the directory to put the file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ComplexItemManager(CustomStructures plugin, File file, File dir) {
        if (!dir.exists())
            dir.mkdir();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().severe("Cannot create Complex Items file. Enable debug mode for more information");
                if (plugin.isDebug())
                    ex.printStackTrace();
                return;
            }
        }
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.plugin = plugin;
    }

    /**
     * Add an item to the Complex Items file.
     *
     * @param key       the key to add
     * @param itemStack the item stack to add
     * @return true if the item was successfully added, or false if the key already exists in the file
     */
    public boolean addItem(String key, ItemStack itemStack) {
        if (this.config.contains(key))
            return false;
        config.set(key + ".data", InventorySerialization.encodeItemStackToString(itemStack.clone()));
        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save Complex Items file after adding an item");
            if (plugin.isDebug()) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Remove an item from the Complex Items file.
     *
     * @param key the key to remove
     * @return true if the key was successfully removed, or false if the key is not in the file
     */
    public boolean removeItem(String key) {
        if (!this.config.contains(key))
            return false;
        config.set(key, null);
        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save Complex Items file after removing an item with key: " + key);
            if (plugin.isDebug()) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Get an item from the item file.
     *
     * @param key the key to which maps to the item
     * @return the item stack, or null if the item does not exist
     */
    public @Nullable ItemStack getItem(String key) {
        String itemAsBase64 = getItemAsBase64(key);
        if (itemAsBase64 != null) {
            return InventorySerialization.decodeItemStack(itemAsBase64);
        } else return null;
    }

    /**
     * Get an item from the item file.
     *
     * @param key the key to which maps to the item
     * @return the item stack as base64 string, or null if the item does not exist
     */
    public @Nullable String getItemAsBase64(String key) {
        if (config.contains(key)) {
            return config.getString(key + ".data");
        } else return null;
    }

    /**
     * Get the File Configuration for the Complex Items file.
     *
     * <p>This is meant for internal use only.</p>
     *
     * @return the file configuration of all complex items
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
