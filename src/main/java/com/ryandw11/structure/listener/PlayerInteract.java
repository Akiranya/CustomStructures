package com.ryandw11.structure.listener;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.OpenLootContainerEvent;
import com.ryandw11.structure.lootchest.LootChestConstant;
import com.ryandw11.structure.lootchest.LootChestPopulator;
import com.ryandw11.structure.lootchest.LootChestTag;
import com.ryandw11.structure.lootchest.LootChestTagType;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class PlayerInteract implements Listener {

    /**
     * <p><b>FOR INTERNAL USE.</b>
     *
     * <p>It is used to fire an {@link com.ryandw11.structure.api.OpenLootContainerEvent} when a player opens a loot
     * container of custom structure.
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
            @Nullable LootChestTag lootChestTag = container.getPersistentDataContainer().get(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE);
            if (lootChestTag == null) { // This container is not from a custom structure
                return;
            }

            // Raw information in the tags
            String structureName = lootChestTag.getStructureName();
            Optional<String> explicitLootTableName = lootChestTag.getExplicitLootTableName();

            // Fire event (to let other code handle the loot generation)
            Structure structure = Objects.requireNonNull(CustomStructures.getInstance().getStructureHandler().getStructure(structureName),
                    "The structure named \"" + structureName + "\" in the loot chest tags was not found in the plugin config. " +
                    "This error could occur if you changed the filename of structure yml after the structure was generated in the world. " +
                    "Regenerating the world should fix this issue.");
            LootTable lootTable = explicitLootTableName.map(s -> CustomStructures.getInstance().getLootTableHandler().getLootTableByName(s)).orElse(null);
            OpenLootContainerEvent openLootContainerEvent = new OpenLootContainerEvent(
                    event.getPlayer(),
                    structure,
                    container,
                    lootChestTag,
                    lootTable);
            Bukkit.getServer().getPluginManager().callEvent(openLootContainerEvent);
            if (openLootContainerEvent.isCancelled()) {
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
    public void generateLoot(OpenLootContainerEvent event) {
        LootChestTag lootChestTag = event.getLootChestTag();
        if (lootChestTag.shouldRefill(event.getPlayer())) {
            LootChestPopulator.instance().populateContents(event.getContainer());
            lootChestTag.processRefill(event.getPlayer());

            // Store the updated tags back
            event.getContainer().getPersistentDataContainer().set(LootChestConstant.LOOT_CHEST, LootChestTagType.INSTANCE, lootChestTag);
        }
    }

}
