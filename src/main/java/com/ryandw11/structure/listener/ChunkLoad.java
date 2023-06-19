package com.ryandw11.structure.listener;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.utils.StructurePicker;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Class for when a chunk loads.
 *
 * @author Ryandw11
 */
public class ChunkLoad implements Listener {

    private final CustomStructures plugin;

    public ChunkLoad() {
        this.plugin = CustomStructures.getInstance();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!CustomStructures.enabled) {
            return;
        }

        // "new_chunks = true": generate structures in all chunks
        // "new_chunks = false": generate structures in new chunks
        if (!event.isNewChunk() && !plugin.getConfig().getBoolean("new_chunks")) {
            return;
        }

        Chunk chunk = event.getChunk();
        Block block = chunk.getBlock(8, 5, 8); // Grabs the block 8, 5, 8 in that chunk.

        // Schematic handler. This activity is done async to prevent the server from lagging.
        try {
            new StructurePicker(block, chunk, plugin).runTaskTimer(plugin, 1, 10);
        } catch (RuntimeException ex) {
            // ignore, error already logged.
        }
    }
}
