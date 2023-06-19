package com.ryandw11.structure.lootchest;

import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents information stored in {@link PersistentDataContainer} of a loot container in a {@link Structure}. This
 * information is later used to identify which structure this container belongs to, and which explicit {@link LootTable}
 * this container should be filled with (if one is explicitly set), and whether this container should refill its
 * contents.
 */
public interface LootChestTag {

    static LootChestTag of(@NotNull String structureName, @Nullable String lootTableName) {
        return new LootChestTagImpl(structureName, lootTableName);
    }

    /**
     * Returns the name of the structure which this loot chest is bound to.
     *
     * @return the name of the structure which this loot chest is bound to
     */
    @NotNull String getStructureName();

    /**
     * Returns the name of explicitly specified {@link LootTable} if one is set, otherwise {@link Optional#empty()}.
     *
     * @return the name of explicitly specified {@link LootTable} if one is set, otherwise {@link Optional#empty()}
     */
    @NotNull Optional<String> getExplicitLootTableName();

    boolean isRefillEnabled();

    default boolean hasBeenFilled() {
        return getLastFilled() != -1;
    }

    boolean hasPlayerLooted(@NotNull UUID player);

    default boolean hasPlayerLooted(@NotNull Player player) {
        return hasPlayerLooted(player.getUniqueId());
    }

    void setPlayerLootedState(@NotNull UUID player, boolean looted);

    default void setPlayerLootedState(@NotNull Player player, boolean looted) {
        setPlayerLootedState(player.getUniqueId(), looted);
    }

    default boolean hasPendingRefill() {
        long nextRefill = getNextRefill();
        return nextRefill != -1 && nextRefill > getLastFilled();
    }

    long getLastFilled();

    long getNextRefill();

    long setNextRefill(long refillAt);

    @NotNull Optional<Long> getLastLooted(@NotNull UUID player);

    default Optional<Long> getLastLooted(@NotNull Player player) {
        return getLastLooted(player.getUniqueId());
    }

    /**
     * Returns whether this container should be refilled.
     *
     * @param player the player who is opening this loot container
     * @return true if this container should be refilled, otherwise false
     */
    boolean shouldRefill(@Nullable Player player);

    /**
     * <p>Processes the refill once.
     *
     * <p>This only updates relevant tags stored in this instance - no changes will apply to the attached container by
     * calling this method. To make the changes actually apply to the container, it requires to manually update the
     * PersistentDataContainer of the container. See {@link org.bukkit.block.Container#getPersistentDataContainer()} for
     * more details.
     *
     * @param player the player who is opening this loot container
     */
    void processRefill(@Nullable Player player);

}
