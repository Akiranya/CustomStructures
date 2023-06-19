package com.ryandw11.structure.api;

import com.ryandw11.structure.lootchest.LootChestTag;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is called just before a container is populated with a loot table.
 * <p>
 * Cancelling this event will stop the loot population for the container, and leaves the container intact.
 * <p>
 * When this event gets called, the container is already allowed to refill its content according to various settings,
 * that is, {@link com.ryandw11.structure.lootchest.LootChestTag#shouldRefill(org.bukkit.entity.Player)} returns true.
 */
public class LootPopulateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final @Nullable Player player;
    private final @NotNull Structure structure;
    private final @NotNull Container container;
    private final @NotNull LootTable lootTable;
    private final @NotNull LootChestTag lootChestTag;
    private boolean canceled;

    /**
     * Construct the loot populate event.
     *
     * @param player       the player who triggers this population
     * @param structure    the structure which this container belongs to
     * @param container    the container to be populated
     * @param lootTable    the loot table that is selected for this container
     * @param lootChestTag the loot chest tags stored in this container
     */
    public LootPopulateEvent(
        @Nullable Player player,
        @NotNull Structure structure,
        @NotNull Container container,
        @NotNull LootTable lootTable,
        @NotNull LootChestTag lootChestTag
    ) {
        this.player = player;
        this.structure = structure;
        this.lootTable = lootTable;
        this.container = container;
        this.lootChestTag = lootChestTag;
        this.canceled = false;
    }


    /**
     * @return the player who is opening this container, or {@code null} if this population has no player related to
     */
    public @Nullable Player getPlayer() {
        return player;
    }

    /**
     * Get the structure that spawned.
     *
     * @return The structure that spawned.
     */
    public @NotNull Structure getStructure() {
        return structure;
    }

    /**
     * Get the container to be populated.
     *
     * @return The location of the container.
     */
    public @NotNull Container getContainer() {
        return container;
    }

    /**
     * Get the selected loot table for the container.
     *
     * @return The selected loot table for the container.
     */
    public @NotNull LootTable getLootTable() {
        return lootTable;
    }

    /**
     * @return the tags stored in the container that the player is opening
     */
    public @NotNull LootChestTag getLootChestTag() {
        return lootChestTag;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        canceled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
