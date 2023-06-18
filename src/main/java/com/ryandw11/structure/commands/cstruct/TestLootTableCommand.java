package com.ryandw11.structure.commands.cstruct;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.commands.SubCommand;
import com.ryandw11.structure.lootchest.LootContentPlacer;
import com.ryandw11.structure.loottables.LootTable;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The Add Item command for the plugin.
 *
 * <p>Permission: customstructures.test.loottable</p>
 *
 * <code>
 * /cstruct testloottable {lootTableName}
 * </code>
 */
public class TestLootTableCommand implements SubCommand {

    private final CustomStructures plugin;

    public TestLootTableCommand(CustomStructures plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean subCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("customstructures.test.loottable")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "You must specify the loot table for the chest to have.");
        } else if (args.length == 1) {
            if (!sender.hasPermission("customstructures.test.loottable")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                return true;
            }
            if (!(sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED + "This command is for players only!");
                return true;
            }
            LootTable lootTable = plugin.getLootTableHandler().getLootTableByName(args[0]);
            if (lootTable == null) {
                sender.sendMessage(ChatColor.RED + "Cannot find specified loot table. Check to make sure that it exists.");
                return true;
            }

            Block block = p.getTargetBlock(null, 20);
            if (!(block.getState() instanceof Container container)) {
                sender.sendMessage(ChatColor.RED + "You must be looking at a container to set its loot table.");
                return true;
            }
            container.getInventory().clear();

            // Put loot contents
            List<ItemStack> items = lootTable.drawAll();
            Inventory inventory = container.getInventory();
            LootContentPlacer.replaceContent(items, inventory);

            sender.sendMessage(ChatColor.GREEN + "The loot table has been applied to the container!");
        }
        return false;
    }

}
