package com.ryandw11.structure.utils;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.SchematicHandler;
import com.ryandw11.structure.api.structaddon.StructureSection;
import com.ryandw11.structure.exceptions.StructureConfigurationException;
import com.ryandw11.structure.ignoreblocks.IgnoreBlocks;
import com.ryandw11.structure.structure.Structure;
import com.ryandw11.structure.structure.StructureHandler;
import com.ryandw11.structure.structure.properties.BlockLevelLimit;
import com.ryandw11.structure.structure.properties.StructureYSpawning;
import com.sk89q.worldedit.WorldEditException;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This class prevents the server from crashing when it attempts to pick a
 * structure.
 * <p>
 * The server will still lag a bit thanks to the nature of 1.14.
 * </p>
 *
 * @author Ryandw11
 */
public class StructurePicker extends BukkitRunnable {

    private final CustomStructures plugin;

    private int currentStructure;
    private final StructureHandler structureHandler;
    private final IgnoreBlocks ignoreBlocks;

    private Block bl;
    private final Chunk ch;

    public StructurePicker(@Nullable Block bl, Chunk ch, CustomStructures plugin) {
        this.plugin = plugin;
        currentStructure = -1;
        this.bl = bl;
        this.ch = ch;
        this.structureHandler = plugin.getStructureHandler();
        this.ignoreBlocks = plugin.getBlockIgnoreManager();
    }

    @Override
    public void run() {
        try {
            currentStructure++;
            if (currentStructure >= structureHandler.getStructures().size()) {
                this.cancel();
                return;
            }

            Structure structure = structureHandler.getStructure(currentStructure);
            StructureYSpawning structureSpawnSettings = structure.getStructureLocation().getSpawnSettings();

            // Calculate the chance.
            if (!structure.canSpawn(bl, ch))
                return;

            // If the block is null, Skip the other steps and spawn.
            if (bl == null) {
                bl = ch.getBlock(0, structureSpawnSettings.getHeight(-1), 0);
                // Now to finally paste the schematic
                SchematicHandler sh = new SchematicHandler();
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    // It is assumed at this point that the structure has been spawned.
                    // Add it to the list of spawned structures.
                    plugin.getStructureHandler().putSpawnedStructure(bl.getLocation(),
                            structure);
                    try {
                        sh.schemHandle(bl.getLocation(),
                                structure.getSchematic(),
                                structure.getStructureProperties().canPlaceAir(),
                                structure);
                    } catch (IOException | WorldEditException e) {
                        e.printStackTrace();
                    }
                });

                // Cancel the process and return.
                this.cancel();
                return;
            }

            // Allows the structure to spawn based on the ocean floor. (If the floor is not found than it just returns with the top of the water).
            if (structureSpawnSettings.isOceanFloor()) {
                if (bl.getType() == Material.WATER) {
                    for (int i = bl.getY(); i >= 4; i--) {
                        if (ch.getBlock(0, i, 0).getType() != Material.WATER) {
                            bl = ch.getBlock(0, i, 0);
                            break;
                        }
                    }
                }
            }

            // Allows the structures to no longer spawn on plant life.
            if (structure.getStructureProperties().isIgnoringPlants() && ignoreBlocks.getBlocks().contains(bl.getType())) {
                for (int i = bl.getY(); i >= 4; i--) {
                    if (!ignoreBlocks.getBlocks().contains(ch.getBlock(0, i, 0).getType()) && ch.getBlock(0, i, 0).getType() != Material.AIR) {
                        bl = ch.getBlock(0, i, 0);
                        break;
                    }
                }
            }

            // calculate SpawnY if first is true
            if (!structureSpawnSettings.isOceanFloor() && structureSpawnSettings.isCalculateSpawnYFirst()) {
                bl = ch.getBlock(0, structureSpawnSettings.getHeight(bl.getY()), 0);
            }

            if (!structure.getStructureLimitations().hasBlock(bl))
                return;

            // If it can spawn in water
            if (!structure.getStructureProperties().canSpawnInWater()) {
                if (bl.getType() == Material.WATER) return;
            }

