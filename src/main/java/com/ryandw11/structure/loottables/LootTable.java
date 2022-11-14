package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.utils.RandomCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a LootTable.
 */
public class LootTable {

    @NotNull private final String name;
    private final int rolls;
    private final boolean replacement;
    @NotNull private final RandomCollection<LootItem> randomCollection;
    @NotNull private final FileConfiguration lootTableConfig;

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
        lootTableConfig = YamlConfiguration.loadConfiguration(lootTableFile); // We cannot catch the exceptions we want from here

        // Just load the file again to catch the exceptions
        try {
            lootTableConfig.load(lootTableFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new LootTableException("Failed loading LootTable: \"" + name + "\"! Please view the guide on the wiki for more information.");
        }

        // ------------------------------

        this.name = name;

        if (!lootTableConfig.contains("Rolls"))
            throw new LootTableException("Invalid loot table! Cannot find global \"Rolls\" setting at: " + name);
        if (!lootTableConfig.contains("Replacement"))
            throw new LootTableException("Invalid loot table! Cannot find global \"Replacement\" setting at: " + name);

        rolls = lootTableConfig.getInt("Rolls");
        replacement = lootTableConfig.getBoolean("Replacement");
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
     * Get whether the loot items should be drawn with-replacement / without-replacement.
     *
     * @return true if the loot items should be drawn with-replacement; false if the loot items should be drawn
     * without-replacement.
     */
    public boolean isReplacement() {
        return replacement;
    }

    /**
     * Draw a loot item from this loot table.
     *
     * @return a random loot item from this loot table
     * @see #drawOne(org.bukkit.entity.Player)
     */
    public @NotNull List<ItemStack> drawOne() {
        return randomCollection.next().getItemStack();
    }

    /**
     * Draw a loot item from this loot table.
     * <p>
     * This method is preferred over {@link #drawOne()} because it has the access to the player who triggers this loot
     * generation. Whenever the player is null, the returned item stack is the same as that returned by
     * {@link #drawOne()}.
     *
     * @param player the player who triggers this loot generation
     * @return a random loot item from this loot table
     */
    public @NotNull List<ItemStack> drawOne(@Nullable Player player) {
        if (player == null) { // Fallback
            return drawOne();
        }
        return randomCollection.next().getItemStack(player);
    }

    /**
     * Select and return loot items depending on the value of {@link #isReplacement()}. If {@link #isReplacement()} is
     * {@literal true}, then the selected loot items are drawn with-replacement; if {@link #isReplacement()} is
     * {@literal false}, then the selected loot items are drawn without-replacement.
     *
     * @return the selected loot items depending on the value of {@link #replacement}
     */
    private Collection<LootItem> selectLoots() {
        Collection<LootItem> loots = replacement ? new ArrayList<>() : new HashSet<>();
        for (int i = 0; i < getRolls(); i++) {
            if (!loots.add(randomCollection.next())) {
                i--;
            }
        }
        return loots;
    }

    /**
     * Draw {@literal N} loot items from this loot table, where {@literal N} is obtained by {@link #getRolls()}.
     *
     * @return {@literal N} random loot items from this loot table
     * @see #drawAll(org.bukkit.entity.Player)
     */
    public @NotNull List<ItemStack> drawAll() {
        return selectLoots().stream().flatMap(loot -> loot.getItemStack().stream()).toList();
    }

    /**
     * Draw {@literal N} loot items from this loot table, where {@literal N} is obtained by {@link #getRolls()}.
     *
     * @param player the player who triggers this loot generation
     * @return {@literal N} random loot items from this loot table
     */
    public @NotNull List<ItemStack> drawAll(@Nullable Player player) {
        if (player == null) { // Fallback
            return drawAll();
        }
        return selectLoots().stream().flatMap(loot -> loot.getItemStack(player).stream()).toList();
    }

    /**
     * Get all the loot items within the loot table.
     *
     * @return an unmodifiable list of all the loot items in this loot table
     */
    public @NotNull List<LootItem> getLootItems() {
        return randomCollection.getMap().values().stream().toList();
    }

    /**
     * Load all the loot items from this loot table file.
     */
    private void loadLootItems() {
        if (!lootTableConfig.contains("Items"))
            throw new LootTableException("Invalid loot table! The \"Items\" section is required in the config: " + name);

        ConfigurationSection itemsSection = lootTableConfig.getConfigurationSection("Items");
        Objects.requireNonNull(itemsSection, "\"Items\" entry not found in the loot table config: " + name);

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);

            // ---- Validate basic config values ----

            if (itemSection == null)
                throw new LootTableException("Invalid file format for loot table at: " + name + "/" + itemId);
            if (!itemSection.contains("Type") || !itemSection.isString("Type"))
                throw new LootTableException("Invalid file format for loot table! Cannot find \"Type\" setting at: " + name + "/" + itemId);
            if (!itemSection.isInt("Weight") || itemSection.getInt("Weight") < 1)
                throw new LootTableException("Invalid file format for loot table! \"Weight\" is not an integer at: " + name + "/" + itemId);

            // ---- Get common config to all types of LootItem ----

            @NotNull String type = Objects.requireNonNull(itemSection.getString("Type")); // Already checked nullability
            @NotNull String amount = itemSection.getString("Amount", "1"); // This is optional so give it a def
            int weight = itemSection.getInt("Weight"); // Already checked nullability

            // ---- Add loot item depending on "Type" ----

            if (type.equalsIgnoreCase("TABLE")) {

                // ---- Type = "TABLE" ----

                String tableKey = Objects.requireNonNull(itemSection.getString("Key"), "The required option \"Key\" not found at: " + name + "/" + itemId);
                TableItem lootItem = LootItemBuilder.tableItem(tableKey)
                        .amount(amount)
                        .weight(weight)
                        .build();
                randomCollection.add(lootItem.getWeight(), lootItem);

            } else if (type.equalsIgnoreCase("COMPLEX")) {

                // ---- Type = "COMPLEX" ----

                String key = Objects.requireNonNull(itemSection.getString("Key"), "The required option \"Key\" not found at: " + name + "/" + itemId);
                String itemAsBase64 = CustomStructures.getInstance().getComplexItemManager().getItemAsBase64(key);
                Objects.requireNonNull(itemAsBase64, "Cannot find a complex item with the key of " + key + " at: " + name + "/" + itemId);
                LootItem lootItem = LootItemBuilder.complexItem(itemAsBase64)
                        .amount(amount)
                        .weight(weight)
                        .build();
                randomCollection.add(lootItem.getWeight(), lootItem);

            } else if (type.equalsIgnoreCase("PLUGIN")) {

                // ---- Type = "PLUGIN" ----

                String id = Objects.requireNonNull(itemSection.getString("ID"), "The required option \"ID\" not found at: " + name + "/" + itemId);
                String[] pluginItemId = id.split(":", 2);
                if (pluginItemId.length != 2) {
                    throw new LootTableException("The option \"ID\" must be in the form of \"{pluginId}:{reference}\". Error at: " + name + "/" + itemId);
                }
                LootItem lootItem = LootItemBuilder.pluginItem(pluginItemId[0], pluginItemId[1])
                        .amount(amount)
                        .weight(weight)
                        .build();
                randomCollection.add(lootItem.getWeight(), lootItem);

            } else {

                // ---- Type = "{Material}" ----

                if (itemSection.contains("Name") ||
                    itemSection.contains("Lore") ||
                    itemSection.contains("Enchantments") ||
                    itemSection.contains("CustomModelData")) { // If it's a CustomItem

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

                    LootItem lootItem = builder.build();
                    randomCollection.add(lootItem.getWeight(), lootItem);

                } else { // If it's a SimpleItem

                    LootItem lootItem = LootItemBuilder.simpleItem(type)
                            .amount(amount)
                            .weight(weight)
                            .build();
                    randomCollection.add(lootItem.getWeight(), lootItem);

                }
            }
        }

    }

}
