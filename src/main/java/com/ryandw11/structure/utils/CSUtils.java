package com.ryandw11.structure.utils;

import com.ryandw11.structure.structure.Structure;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.UUID;

/**
 * General utilities for Custom Structures.
 */
public class CSUtils {
    /**
     * Replace the placeholders on commands in the command group.
     *
     * @param command      The command.
     * @param signLocation The location of the sign.
     * @param minLoc       The minimum location of the structure.
     * @param maxLoc       The maximum location of the structure.
     * @param structure    The structure.
     * @return The command with the placeholders replaced.
     */
    public static String replacePlaceHolders(String command, Location signLocation, Location minLoc, Location maxLoc, Structure structure) {
        return command
            .replace("<world>", Objects.requireNonNull(signLocation.getWorld()).getName())
            .replace("<x>", String.valueOf(signLocation.getBlockX()))
            .replace("<y>", String.valueOf(signLocation.getBlockY()))
            .replace("<z>", String.valueOf(signLocation.getBlockZ()))
            .replace("<structX1>", String.valueOf(minLoc.getBlockX()))
            .replace("<structY1>", String.valueOf(minLoc.getBlockY()))
            .replace("<structZ1>", String.valueOf(minLoc.getBlockZ()))
            .replace("<structX2>", String.valueOf(maxLoc.getBlockX()))
            .replace("<structY2>", String.valueOf(maxLoc.getBlockY()))
            .replace("<structZ2>", String.valueOf(maxLoc.getBlockZ()))
            .replace("<minX>", String.valueOf(minLoc.getBlockX()))
            .replace("<minY>", String.valueOf(minLoc.getBlockY()))
            .replace("<minZ>", String.valueOf(minLoc.getBlockZ()))
            .replace("<maxX>", String.valueOf(maxLoc.getBlockX()))
            .replace("<maxY>", String.valueOf(maxLoc.getBlockY()))
            .replace("<maxZ>", String.valueOf(maxLoc.getBlockZ()))
            .replace("<uuid>", UUID.randomUUID().toString())
            .replace("<structName>", structure.getName());
    }

    public static void renameConfigString(ConfigurationSection configurationSection, String originalName, String newName) {
        if (configurationSection.contains(originalName)) {
            configurationSection.set(newName, configurationSection.getString(originalName));
            configurationSection.set(originalName, null);
        }
    }

    public static void renameConfigBoolean(ConfigurationSection configurationSection, String originalName, String newName) {
        if (configurationSection.contains(originalName)) {
            configurationSection.set(newName, configurationSection.getBoolean(originalName));
            configurationSection.set(originalName, null);
        }
    }

    public static void renameConfigInteger(ConfigurationSection configurationSection, String originalName, String newName) {
        if (configurationSection.contains(originalName)) {
            configurationSection.set(newName, configurationSection.getInt(originalName));
            configurationSection.set(originalName, null);
        }
    }

    public static void renameConfigStringList(ConfigurationSection configurationSection, String originalName, String newName) {
        if (configurationSection.contains(originalName)) {
            configurationSection.set(newName, configurationSection.getStringList(originalName));
            configurationSection.set(originalName, null);
        }
    }

    public static void renameStringConfigurationSection(ConfigurationSection configurationSection, String originalName, String newName) {
        if (!configurationSection.contains(originalName)) return;

        for (String key : configurationSection.getKeys(false)) {
            configurationSection.set(newName + "." + key, configurationSection.getString(originalName + "." + key));
        }
        configurationSection.set(originalName, null);
    }

    /**
     * Check if an integer is in a range.
     *
     * @param pair  The range.
     * @param value The integer.
     * @return If the integer is in the range.
     */
    public static boolean isPairInRange(Pair<Integer, Integer> pair, int value) {
        if (pair.getLeft() > value) return false;
        return pair.getRight() > value;
    }

    /**
     * Check if an integer is in a local range.
     *
     * @param pair     The local range.
     * @param localPin The pin that will be added to the local range.
     * @param value    The integer.
     * @return If the integer is within the local range.
     */
    public static boolean isPairInLocalRange(Pair<Integer, Integer> pair, int localPin, int value) {
        if (pair.getLeft() + localPin > value) return false;
        return pair.getRight() + localPin > value;
    }
}