            // If the structure can spawn in lava
            if (!structure.getStructureProperties().canSpawnInLavaLakes()) {
                if (bl.getType() == Material.LAVA) return;
            }

            // calculate SpawnY if first is false
            if (!structureSpawnSettings.isOceanFloor() && !structureSpawnSettings.isCalculateSpawnYFirst()) {
                bl = ch.getBlock(0, structureSpawnSettings.getHeight(bl.getY()), 0);
            }

            // If the structure can follows block level limit.
            // This only triggers if it spawns on the top.
            if (structure.getStructureLimitations().getBlockLevelLimit().isEnabled()) {
                BlockLevelLimit limit = structure.getStructureLimitations().getBlockLevelLimit();
                if (limit.getMode().equalsIgnoreCase("flat")) {
                    for (int x = limit.getX1() + bl.getX(); x <= limit.getX2() + bl.getX(); x++) {
                        for (int z = limit.getZ1() + bl.getZ(); z <= limit.getZ2() + bl.getZ(); z++) {
                            Block top = ch.getWorld().getBlockAt(x, bl.getY() + 1, z);
                            Block bottom = ch.getWorld().getBlockAt(x, bl.getY() - 1, z);
                            if (!(top.getType() == Material.AIR || ignoreBlocks.getBlocks().contains(top.getType())))
                                return;
                            if (bottom.getType() == Material.AIR)
                                return;
                        }
                    }
                } else if (limit.getMode().equalsIgnoreCase("flat_error")) {
                    int total = 0;
                    int error = 0;
                    for (int x = limit.getX1() + bl.getX(); x <= limit.getX2() + bl.getX(); x++) {
                        for (int z = limit.getZ1() + bl.getZ(); z <= limit.getZ2() + bl.getZ(); z++) {
                            Block top = ch.getWorld().getBlockAt(x, bl.getY() + 1, z);
                            Block bottom = ch.getWorld().getBlockAt(x, bl.getY() - 1, z);
                            if (!(top.getType() == Material.AIR || ignoreBlocks.getBlocks().contains(top.getType())))
                                error++;
                            if (bottom.getType() == Material.AIR)
                                error++;

                            total += 2;
                        }
                    }

                    if (((double) error / total) > limit.getError())
                        return;
                }
            }

            for (StructureSection section : structure.getStructureSections()) {
                // Check if the structure can spawn according to the section.
                // If an error occurs, report it to the user.
                try {
                    if(!section.checkStructureConditions(structure, bl, ch)) return;
                } catch (Exception ex) {
                    plugin.getLogger().severe(String.format("[CS Addon] An error has occurred when attempting to spawn" +
                            "the structure %s with the custom property %s!", structure.getName(), section.getName()));
                    plugin.getLogger().severe("This is not a CustomStructures error! Please report" +
                            "this to the developer of the addon.");
                    if (plugin.isDebug()) {
                        ex.printStackTrace();
                    } else {
                        plugin.getLogger().severe("Enable debug mode to see the stack trace.");
                    }
                    return;
                }
            }

            // Now to finally paste the schematic
            SchematicHandler sh = new SchematicHandler();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                // It is assumed at this point that the structure has been spawned.
                // Add it to the list of spawned structures.
                plugin.getStructureHandler().putSpawnedStructure(bl.getLocation(),
                        structure);
                try {
                    sh.schemHandle(bl.getLocation(),
                            structure.getSchematic(),
                            structure.getStructureProperties().canPlaceAir(),
                            structure);
                } catch (IOException | WorldEditException e) {
                    e.printStackTrace();
                }
            });

            this.cancel();// return after pasting
        } catch (StructureConfigurationException ex) {
            this.cancel();
            plugin.getLogger().severe("A configuration error was encountered when attempting to spawn the structure: "
                    + structureHandler.getStructure(currentStructure).getName());
            plugin.getLogger().severe(ex.getMessage());
        } catch (Exception ex) {
            this.cancel();
            plugin.getLogger().severe("An error was encountered during the schematic pasting section.");
            plugin.getLogger().severe("The task was stopped for the safety of your server!");
            plugin.getLogger().severe("For more information enable debug mode.");
            if (plugin.isDebug())
                ex.printStackTrace();
        }
    }

}
