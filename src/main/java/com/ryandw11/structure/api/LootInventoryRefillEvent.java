package com.ryandw11.structure.api;

import com.ryandw11.structure.lootchest.LootChestTag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a loot container is about to get refilled. Note that this event is called
 * <b>before</b> the loot table is selected and populated in the container, so the loot table is unknown when this
 * event gets called.
 * <p>
 * This event only gets fired when refilling, not when the container is populated for the first time. If you wanted to
 * cancel the initial content population, you may use {@link com.ryandw11.structure.api.LootPopulateEvent}.
 * <p>
 * Cancelling this event will prevent the container from being refilled, and make all other potential code think the
 * loot inventory should not be refilled.
 */
public class LootInventoryRefillEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull LootChestTag lootChestTag;
    private boolean cancel = false;

    public LootInventoryRefillEvent(@NotNull Player who, @NotNull LootChestTag lootChestTag) {
        super(who);
        this.lootChestTag = lootChestTag;
    }

    /**
     * @return the tags stored in the container that the player is opening
     */
    public @NotNull LootChestTag getLootChestTag() {
        return lootChestTag;
    }

    @Override public boolean isCancelled() {
        return cancel;
    }

    @Override public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}