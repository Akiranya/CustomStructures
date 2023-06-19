package com.ryandw11.structure.lootchest;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.LootInventoryRefillEvent;
import com.ryandw11.structure.config.Duration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

class LootChestTagImpl implements LootChestTag {

    // Generation related
    private final String structureName;
    private final String explicitLootTableName;

    // Refill related
    private static final Random RANDOM = new Random();
    private long lastFill = -1;
    private long nextRefill = -1;
    private int numRefills = 0;
    private Map<UUID, Long> lootedPlayers;

    public LootChestTagImpl(@NotNull String structureName, @Nullable String explicitLootTableName) {
        this.structureName = structureName;
        this.explicitLootTableName = explicitLootTableName;
    }

    @Override
    public @NotNull String getStructureName() {
        return structureName;
    }

    @Override
    public @NotNull Optional<String> getExplicitLootTableName() {
        return Optional.ofNullable(explicitLootTableName);
    }

    public boolean shouldRefill(@Nullable Player player) {
        // ALWAYS process the first fill or if the feature is disabled
        if (this.lastFill == -1 || !isRefillEnabled()) {
            return true;
        }

        // Only process refills when a player is set
        if (player == null) {
            return false;
        }

        // Chest is not scheduled for refill
        if (this.nextRefill == -1) {
            return false;
        }

        FileConfiguration config = CustomStructures.getInstance().getConfig();

        // Check if max refills has been hit
        if (config.getInt("lootables.maxRefills") != -1 && this.numRefills >= config.getInt("lootables.maxRefills")) {
            return false;
        }

        // Refill has not been reached
        if (this.nextRefill > System.currentTimeMillis()) {
            return false;
        }

        LootInventoryRefillEvent lootInventoryRefillEvent = new LootInventoryRefillEvent(player, this);
        lootInventoryRefillEvent.callEvent();
        if (config.getBoolean("lootables.restrictPlayerReloot") && hasPlayerLooted(player)) {
            lootInventoryRefillEvent.setCancelled(true);
        }
        return !lootInventoryRefillEvent.isCancelled();
    }

    public void processRefill(@Nullable Player player) {
        lastFill = System.currentTimeMillis();
        FileConfiguration config = CustomStructures.getInstance().getConfig();
        if (config.getBoolean("lootables.autoRefill")) {
            long min = Duration.of(config.getString("lootables.refreshMin")).seconds();
            long max = Duration.of(config.getString("lootables.refreshMax")).seconds();
            nextRefill = lastFill + (min + RANDOM.nextLong(max - min + 1)) * 1000L;
            numRefills++;
            if (player != null) {
                setPlayerLootedState(player, true);
            }
        }
    }

    @Override
    public boolean isRefillEnabled() {
        return CustomStructures.getInstance().getConfig().getBoolean("lootables.autoRefill");
    }

    @Override
    public boolean hasPlayerLooted(@NotNull UUID player) {
        return lootedPlayers != null && lootedPlayers.containsKey(player);
    }

    @Override
    public void setPlayerLootedState(@NotNull UUID player, boolean looted) {
        if (looted && lootedPlayers == null) {
            lootedPlayers = new HashMap<>();
        }
        if (looted) {
            if (!lootedPlayers.containsKey(player)) {
                lootedPlayers.put(player, System.currentTimeMillis());
            }
        } else if (lootedPlayers != null) {
            lootedPlayers.remove(player);
        }
    }

    @Override
    public long getLastFilled() {
        return lastFill;
    }

    @Override
    public long getNextRefill() {
        return nextRefill;
    }

    @Override
    public long setNextRefill(long nextRefill) {
        long prevNextRefill = this.nextRefill;
        this.nextRefill = nextRefill;
        return prevNextRefill;
    }

    @Override
    public @NotNull Optional<Long> getLastLooted(@NotNull UUID player) {
        return lootedPlayers != null ? Optional.ofNullable(lootedPlayers.get(player)) : Optional.empty();
    }

}
