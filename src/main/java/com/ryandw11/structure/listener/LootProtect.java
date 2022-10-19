package com.ryandw11.structure.listener;

import com.ryandw11.structure.lootchest.LootChestConstant;
import com.ryandw11.structure.lootchest.LootChestTag;
import com.ryandw11.structure.lootchest.LootChestTagType;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class LootProtect implements Listener {

    /**
     * Prevents loot container from being destroyed by players.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void preventBreakLoot(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Container container) {
            LootChestTag lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
            if (lootChestTag != null && !event.getPlayer().hasPermission("customstructures.breaklootchest")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.playSound(player, Sound.ENTITY_CAT_STRAY_AMBIENT, 1F, 1F);
                player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(-1));
            }
        }
    }

    /**
     * Prevent loot container from being destroyed by entity explosion.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void preventEntityExplosion(EntityExplodeEvent event) {
        preventExplosion(event.blockList());
    }

    /**
     * Prevent loot container from being destroyed by block explosion.
     *
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void preventBlockExplosion(BlockExplodeEvent event) {
        preventExplosion(event.blockList());
    }

    private void preventExplosion(@NotNull List<Block> blocks) {
        Iterator<Block> blockList = blocks.iterator();
        while (blockList.hasNext()) {
            Block block = blockList.next();
            if (block.getState() instanceof Container container) {
                LootChestTag lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
                if (lootChestTag != null) {
                    blockList.remove();
                }
            }
        }
    }

}