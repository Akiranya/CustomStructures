package com.ryandw11.structure.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.SchematicHandeler;
import com.ryandw11.structure.api.CustomStructuresAPI;
import com.sk89q.worldedit.WorldEditException;

/**
 * This class prevents the server from crashing when it attempts to pick a structure.
 * <p>The server will still lag a bit thanks to the nature of 1.14.</p>
 * @author Ryandw11
 *
 */
public class StructurePicker extends BukkitRunnable {
	
	private CustomStructures plugin;
	private CustomStructuresAPI api;
	
	private Random r;
	private int count;
	private String currentSchem;
	private int numberOfSchem;
	
	private Block bl;
	private Chunk ch;
	
	public StructurePicker (Block bl, Chunk ch){
		this.plugin = CustomStructures.plugin;
		r = new Random();
		count = 0;
		api = new CustomStructuresAPI();
		numberOfSchem = api.getNumberOfStructures();
		this.bl = bl;
		this.ch = ch;
	}

	@Override
	public void run() {
		if(count >= numberOfSchem) {
			this.cancel();
			return;
		}
		
		currentSchem = plugin.structures.get(count);
		
		//Calculate the chance.
		int num = r.nextInt(plugin.getConfig().getInt("Schematics." + currentSchem + ".Chance.OutOf") - 1) + 1;
		if(num <= plugin.getConfig().getInt("Schematics." + currentSchem + ".Chance.Number")){
			if(!plugin.getConfig().getBoolean("Schematics." + currentSchem + ".AllWorlds")){ // Checking to see if the world is correct
				@SuppressWarnings("unchecked")
				ArrayList<String> worlds = (ArrayList<String>) plugin.getConfig().get("Schematics." + currentSchem + ".AllowedWorlds");
				if(!worlds.contains(bl.getWorld().getName()))
					return;
			}
		
			ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Schematics." + currentSchem);
		
		if(!plugin.getConfig().getString("Schematics." + currentSchem + ".Biome").equalsIgnoreCase("all")){//Checking biome
			if(!getBiomes(plugin.getConfig().getString("Schematics." + currentSchem + ".Biome").toLowerCase()).contains(bl.getBiome().toString().toLowerCase()))
				return;
		}
		if(cs.getInt("SpawnY") < -1){
			bl = ch.getBlock(0, (bl.getY() + plugin.getConfig().getInt("Schematics." + currentSchem + ".SpawnY")) , 0);
		}
		else if(cs.contains("SpawnY") && cs.getInt("SpawnY") != -1){
			bl = ch.getBlock(0, cs.getInt("SpawnY"), 0);
		}
		if(!cs.getBoolean("spawnInLiquid")) {
			if(bl.getType() == Material.WATER || bl.getType() == Material.LAVA) 
				return; 
		}
		//Now to finally paste the schematic
		SchematicHandeler sh = new SchematicHandeler();
		try {
			sh.schemHandle(bl.getLocation(), plugin.getConfig().getString("Schematics." + currentSchem + ".Schematic"), plugin.getConfig().getBoolean("Schematics." + currentSchem + ".PlaceAir"));
		} catch (IOException | WorldEditException e) {
			e.printStackTrace();
		}
		this.cancel();// return after pasting
	}
		
		count++;
	}
	
	protected ArrayList<String> getBiomes(String s){
		String[] biomes = s.split(",");
		ArrayList<String> output = new ArrayList<String>();
		for(String b : biomes)
			output.add(b);
		return output;
	}

}
