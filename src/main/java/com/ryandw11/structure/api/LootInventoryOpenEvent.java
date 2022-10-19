package com.ryandw11.structure.api;

import com.ryandw11.structure.lootchest.LootChestTag;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * <p>Represents an event which is called when a player opens (i.e. typically right-clicks) a loot container that
 * belongs to a custom structure. Cancelling this event will only prevent the player opening the loot container, but it
 * does NOT prevent the container from generating loots or refilling.
 *
 * <p>Listeners may use this event to modify the loot contents of the container. An example would be
 * {@link com.ryandw11.structure.listener.PlayerInteract#generateLoot(LootInventoryOpenEvent)}.
 */
@SuppressWarnings("unused")
public class LootInventoryOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull private final Structure structure;
    @NotNull private final Container container;
    @NotNull private final LootChestTag lootChestTag;
    @Nullable private final LootTable lootTable;
    private boolean cancel = false;

    /**
     * @param who       the player who opens the container
     * @param structure the structure which this container belongs to
     * @param container the container opened by the player
     * @param lootTable the loot table if one is specifically set for this container, or simply pass {@code null} if no
     *                  one is set
     */
    public LootInventoryOpenEvent(
            @NotNull Player who,
            @NotNull Structure structure,
            @NotNull Container container,
            @NotNull LootChestTag lootChestTag,
            @Nullable LootTable lootTable) {
        super(who);
        this.structure = structure;
        this.container = container;
        this.lootChestTag = lootChestTag;
        this.lootTable = lootTable;
    }

    /**
     * @return the structure which this container belongs to
     */
    public @NotNull Structure getStructure() {
        return structure;
    }

    /**
     * @return the container the player is opening
     */
    public @NotNull Container getContainer() {
        return container;
    }

    /**
     * @return the explicitly specific loot table for this container (if one is set), or {@code empty} if there is no
     * explicitly specific loot table set for this container
     */
    public @NotNull Optional<LootTable> getLootTable() {
        return Optional.ofNullable(lootTable);
    }

    /**
     * @return the loot chest tags stored in this container
     */
    public @NotNull LootChestTag getLootChestTag() {
        return lootChestTag;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
