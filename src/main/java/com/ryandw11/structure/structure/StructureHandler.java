package com.ryandw11.structure.structure;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.CustomStructuresAPI;
import com.ryandw11.structure.exceptions.StructureConfigurationException;
import com.ryandw11.structure.io.StructureDatabaseHandler;
import com.ryandw11.structure.threading.CheckStructureList;
import com.ryandw11.structure.utils.Pair;
import org.bukkit.Location;

import java.io.File;
import java.util.*;

/**
 * This handler manages the list of active structures.
 * <p>
 * You can access this handler from {@link CustomStructuresAPI#getStructureHandler()} or
 * {@link CustomStructures#getStructureHandler()}.
 * <p>
 * <b>Note:</b> Do not store a long term instance of this class as it can be invalidated when the {@code /cstruct reload}
 * command is done.
 */
public class StructureHandler {

    private final SortedMap<Pair<Location, Long>, Structure> spawnedStructures = new TreeMap<>(
        Comparator.comparingDouble(o -> o.getLeft().distance(new Location(o.getLeft().getWorld(), 0, 0, 0)))
    );

    private final List<Structure> structures;
    private final List<String> names;
    private final CheckStructureList checkStructureList;
    private StructureDatabaseHandler structureDatabaseHandler;

    /**
     * Constructor for the structure handler.
     * <p>
     * This is for internal use only. Use {@link CustomStructuresAPI#getStructureHandler()} or
     * {@link CustomStructures#getStructureHandler()} instead.
     *
     * @param stringStructs The list of structures.
     * @param plugin        The plugin.
     */
    public StructureHandler(List<String> stringStructs, CustomStructures plugin) {
        structures = new ArrayList<>();
        names = new ArrayList<>();
        plugin.getLogger().info("Loading structures from files.");
        for (String s : stringStructs) {
            File struct = plugin.getDataFolderPath().resolve("structures").resolve(s.replace(".yml", "") + ".yml").toFile();
            if (!struct.exists()) {
                plugin.getLogger().warning("Structure file: " + s + ".yml does not exist! Did you make a new structure file in the \"structures\" folder?");
                plugin.getLogger().warning("For more information please check to wiki.");
                continue;
            }
            try {
                Structure tempStruct = new StructureBuilder(s.replace(".yml", ""), struct).build();
                structures.add(tempStruct);
                names.add(tempStruct.getName());
            } catch (StructureConfigurationException ex) {
                plugin.getLogger().warning("The structure '" + s + "' has an invalid configuration file:");
                plugin.getLogger().warning(ex.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().severe("An unexpected error has occurred when trying to load the structure: " + s + ".");
                plugin.getLogger().severe("Please ensure that your configuration file is valid!");
                if (plugin.isDebug()) {
                    ex.printStackTrace();
                } else {
                    plugin.getLogger().severe("Please enable debug mode to see the full error.");
                }
            }
        }

        checkStructureList = new CheckStructureList(this);
        // Run every 5 minutes.
        checkStructureList.runTaskTimerAsynchronously(plugin, 20, 6000);

        if (plugin.getConfig().getBoolean("logStructures")) {
            structureDatabaseHandler = new StructureDatabaseHandler(plugin);
            structureDatabaseHandler.runTaskTimerAsynchronously(plugin, 20, 300);
        }
    }

    /**
     * Get the list of structures.
     * <p>This list is read only and cannot be modified.</p>
     *
     * @return The list of structures.
     */
    public List<Structure> getStructures() {
        return Collections.unmodifiableList(structures);
    }

    /**
     * Get structure by name
     *
     * @param name The name
     * @return The structure. (Returns null if the structure is not found).
     */
    public Structure getStructure(String name) {
        List<Structure> result = structures.stream().filter(struct -> struct.getName().equals(name)).toList();
        if (result.isEmpty())
            return null;
        return result.get(0);
    }

    /**
     * Get the structure by a number.
     *
     * @param i The number
     * @return The structure.
     */
    public Structure getStructure(int i) {
        return structures.get(i);
    }

    /**
     * Get a modifiable copy of the names of the structures.
     *
     * @return the names of the structures
     */
    public List<String> getStructureNames() {
        return new ArrayList<>(names);
    }

    /**
     * Get the Map of spawned structures.
     * <p>Note: This map is not synchronized by default and can be modified on a different thread.</p>
     *
     * @return The list of spawned structures.
     */
    public SortedMap<Pair<Location, Long>, Structure> getSpawnedStructures() {
        return spawnedStructures;
    }

    /**
     * Add a structure to the list of spawned structures.
     * <p>This feature must be enabled via the config.</p>
     * <p>Note: This will not spawn in a structure, only add one to the list of spawned structures
     * (seen by /cstruct nearby). Use {@link Structure#spawn(Location)} to spawn a structure in the world.</p>
     *
     * @param loc    The location.
     * @param struct The structure.
     */
    public void putSpawnedStructure(Location loc, Structure struct) {
        synchronized (spawnedStructures) {
            if (structureDatabaseHandler != null) {
                structureDatabaseHandler.addStructure(loc, struct);
            }
            this.spawnedStructures.put(Pair.of(loc, System.currentTimeMillis()), struct);
        }
    }

    /**
     * Calculate if the structure is far enough away from other structures.
     *
     * @param struct   The structure to calculate that for.
     * @param location The location that the structure is spawning.
     * @return If the distance is valid according to its config.
     */
    public boolean validDistance(Structure struct, Location location) {
        double closest = Double.MAX_VALUE;
        synchronized (spawnedStructures) {
            for (Map.Entry<Pair<Location, Long>, Structure> entry : spawnedStructures.entrySet()) {
                if (entry.getKey().getLeft().getWorld() != location.getWorld()) continue;

                if (entry.getKey().getLeft().distance(location) < closest)
                    closest = entry.getKey().getLeft().distance(location);
            }
        }
        return struct.getStructureLocation().getDistanceFromOthers() < closest;
    }

    /**
     * Calculate if the structure is far enough away from other structures of the same type.
     *
     * @param struct   The structure to calculate.
     * @param location The location where the structure would spawn.
     * @return If the distance is valid according to its config.
     */
    public boolean validSameDistance(Structure struct, Location location) {

        synchronized (spawnedStructures) {
            double closest = Double.MAX_VALUE;
            for (Map.Entry<Pair<Location, Long>, Structure> entry : spawnedStructures.entrySet()) {
                if (entry.getKey().getLeft().getWorld() != location.getWorld())
                    continue;
                if (!Objects.equals(entry.getValue().getName(), struct.getName()))
                    continue;
                if (entry.getKey().getLeft().distance(location) < closest)
                    closest = entry.getKey().getLeft().distance(location);
            }
            return struct.getStructureLocation().getDistanceFromSame() < closest;
        }

    }

    /**
     * Get the structure database handler.
     * <p>This feature must be enabled via the config.</p>
     *
     * @return An Optional of the StructureDatabaseHandler.
     */
    public Optional<StructureDatabaseHandler> getStructureDatabaseHandler() {
        return Optional.ofNullable(structureDatabaseHandler);
    }

    /**
     * Shutdown internal processes.
     */
    public void cleanup() {
        checkStructureList.cancel();
        if (structureDatabaseHandler != null) {
            structureDatabaseHandler.cancel();
        }
        spawnedStructures.clear();
    }
}
