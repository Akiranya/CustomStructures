package com.ryandw11.structure.loottables;

import com.ryandw11.structure.exceptions.LootTableException;
import com.ryandw11.structure.utils.NumberStylizer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a loot item that has custom name, lore, enchantments and custom model data.
 */
@SuppressWarnings("unused")
public class CustomItem extends SimpleItem {

    @Nullable private final String name;
    @NotNull private final List<String> lore;
    @NotNull private final Map<String, String> enchantments;
    private final int customModelData;

    /**
     * Constructs a custom item without any meta (essentially a SimpleItem).
     *
     * @param weight          the weight
     * @param amount          the amount
     * @param mat             the material
     * @param name            the name, or null if none
     * @param lore            the lore, or empty list if none
     * @param enchantments    the enchantments, or empty map if none
     * @param customModelData the custom model data, or -1 if none
     */
    protected CustomItem(
            int weight,
            @NotNull String amount,
            @NotNull Material mat,
            @Nullable String name,
            @NotNull List<String> lore,
            @NotNull Map<String, String> enchantments,
            int customModelData) {
        super(weight, amount, mat);
        this.name = name;
        this.lore = lore;
        this.enchantments = enchantments;
        this.customModelData = customModelData;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        ItemStack item = new ItemStack(getMaterial());

        // Set amount
        item.setAmount(getAmount());

        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());

        // Set display name if any
        meta.setDisplayName(name);

        // Set lore
        meta.setLore(lore.isEmpty() ? null : lore);

