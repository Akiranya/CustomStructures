package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.utils.Pair;
import com.ryandw11.structure.utils.RandomCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a LootTable.
 */
public class LootTable {

    @NotNull private final String name;
    private final int rolls;
    @NotNull private final RandomCollection<LootItem> randomCollection;
    @NotNull private final FileConfiguration lootTablesFC;

    /**
     * Create a loot table with the given name.
     *
     * <p>This will try to load the loot table file with the specified name.</p>
     *
     * @param name the name of the loot table
     */
    public LootTable(@NotNull String name) {

        // ---- Load loot table file ----

        File lootTableFile = new File(CustomStructures.getInstance().getDataFolder() + "/lootTables/" + name + ".yml");
        if (!lootTableFile.exists())
            throw new LootTableException("Cannot find the following loot table file: " + name);
        lootTablesFC = YamlConfiguration.loadConfiguration(lootTableFile); // We cannot catch the exceptions we want from here

        // Just load the file again to catch the exceptions
        try {
            lootTablesFC.load(lootTableFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new LootTableException("Failed loading LootTable: \"" + name + "\"! Please view the guide on the wiki for more information.");
        }

        // ------------------------------

        this.name = name;

        if (!lootTablesFC.contains("Rolls"))
            throw new LootTableException("Invalid loot table! Cannot find global \"Rolls\" setting at: " + name);

        rolls = lootTablesFC.getInt("Rolls");
        randomCollection = new RandomCollection<>();

        loadLootItems();
    }

    /**
     * Get the name of the loot table.
     *
     * @return the name of the loot table
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Get the number of items chosen.
     *
     * @return the number of items chosen
     */
    public int getRolls() {
        return rolls;
    }

    /**
     * Get a random item from the table.
     *
     * @return a random item
     */
    public @NotNull ItemStack getRandomWeightedItem() {
        return randomCollection.next().getItemStack();
    }

    /**
     * Get the loot items within the loot table.
     *
     * @return an unmodifiable list of the loot items in this loot table
     */
    public @NotNull List<LootItem> getLootItems() {
        return randomCollection.getMap().values().stream().toList();
    }

    /**
     * Load all the loot items from this loot table file.
     */
    private void loadLootItems() {
        if (!lootTablesFC.contains("Items"))
            throw new LootTableException("Invalid loot table! The \"Items\" section is required in the config: " + name);

        ConfigurationSection itemsSection = lootTablesFC.getConfigurationSection("Items");
        Objects.requireNonNull(itemsSection, "\"Items\" entry not found in the loot table config: " + name);

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            Pair<LootItem, Integer> lootItem = loadItemFromConfig(itemId, itemSection);
            randomCollection.add(lootItem.getRight(), lootItem.getLeft());
        }

    }

    /**
     * Load a loot item from a Configuration Section.
     *
     * @param itemId      the item ID
     * @param itemSection the configuration section of the item
     * @return a pair, where left is the loaded LootItem and right is its Weight
     */
    private @NotNull Pair<LootItem, Integer> loadItemFromConfig(@NotNull String itemId, @Nullable ConfigurationSection itemSection) {
        if (itemSection == null)
            throw new LootTableException("Invalid file format for loot table at: " + name + "/" + itemId);
        if (!itemSection.contains("Type") || !itemSection.isString("Type"))
            throw new LootTableException("Invalid file format for loot table! Cannot find \"Type\" setting at: " + name + "/" + itemId);
        if (!itemSection.isInt("Weight") || itemSection.getInt("Weight") < 1)
            throw new LootTableException("Invalid file format for loot table! \"Weight\" is not an integer at: " + name + "/" + itemId);

        @NotNull String type = Objects.requireNonNull(itemSection.getString("Type")); // Already checked nullability in validateItemConfig(itemId)
        @NotNull String amount = itemSection.getString("Amount", "1"); // This is optional so give it a def
        int weight = itemSection.getInt("Weight"); // Already checked nullability in validateItemConfig(itemId)

        LootItem lootItem; // The loot item to return

        if (type.equalsIgnoreCase("COMPLEX")) {
            // ---- Type = "COMPLEX" ----

            String key = Objects.requireNonNull(itemSection.getString("Key"), "The required option \"Key\" not found at: " + name + "/" + itemId);
            String itemAsBase64 = CustomStructures.getInstance().getComplexItemManager().getItemAsBase64(key);
            Objects.requireNonNull(itemAsBase64, "Cannot find a complex item with the key of " + key + " at: " + name + "/" + itemId);
            lootItem = LootItemBuilder.complexItem(itemAsBase64)
                    .amount(amount)
                    .weight(weight)
                    .build();

        } else if (type.equalsIgnoreCase("PLUGIN")) {
            // ---- Type = "PLUGIN" ----

            String id = Objects.requireNonNull(itemSection.getString("ID"), "The required option \"ID\" not found at: " + name + "/" + itemId);
            String[] pluginItemId = id.split(":", 2);
            if (pluginItemId.length != 2) {
                throw new LootTableException("The option \"ID\" must be in the form of \"{pluginId}:{reference}\". Error at: " + name + "/" + itemId);
            }
            lootItem = LootItemBuilder.pluginItem(pluginItemId[0], pluginItemId[1])
                    .amount(amount)
                    .weight(weight)
                    .build();

        } else {
            // ---- Type = "{Material}" ----

            if (itemSection.contains("Name") || itemSection.contains("Lore") || itemSection.contains("Enchantments") || itemSection.contains("CustomModelData")) {
                // If it's a CustomItem

                CustomItem.CustomLootItemBuilder builder = LootItemBuilder.customItem(type)
                        .amount(amount)
                        .weight(weight)
                        .name(itemSection.getString("Name"))
                        .lore(itemSection.getStringList("Lore"))
                        .customModelData(itemSection.getInt("CustomModelData", -1));
                ConfigurationSection enchantSection = itemSection.getConfigurationSection("Enchantments");
                if (enchantSection != null) {
                    Set<String> keys = enchantSection.getKeys(false);
                    for (String key : keys) {
                        if (!enchantSection.isString(key) || !enchantSection.isInt(key)) {
                            throw new LootTableException("Enchantment level must be an integer \"X\" or ranged integers \"[X:Y]\" at: " + name + "/" + enchantSection.getCurrentPath() + "." + key);
                        }
                        builder.enchantment(key, Objects.requireNonNull(enchantSection.getString(key)));
                    }
                }

                lootItem = builder.build();

            } else {
                // If it's a SimpleItem

                lootItem = LootItemBuilder.simpleItem(type)
                        .amount(amount)
                        .weight(weight)
                        .build();
            }
        }

        return Pair.of(lootItem, weight);
    }

}
