package com.ryandw11.structure.listener;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.LootInventoryOpenEvent;
import com.ryandw11.structure.lootchest.LootChestConstant;
import com.ryandw11.structure.lootchest.LootChestPopulator;
import com.ryandw11.structure.lootchest.LootChestTag;
import com.ryandw11.structure.lootchest.LootChestTagType;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class PlayerInteract implements Listener {

    /**
     * <p><b>Internal Use Only.</b>
     *
     * <p>It is used to fire an {@link com.ryandw11.structure.api.LootInventoryOpenEvent} when a player opens a loot
     * container of custom structure, so other code (internal or external code) can modify the loot container.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void callLootEvent(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType().isAir()) { // Make sure we are manipulating a block
            return;
        }

        BlockState state = clickedBlock.getState();
        if (state instanceof Container container) {
            @Nullable LootChestTag lootChestTag; // null means this container is not from a custom structure
            if (container.getInventory() instanceof DoubleChestInventory doubleChest) {
                // We need to check both sides of DoubleChest as the tags are only stored in one of the two sides
                container = (Container) Objects.requireNonNull(doubleChest.getLeftSide().getLocation(), "left block is null").getBlock().getState();
                lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
                if (lootChestTag == null) { // Left does not have tags, check right then
                    container = (Container) Objects.requireNonNull(doubleChest.getRightSide().getLocation(), "right block is null").getBlock().getState();
                    lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
                }
            } else {
                lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
            }

            if (lootChestTag == null) {
                return;
            }

            // ---- Get raw information in the tags ----

            String structureName = lootChestTag.getStructureName();
            Optional<String> explicitLootTableName = lootChestTag.getExplicitLootTableName();

            // ---- Construct and fire the event ----

            // Note that we need to make sure the stored name of structure and loot table are valid
            // before we fire the event. This should make the life of listeners of this event easier!

            Structure structure = Objects.requireNonNull(CustomStructures.getInstance().getStructureHandler().getStructure(structureName), "The structure named \"" + structureName + "\" in the loot chest tags was not found in the plugin config.");
            LootTable lootTable = explicitLootTableName.map(s -> Objects.requireNonNull(CustomStructures.getInstance().getLootTableHandler().getLootTableByName(s), "The loot table named \"" + s + "\" in the loot chest tags was not found in the plugin config. ")).orElse(null);
            if (!new LootInventoryOpenEvent(
                event.getPlayer(),
                structure,
                container,
                lootChestTag,
                lootTable
            ).callEvent()) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    /**
     * Generates loot contents for the container from the event.
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void generateLoot(LootInventoryOpenEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage("Spectator does not trigger loot generation.");
            return;
        }

        LootChestTag lootChestTag = event.getLootChestTag();
        if (lootChestTag.shouldRefill(player)) {
            LootChestPopulator.instance().populateContents(player, event.getContainer());
        }
    }

}
