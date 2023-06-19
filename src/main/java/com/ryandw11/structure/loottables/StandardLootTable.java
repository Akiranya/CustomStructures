package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.lootchest.LootContentPlacer;
import com.ryandw11.structure.utils.RandomCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Represents a StandardLootTable.
 */
public class StandardLootTable implements LootTable {

    private final @NotNull String name;
    private final int rolls;
    private final boolean replacement;
    private final @NotNull RandomCollection<LootItem> randomCollection;
    private final @NotNull FileConfiguration lootTableConfig;

    /**
     * Create a loot table with the given name.
     *
     * <p>This will try to load the loot table file with the specified name.</p>
     *
     * @param name the name of the loot table
     */
    public StandardLootTable(@NotNull String name) {

        // ---- Load loot table file ----

        File lootTableFile = CustomStructures.getInstance().getDataFolder()
            .toPath()
            .resolve("lootTables")
            .resolve(name + ".yml")
            .toFile();
        if (!lootTableFile.exists())
            throw new LootTableException("Cannot find the following loot table file: " + name);

        this.lootTableConfig = YamlConfiguration.loadConfiguration(lootTableFile); // We cannot catch the exceptions we want from here

        // Just load the file again to catch the exceptions
        try {
            lootTableConfig.load(lootTableFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new LootTableException("Failed loading StandardLootTable: \"" + name + "\"! Please view the guide on the wiki for more information.");
        }

        this.name = name;

        if (!lootTableConfig.contains("Rolls"))
            throw new LootTableException("Invalid loot table! Cannot find global \"Rolls\" setting at: " + name);
        if (!lootTableConfig.contains("Replacement"))
            throw new LootTableException("Invalid loot table! Cannot find global \"Replacement\" setting at: " + name);

        this.rolls = lootTableConfig.getInt("Rolls");
        this.replacement = lootTableConfig.getBoolean("Replacement");
        this.randomCollection = new RandomCollection<>();

        loadLootItems();
    }

    @Override public @NotNull String name() {
        return name;
    }

    @Override public @NotNull Collection<ItemStack> populateLoot(final @NotNull Player player) {
        Objects.requireNonNull(player);

        return draw(player);
    }

    @Override public void fillInventory(final @NotNull Inventory inventory, final @NotNull Player player) {
        Objects.requireNonNull(inventory);
        Objects.requireNonNull(player);

        LootContentPlacer.replaceContent(draw(player), inventory);
    }

    /**
     * Get the number of items chosen.
     *
     * @return the number of items chosen
     */
    private int getRolls() {
        return rolls;
    }

    /**
     * Get whether the loot items should be drawn with-replacement / without-replacement.
     *
     * @return true if the loot items should be drawn with-replacement;
     * false if the loot items should be drawn without-replacement.
     */
    private boolean isReplacement() {
        return replacement;
    }

    /**
     * Select and return loot items depending on the value of {@link #isReplacement()}. If {@link #isReplacement()} is
     * {@code true}, then the selected loot items are drawn with-replacement; if {@link #isReplacement()} is
     * {@code false}, then the selected loot items are drawn without-replacement.
     *
     * @return the selected loot items depending on the return value of {@link #isReplacement()}
     */
    private Collection<LootItem> select() {
        Collection<LootItem> loots = isReplacement() ? new ArrayList<>() : new HashSet<>();

        int groundRolls;
        if (isReplacement()) {
            groundRolls = getRolls();
        } else {
            groundRolls = Math.min(getRolls(), randomCollection.size()); // Take min value to prevent infinite loops
        }

        for (int i = 0; i < groundRolls; i++) {
            if (!loots.add(randomCollection.next()))
                i--;
        }

        return loots;
    }

    /**
     * Draw {@literal N} loot items from this loot table, where {@literal N} is obtained by {@link #getRolls()}.
     *
     * @param player the player who triggers this loot generation
     * @return {@literal N} random loot items from this loot table
     */
    private @NotNull List<ItemStack> draw(@NotNull Player player) {
        Objects.requireNonNull(player);

        return select().stream().flatMap(loot -> loot.getItemStack(player).stream()).toList();
    }

    /**
     * Get all the loot items within the loot table.
     *
     * @return an unmodifiable list of all the loot items in this loot table
     */
    private @NotNull List<LootItem> getLootItems() {
        return randomCollection.getMap().values().stream().toList();
    }

    /**
     * Load all the loot items from this loot table file.
     */
    private void loadLootItems() {
        if (!lootTableConfig.contains("Items"))
            throw new LootTableException("Invalid loot table! The \"Items\" section is required in the config: " + name);

        ConfigurationSection itemsSection = Objects.requireNonNull(lootTableConfig.getConfigurationSection("Items"), "\"Items\" entry not found in the loot table config: " + name);

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);

            //<editor-fold desc="Validate basic config values">
            if (itemSection == null)
                throw new LootTableException("Invalid file format for loot table at: " + name + "/" + itemId);
            if (!itemSection.isString("Type"))
                throw new LootTableException("Invalid file format for loot table! Cannot find \"Type\" setting at: " + name + "/" + itemId);
            if (!itemSection.isInt("Weight") || itemSection.getInt("Weight") < 1)
                throw new LootTableException("Invalid file format for loot table! \"Weight\" is not an integer at: " + name + "/" + itemId);
            //</editor-fold>

            //<editor-fold desc="Get common config to all types of LootItem">
            String type = Objects.requireNonNull(itemSection.getString("Type")).toUpperCase(); // Already checked nullability
            String amount = itemSection.getString("Amount", "1"); // This is optional so give it a def
            int weight = itemSection.getInt("Weight"); // Already checked nullability
            //</editor-fold>

            //<editor-fold desc="Add loot item depending on "Type"">
            switch (type) {
                case "TABLE" -> {
                    String tableKey = Objects.requireNonNull(itemSection.getString("Key"), "The required option \"Key\" not found at: " + name + "/" + itemId);
                    TableItem lootItem = LootItemBuilder.tableItem(tableKey)
                        .amount(amount)
                        .weight(weight)
                        .build();
                    randomCollection.add(lootItem.getWeight(), lootItem);
                }
                case "COMPLEX" -> {
                    String key = Objects.requireNonNull(itemSection.getString("Key"), "The required option \"Key\" not found at: " + name + "/" + itemId);
                    String itemAsBase64 = Objects.requireNonNull(CustomStructures.getInstance().getComplexItemManager().getItemAsBase64(key), "Cannot find a complex item with the key of " + key + " at: " + name + "/" + itemId);
                    LootItem lootItem = LootItemBuilder.complexItem(itemAsBase64)
                        .amount(amount)
                        .weight(weight)
                        .build();
                    randomCollection.add(lootItem.getWeight(), lootItem);
                }
                case "PLUGIN" -> {
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
                }
                default -> {
                    if (itemSection.contains("Name") ||
                        itemSection.contains("Lore") ||
                        itemSection.contains("Enchantments") ||
                        itemSection.contains("CustomModelData")
                    ) {
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
                        LootItem lootItem = builder.build();
                        randomCollection.add(lootItem.getWeight(), lootItem);
                    } else {
                        // If it's a SimpleItem

                        LootItem lootItem = LootItemBuilder.simpleItem(type)
                            .amount(amount)
                            .weight(weight)
                            .build();
                        randomCollection.add(lootItem.getWeight(), lootItem);
                    }
                }
            }
            //</editor-fold>
        }
    }
}
