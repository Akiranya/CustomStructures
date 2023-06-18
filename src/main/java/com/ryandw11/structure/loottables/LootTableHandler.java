package com.ryandw11.structure.loottables;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.CustomStructuresAPI;
import com.ryandw11.structure.exceptions.LootTableException;

import java.util.*;

/**
 * This handles the loot tables.*
 * <p>
 * Get this handler via {@link CustomStructuresAPI#getLootTableHandler()}.
 */
public class LootTableHandler {

    private final Map<String, LootTable> lootTables;

    public LootTableHandler() {
        lootTables = new HashMap<>();
    }

    /**
     * Get the loot table by the name.
     * <p>
     * This will automatically load a loot table
     *
     * @param lootTableName the name of the loot table
     * @return the loot table, or null if the loot table does not exist or loads with an error
     */
    public LootTable getLootTableByName(String lootTableName) {
        if (!lootTables.containsKey(lootTableName)) {
            try {
                lootTables.put(lootTableName, new LootTable(lootTableName));
            } catch (LootTableException ex) {
                CustomStructures.getInstance().getLogger().severe("There seems to be a problem with the \"" + lootTableName + "\" loot table:");
                CustomStructures.getInstance().getLogger().severe(ex.getMessage());
            }
        }
        return lootTables.get(lootTableName);
    }

    /**
     * Get an unmodifiable map of the loot tables.
     *
     * @return an unmodifiable map of loot tables
     */
    public Map<String, LootTable> getLootTables() {
        return Collections.unmodifiableMap(lootTables);
    }

    /**
     * Get a copy of mutable list with the names of all loot tables.
     *
     * @return a copy of list with names of all loot tables
     */
    public List<String> getLootTablesNames() {
        return new ArrayList<>(lootTables.keySet());
    }

}