        // Set enchantments
        enchantments.forEach((e, l) -> {
            int level = NumberStylizer.getStylizedInt(l);
            Enchantment enchantment = Objects.requireNonNull(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(e))); // This should never be null
            if (meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
                enchantmentStorageMeta.addStoredEnchant(enchantment, level, true);
            } else {
                item.addUnsafeEnchantment(enchantment, level);
            }
        });

        // Set custom model data
        meta.setCustomModelData(customModelData == -1 ? null : customModelData);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public @NotNull ItemStack getItemStack(Player player) {
        return getItemStack();
    }

    @Override
    public boolean matches(ItemStack other) {
        if (getMaterial() != other.getType()) {
            return false;
        }
        if (!other.hasItemMeta()) {
            return false;
        }
        ItemMeta otherMeta = Objects.requireNonNull(other.getItemMeta());

        // ---- Check name ----

        if (name != null && !otherMeta.hasDisplayName()) { // This has name, other does not
            return false;
        } else if (name != null && otherMeta.hasDisplayName()) { // Both have name
            if (!ChatColor.stripColor(name).equalsIgnoreCase(ChatColor.stripColor(otherMeta.getDisplayName()))) { // Check if not matching
                return false;
            }
        } /*else {}*/ // Neither have name, or this does not have name (but other does)

        // ---- Check custom model data ----

        if (getCustomModelData().isPresent() && !otherMeta.hasCustomModelData()) { // This has CMD, other does not
            return false;
        } else if (getCustomModelData().isPresent() && otherMeta.hasCustomModelData() && !getCustomModelData().get().equals(otherMeta.getCustomModelData())) { // Both have CMD, check if not matching
            return false;
        } /*else {}*/ // Neither have CMD, or this does not have CMD (but other does)

        // ---- Check lore ----

        if (!lore.isEmpty() && !otherMeta.hasLore()) { // This has lore, other does not
            return false;
        } else if (!lore.isEmpty() && otherMeta.hasLore()) { // Both have lore
            if (!matchLore(otherMeta.getLore())) { // Check if not matching
                return false;
            }
        } /*else {}*/ // Neither have lore, or this does not have lore (but other does)

        // ---- Check enchantments (ignoring levels) ----

        if (enchantments.size() > otherMeta.getEnchants().size()) { // This has more # enchantments than other
            return false;
        } else { // This has the same or less # enchantments than other
            Set<String> thisEnchantSet = this.enchantments.keySet();
            List<String> otherEnchantList = otherMeta.getEnchants().keySet().stream().map(e -> e.getKey().getKey()).toList();
            for (String thisEnchant : thisEnchantSet) {
                boolean included = false;
                for (String otherEnchant : otherEnchantList) {
                    if (thisEnchant.equals(otherEnchant)) {
                        included = true;
                        break;
                    }
                }
                if (!included) { // This has the enchantment but other does not
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * If this item has lore that matches the given lore.
     * <p>
     * It matches if our lore is contained in the given lore consecutively, ignoring color of the given lore.
     *
     * @param otherLore the given lore to match
     * @return true if the given lore contains our lore consecutively
     */
    private boolean matchLore(List<String> otherLore) {
        if (lore.isEmpty()) return true;
        int lastIndex = 0;
        boolean foundFirst = false;
        for (String line : lore) {
            do {
                if (lastIndex == otherLore.size()) {
                    // There is more in lore than in otherLore, bad
                    return false;
                }
                String usedLine = otherLore.get(lastIndex);
                if (line.equalsIgnoreCase(usedLine) || line.equalsIgnoreCase(ChatColor.stripColor(usedLine))) {
                    // If the line is correct, we have found our first, and we want all consecutive lines to also equal
                    foundFirst = true;
                } else if (foundFirst) {
                    // If a consecutive line is not equal, that's bad
                    return false;
                }
                lastIndex++;
                // If we once found one correct line, iterate over 'lore' consecutively
            } while (!foundFirst);
        }
        return true;
    }

    /**
     * Gets the display name of this item if any, or {@code null} if none.
     * <p>
     * Note that color codes in the string are {@link org.bukkit.ChatColor#COLOR_CHAR}.
     *
     * @return the display name of this item if any, or {@code null} if none
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Gets the lore on this item, or empty list if none.
     * <p>
     * Note that color codes in the string are {@link org.bukkit.ChatColor#COLOR_CHAR}.
     *
     * @return the lore on this item, or empty list if none
     */
    public @NotNull List<String> getLore() {
        return lore;
    }

    /**
     * @return the enchantments on this item, or empty list if none
     */
    public @NotNull Map<String, String> getEnchantments() {
        return enchantments;
    }

    /**
     * @return the custom model data on this item
     */
    public Optional<Integer> getCustomModelData() {
        return customModelData == -1 ? Optional.empty() : Optional.of(customModelData);
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public static class CustomLootItemBuilder {
        private int weight;
        private String amount;
        private final Material mat;
        private String name;
        private final List<String> lore;
        private final Map<String, String> enchantments;
        private int customModelData;

        protected CustomLootItemBuilder(String type) {
            weight = 1;
            amount = "1";
            mat = Material.matchMaterial(type);
            if (mat == null) {
                throw new LootTableException("Unknown Material Type: " + type);
            }
            name = null;
            lore = new ArrayList<>();
            enchantments = new HashMap<>();
            customModelData = -1; // -1 means no custom model data
        }

        public CustomLootItemBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public CustomLootItemBuilder amount(String amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Set the display name of this item. Support color code "&".
         *
         * @param name the display name to set
         */
        public CustomLootItemBuilder name(@Nullable String name) {
            if (name != null)
                this.name = ChatColor.translateAlternateColorCodes('&', name);
            return this;
        }

        /**
         * Add lore to this item. Support color code "&".
         *
         * @param lore the lore to add
         */
        public CustomLootItemBuilder lore(@NotNull List<String> lore) {
            if (lore.isEmpty()) return this;
            List<String> colorized = lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList();
            this.lore.addAll(colorized);
            return this;
        }

        /**
         * @param lore the lore to add
         * @see #lore(java.util.List)
         */
        public CustomLootItemBuilder lore(@Nullable String... lore) {
            if (lore == null) return this;
            List<String> added = Arrays.stream(lore).toList();
            lore(added);
            return this;
        }

        /**
         * Add enchantment to this item.
         *
         * @param key   the namespaced key of the enchantment. See <a
         *              href="https://minecraft.fandom.com/wiki/Enchanting">Enchanting (Minecraft Wiki)</a> for the
         *              correct namespaced key (click on any enchantment page from the table, then you can see the
         *              namespaced key)
         * @param level the level of this enchantment. Stylised number is supported, see
         *              {@link com.ryandw11.structure.utils.NumberStylizer} for details
         */
        public CustomLootItemBuilder enchantment(@NotNull String key, @NotNull String level) {
            if (EnchantmentWrapper.getByKey(NamespacedKey.minecraft(key)) == null) {
                throw new LootTableException("Invalid enchantment key: " + key);
            }
            this.enchantments.put(key.trim().toLowerCase(), level);
            return this;
        }

        /**
         * Set the custom model data on this item. Passing {@code -1} to clear the custom model data.
         *
         * @param customModelData the custom model data to set, or {@code -1} to clear it
         */
        public CustomLootItemBuilder customModelData(int customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public @NotNull CustomItem build() {
            return new CustomItem(
                    weight,
                    amount,
                    mat,
                    name,
                    lore,
                    enchantments,
                    customModelData
            );
        }
    }

}
