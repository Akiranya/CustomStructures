package com.ryandw11.structure.bottomfill;

import com.google.common.base.Preconditions;
import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.structure.Structure;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The default implementation for the bottom fill feature.
 */
public class DefaultBottomFill extends BukkitRunnable implements BottomFill {

    private Structure structure;
    private Location spawnLocation;
    private Queue<BlockVector2> groundPlane;
    private int minY;
    private Material fillMaterial;

    @Override
    public void performFill(Structure structure, Location spawnLocation, Location minLoc, Location maxLoc) throws IOException {
        var fillMaterial = structure.getBottomSpaceFill().getFillMaterial(spawnLocation.getBlock().getBiome());
        if (fillMaterial.isPresent()) {
            this.fillMaterial = fillMaterial.get();
        } else return;

        this.structure = structure;
        this.spawnLocation = spawnLocation;
        this.minY = minLoc.getBlockY();

        // The 2D plane which the blocks will be ground placed on
        groundPlane = new LinkedList<>();

        // The world to manipulate
        World world = spawnLocation.getWorld();
        Preconditions.checkNotNull(world, "world");

        // To get the ground plane, we need to read the schematic
        File file = new File(CustomStructures.getInstance().getDataFolder() + "/schematics/" + structure.getSchematic());
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            CustomStructures.getInstance().getLogger().warning("Invalid schematic format for schematic " + structure.getSchematic() + "!");
            CustomStructures.getInstance().getLogger().warning("Please create a valid schematic using the in-game commands!");
            return;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();

            // Note: The minimum point of a region is the lowest abs point in the original world

            var minX = minLoc.getBlockX();
            var minZ = minLoc.getBlockZ();
            var clipboardMinY = clipboard.getMinimumPoint().getBlockY();
            for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++) {
                for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint().getBlockZ(); z++) {
                    if (clipboard.getBlock(BlockVector3.at(x, clipboardMinY, z)).getBlockType().getMaterial().isMovementBlocker()) {
                        BlockVector2 groundPoint = BlockVector3.at(x, clipboardMinY, z)
                                // Get relative location for moving to new coordinate system
                                .subtract(clipboard.getMinimumPoint())
                                .toBlockVector2()
                                // Now we put the location to the new coordinate system
                                .add(minX, minZ);
                        groundPlane.add(groundPoint);
                    }
                }
            }
        }

        runTaskTimer(CustomStructures.getInstance(), 0, 1);
    }

    @Override
    public void run() {
        var world = spawnLocation.getWorld();
        Preconditions.checkNotNull(world, "world");

        // First pick 8 ground points
        for (int i = 0; i < 8; i++) {
            var groundPoint = groundPlane.poll();
            if (groundPoint == null) {
                cancel();
                return;
            }

            // Then fill the bottom space of the 8 ground points down to 32 blocks
            var y = minY - 1;
            var x = groundPoint.getBlockX();
            var z = groundPoint.getBlockZ();
            for (int j = 0; j < 32; j++) {
                boolean shouldFill =
                        // If the block is empty
                        world.getBlockAt(x, y, z).isEmpty()
                        // If the block is in the list of ignore blocks.
                        || CustomStructures.getInstance().getBlockIgnoreManager().getBlocks().contains(world.getBlockAt(x, y, z).getType())
                        // If it is water (if it is set to be ignored)
                        || (structure.getStructureProperties().shouldIgnoreWater() && world.getBlockAt(x, y, z).getType() == Material.WATER);
                if (!shouldFill) {
                    break;
                }

                world.getBlockAt(x, y, z).setType(fillMaterial);
                y--;
            }
        }
    }

}
