package com.ryandw11.structure.bottomfill;

import com.ryandw11.structure.structure.Structure;
import org.bukkit.Location;

/**
 * The interface for bottom fill implementations.
 *
 * <p>Use the {@link BottomFillProvider} to get the correct implementation and
 * register a new one.</p>
 */
public interface BottomFill {
    /**
     * Called by the plugin when a bottom fill should be performed.
     *
     * <p>This will only be called if the BottomFill option is enabled.</p>
     *
     * @param structure     The structure that was spawned.
     * @param spawnLocation The spawn (paste) location of the structure.
     * @param minLoc        The minimum location of the structure in the world to paste onto.
     * @param maxLoc        The maximum location of the structure in the world to paste onto.
     */
    void performFill(Structure structure, Location spawnLocation, Location minLoc, Location maxLoc);
}
