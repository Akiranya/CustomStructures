package com.ryandw11.structure.api;

import com.ryandw11.structure.lootchest.LootChestTag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event which is called when a loot container gets refilled. Cancelling this event will prevent the
 * container from being refilled.
 */
@SuppressWarnings("unused")
public class LootContainerRefillEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull private final LootChestTag lootChestTag;
    private boolean cancel = false;

    public LootContainerRefillEvent(@NotNull Player who, @NotNull LootChestTag lootChestTag) {
        super(who);
        this.lootChestTag = lootChestTag;
    }

    /**
     * @return the tags stored in the container that the player is opening
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

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}