package com.ryandw11.structure.structure;

import com.ryandw11.structure.CustomStructures;
import com.ryandw11.structure.api.structaddon.CustomStructureAddon;
import com.ryandw11.structure.api.structaddon.StructureSection;
import com.ryandw11.structure.api.structaddon.StructureSectionProvider;
import com.ryandw11.structure.exceptions.StructureConfigurationException;
import com.ryandw11.structure.loottables.LootTable;
import com.ryandw11.structure.loottables.LootTableType;
import com.ryandw11.structure.structure.properties.*;
import com.ryandw11.structure.utils.RandomCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * <p>This class is used to make a brand-new Structure.
 * <p>This class is also used internally to load structures from structure config files.
 * <p>You can create a structure completely via code or load a structure from a yaml file.
 * <p>Example using a yaml file:
 * <pre>{@code
 *      StructureBuilder builder = new StructureBuilder("MyName", file);
 *      Structure struct = builder.build();
 * }</pre>
 * <p>Example using code:
 * <pre>{@code
 *      StructureBuilder builder = new StructureBuilder("MyName", file);
 *      builder.setStructureLimitations(new StructureLimitations());
 *      ...
 *      Structure struct = builder.build();
 * }</pre>
 */
public class StructureBuilder {

    private final CustomStructures plugin;
    private FileConfiguration config;

    protected String name;
    protected String schematic;
    protected int probabilityNumerator;
    protected int probabilityDenominator;
    protected int priority;
    protected String compiledSchematic;
    protected boolean isCompiled = false;
    protected StructureLocation structureLocation;
    protected StructureProperties structureProperties;
    protected StructureLimitations structureLimitations;
    protected MaskProperty sourceMaskProperty;
    protected MaskProperty targetMaskProperty;
    protected SubSchematics subSchematics;
    protected AdvancedSubSchematics advancedSubSchematics;
    protected BottomSpaceFill bottomSpaceFill;
    protected Map<LootTableType, RandomCollection<LootTable>> lootTables;
    protected List<StructureSection> structureSections;
    // Base Rotation in Radians.
    protected double baseRotation;

    /**
     * Build a structure using code.
     *
     * @param name      The name of the structure.
     * @param schematic The schematic of the structure.
     */
    public StructureBuilder(String name, String schematic) {
        this(name, schematic, new ArrayList<>());
    }

    /**
     * Build a structure.
     *
     * @param name      The name of the structure.
     * @param schematic The location of the structure schematic file.
     * @param sections  The list of structure sections.
     */
    public StructureBuilder(String name, String schematic, List<StructureSection> sections) {
        this.plugin = CustomStructures.getInstance();
        this.name = name;
        this.schematic = schematic;
        this.priority = 100;
        this.baseRotation = 0;
        this.lootTables = new HashMap<>();
        this.structureSections = sections;
    }

    /**
     * Build a structure.
     *
     * @param name      The name of the structure.
     * @param schematic The location of the structure schematic file.
     * @param sections  The structure sections to add.
     */
    public StructureBuilder(String name, String schematic, StructureSection... sections) {
        this.plugin = CustomStructures.getInstance();
        this.name = name;
        this.schematic = schematic;
        this.priority = 100;
        this.baseRotation = 0;
        this.lootTables = new HashMap<>();
        this.structureSections = Arrays.asList(sections);
    }


    /**
     * Build a structure using a yaml configuration file.
     * <p>No further editing of this class is required if you use this method.</p>
     * <p>Errors are outputted to the console. If an error occurs {@link #build()} will return null.</p>
     *
     * @param name The name of the structure.
     * @param file The file to read from.
     */
    public StructureBuilder(String name, File file) {
        if (!file.exists())
            throw new RuntimeException("Cannot build structure: That file does not exist!");

        this.plugin = CustomStructures.getInstance();
        this.config = YamlConfiguration.loadConfiguration(file);

        this.name = name;
        this.structureSections = new ArrayList<>();

        checkValidity();

        this.schematic = config.getString("Schematic");
        this.probabilityNumerator = config.getInt("Probability.Numerator");
        this.probabilityDenominator = config.getInt("Probability.Denominator");
        this.priority = config.contains("Priority") ? config.getInt("Priority") : 100;
        this.baseRotation = 0;

        if (this.config.contains("CompiledSchematic")) {
            this.isCompiled = CustomStructures.getInstance().getDataFolderPath()
                .resolve("schematics")
                .resolve(Objects.requireNonNull(config.getString("CompiledSchematic")))
                .toFile().exists();
            if (!isCompiled) {
                CustomStructures.getInstance().getLogger().severe("Invalid compiled schematic file for: " + name);
            } else {
                this.compiledSchematic = config.getString("CompiledSchematic");
            }
        }

        this.structureLocation = new StructureLocation(config);
        this.structureProperties = new StructureProperties(config);
        this.structureLimitations = new StructureLimitations(config);
        this.sourceMaskProperty = new MaskProperty("SourceMask", config);
        this.targetMaskProperty = new MaskProperty("TargetMask", config);
        this.subSchematics = new SubSchematics(config, CustomStructures.getInstance());
        this.advancedSubSchematics = new AdvancedSubSchematics(config, CustomStructures.getInstance());
        this.bottomSpaceFill = new BottomSpaceFill(config);

        this.lootTables = new HashMap<>();
        if (config.contains("LootTables")) {
            ConfigurationSection lootTableConfig = config.getConfigurationSection("LootTables");
            setLootTables(Objects.requireNonNull(lootTableConfig)); // Should never throw NPE
        }

        // Go through and set up the sections for the addons.
        for (CustomStructureAddon addon : CustomStructures.getInstance().getAddonHandler().getCustomStructureAddons()) {
            for (StructureSectionProvider provider : addon.getProviderSet()) {
                try {
                    StructureSection section = provider.createSection();
                    if (!config.contains(section.getName())) {
                        section.setupSection(null);
                    } else {
                        section.setupSection(config.getConfigurationSection(section.getName()));
                    }
                    this.structureSections.add(section);
                } catch (StructureConfigurationException ex) {
                    // Handle the structureConfigurationException.
                    throw new StructureConfigurationException("[%s Addon] %s. This is not an issue with the CustomStructures plugin.".formatted(addon.getName(), ex.getMessage()));
                } catch (Throwable ex) {
                    // Inform the user of errors.
                    plugin.getLogger().severe("An error was encountered in the %s addon! Enable debug for more information.".formatted(addon.getName()));
                    plugin.getLogger().severe(ex.getMessage());
                    plugin.getLogger().severe("This is not an issue with CustomStructures! Please contact the addon developer.");
                    if (plugin.isDebug())
                        ex.printStackTrace();
                }
            }

            for (Class<? extends StructureSection> section : addon.getStructureSections()) {
                try {
                    StructureSection constructedSection = section.getConstructor().newInstance();
                    // Check if the section exists in the config file.
                    if (!config.contains(constructedSection.getName())) {
                        constructedSection.setupSection(null);
                    } else {
                        constructedSection.setupSection(config.getConfigurationSection(constructedSection.getName()));
                    }
                    this.structureSections.add(constructedSection);
                } catch (NoSuchMethodException |
                         InstantiationException |
                         IllegalAccessException |
                         InvocationTargetException ex
                ) {
                    // Inform the user of errors.
                    plugin.getLogger().severe("The section %s for the addon %s is configured incorrectly. If you are the developer please refer to the API documentation.".formatted(section.getName(), addon.getName()));
                    plugin.getLogger().severe("This is not an issue with CustomStructures. Report this error to the addon developer!!");
                } catch (StructureConfigurationException ex) {
                    // Handle the structureConfigurationException.
                    throw new StructureConfigurationException("[%s Addon] %s. This is not an issue with the CustomStructures plugin.".formatted(addon.getName(), ex.getMessage()));
                } catch (Exception ex) {
                    // Inform the user of errors.
                    plugin.getLogger().severe("An error was encountered in the %s addon! Enable debug for more information.".formatted(addon.getName()));
                    plugin.getLogger().severe(ex.getMessage());
                    plugin.getLogger().severe("This is not an issue with CustomStructures! Please contact the addon developer.");
                    if (plugin.isDebug()) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private void checkValidity() {
        if (!config.contains("Schematic")) {
            throw new StructureConfigurationException("Invalid structure config: No Schematic found!");
        }
        if (!config.contains("Probability.Numerator")) {
            throw new StructureConfigurationException("Invalid structure config: `Probability.Numerator` is required!");
        }
        if (!config.contains("Probability.Denominator")) {
            throw new StructureConfigurationException("Invalid structure config: `Probability.Denominator` is required!");
        }
        if (!config.isInt("Probability.Numerator") || config.getInt("Probability.Numerator") < 1) {
            throw new StructureConfigurationException("Invalid structure config: `Probability.Numerator` must be a number cannot be less than 1!");
        }
        if (!config.isInt("Probability.Denominator") || config.getInt("Probability.Denominator") < 1) {
            throw new StructureConfigurationException("Invalid structure config: `Probability.Denominator` must be a number cannot be less than 1!");
        }
    }

    /**
     * Set the probability of the structure spawning.
     * <p>How many times (numerator) a structure should spawn per x (denominator) chunks.</p>
     *
     * @param numerator   The numerator of the probability fraction.
     * @param denominator The denominator of the probability fraction.
     */
    public void setProbability(int numerator, int denominator) {
        this.probabilityNumerator = numerator;
        this.probabilityDenominator = denominator;
    }

    /**
     * Set the priority of the structure.
     * <p>The lower the number, the greater the priority.</p>
     *
     * @param priority The priority of the structure. (Default 100).
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Set the compiled schematic.
     * <p>This will automatically set isCompiled to true if the file is found.</p>
     *
     * @param cschem The compiled schematic name. (Include the .cschem)
     *               <p>This file MUST be in the schematics folder.</p>
     *               <p>An IllegalArgumentException is thrown when the file is not found.</p>
     */
    public void setCompiledSchematic(String cschem) {
        if (!CustomStructures.getInstance().getDataFolderPath().resolve("schematics").resolve(cschem).toFile().exists())
            throw new IllegalArgumentException("Compiled Schem File not found!");
        this.compiledSchematic = cschem;
        this.isCompiled = true;
    }

    /**
     * Set the structure limitations.
     *
     * @param limitations The structure limitations.
     */
    public void setStructureLimitations(StructureLimitations limitations) {
        this.structureLimitations = limitations;
    }

    /**
     * Set the structure properties.
     *
     * @param properties The structure properties.
     */
    public void setStructureProperties(StructureProperties properties) {
        this.structureProperties = properties;
    }

    /**
     * Set the structure location.
     *
     * @param location The structure location.
     */
    public void setStructureLocation(StructureLocation location) {
        this.structureLocation = location;
    }

    /**
     * Set the source mask property.
     *
     * @param mask The source mask property.
     */
    public void setSourceMaskProperty(MaskProperty mask) {
        this.sourceMaskProperty = mask;
    }

    /**
     * Set the target mask property.
     *
     * @param mask The target mask property.
     */
    public void setTargetMaskProperty(MaskProperty mask) {
        this.targetMaskProperty = mask;
    }

    /**
     * Set the bottom space fill property.
     *
     * @param bottomSpaceFill The bottom space fill property.
     */
    public void setBottomSpaceFill(BottomSpaceFill bottomSpaceFill) {
        this.bottomSpaceFill = bottomSpaceFill;
    }

    /**
     * Set the (simple) sub-schematic property.
     *
     * @param subSchematics The sub-schematic property.
     */
    public void setSubSchematics(SubSchematics subSchematics) {
        this.subSchematics = subSchematics;
    }

    /**
     * Set the advanced sub-schematic property.
     *
     * @param advancedSubSchematics The advanced sub-schematic property.
     */
    public void setAdvancedSubSchematics(AdvancedSubSchematics advancedSubSchematics) {
        this.advancedSubSchematics = advancedSubSchematics;
    }

    /**
     * Set the loot tables from a configuration section.
     *
     * @param lootTableConfig The loot table configuration section.
     */
    public void setLootTables(ConfigurationSection lootTableConfig) {
        lootTables = new HashMap<>();
        for (String lootTableType : lootTableConfig.getKeys(false)) {
            if (!LootTableType.exists(lootTableType))
                continue;

            LootTableType type = LootTableType.valueOf(lootTableType.toUpperCase());

            // Loop through the new loot table section.
            for (String lootTableName : Objects.requireNonNull(lootTableConfig.getConfigurationSection(lootTableType)).getKeys(false)) {
                int weight = lootTableConfig.getInt(lootTableType + "." + lootTableName);
                LootTable table = CustomStructures.getInstance().getLootTableHandler().getLootTableByName(lootTableName);
                Objects.requireNonNull(table, "The loot table named \"" + lootTableName + "\" was not found in the plugin config.");
                lootTables.computeIfAbsent(type, k -> new RandomCollection<>()).add(weight, table);
            }
        }
    }

    /**
     * Set the loot tables using a collection of LootTable.
     *
     * @param lootTables The collection of LootTables.
     */
    public void setLootTables(Map<LootTableType, RandomCollection<LootTable>> lootTables) {
        this.lootTables = lootTables;
    }

    /**
     * Add a loot table to the structure.
     *
     * @param type      the container type which the loot table is bound to.
     * @param lootTable The loot table to add.
     * @param weight    The weight.
     */
    public void addLootTable(LootTableType type, LootTable lootTable, double weight) {
        lootTables.computeIfAbsent(type, k -> new RandomCollection<>()).add(weight, lootTable);
    }

    /**
     * Set the base rotation of a structure.
     *
     * <p>This is an API only functionality. It sets what the structure should be rotated by, while still allowing
     * for random rotation is desired.</p>
     *
     * @param baseRotation The base rotation of a structure. (In Radians.)
     */
    public void setBaseRotation(double baseRotation) {
        this.baseRotation = baseRotation;
    }

    /**
     * Add a structure section to the structure builder.
     * <p>Note: {@link StructureSection#setupSection(ConfigurationSection)} is NOT called by this method. You are
     * expected to use a constructor.</p>
     *
     * @param structureSection The structure section to add.
     */
    public void addStructureSection(StructureSection structureSection) {
        this.structureSections.add(structureSection);
    }

    /**
     * Build the structure.
     * <p>Note: This does not check to see if all values are set. If any of the properties are not set
     * than a NullPointerException will occur.</p>
     *
     * @return The structure.
     */
    public Structure build() {
        Objects.requireNonNull(name, "The structure name cannot be null.");
        Objects.requireNonNull(schematic, "The structure schematic cannot be null.");
        Objects.requireNonNull(structureLocation, "The structure location cannot be null.");
        Objects.requireNonNull(structureProperties, "The structure property cannot be null.");
        Objects.requireNonNull(structureLimitations, "The structure limitations cannot be null.");
        Objects.requireNonNull(sourceMaskProperty, "The structure source mask property cannot be null.");
        Objects.requireNonNull(targetMaskProperty, "The structure target mask property cannot be null.");
        Objects.requireNonNull(subSchematics, "The structure sub-schematic property cannot be null.");
        Objects.requireNonNull(advancedSubSchematics, "The structure advanced sub-schematic property cannot be null.");
        Objects.requireNonNull(bottomSpaceFill, "The structure bottom space fill property cannot be null.");
        Objects.requireNonNull(lootTables, "The structure loot tables cannot be null.");
        Objects.requireNonNull(structureSections, "The structure sections list cannot be null.");
        return new Structure(this);
    }

    /**
     * Save the structure as a structure configuration file.
     * <p>This automatically saves the file in the structures folder.</p>
     *
     * @param file The file to save.
     * @throws IOException If an IO Exception occurs.
     */
    public void save(File file) throws IOException {
        file.createNewFile();
        config = YamlConfiguration.loadConfiguration(file);
        config.set("Schematic", schematic);
        config.set("Probability.Numerator", probabilityNumerator);
        config.set("Probability.Denominator", probabilityDenominator);

        config.set("StructureLocation.Worlds", structureLocation.getWorlds());
        config.set("StructureLocation.SpawnY", structureLocation.getSpawnSettings().getValue());
        config.set("StructureLocation.SpawnYHeightMap", structureLocation.getSpawnSettings().getHeightMap().toString());
        config.set("StructureLocation.Biome", structureLocation.getBiomes());

        config.set("StructureProperties.PlaceAir", structureProperties.canPlaceAir());
        config.set("StructureProperties.RandomRotation", structureProperties.isRandomRotation());
        config.set("StructureProperties.IgnorePlants", structureProperties.isIgnoringPlants());
        config.set("StructureProperties.SpawnInWater", structureProperties.canSpawnInWater());
        config.set("StructureProperties.SpawnInLavaLakes", structureProperties.canSpawnInLavaLakes());

        config.set("StructureLimitations.WhitelistSpawnBlocks", structureLimitations.getWhitelistBlocks());

        if (isCompiled)
            config.set("CompiledSchematic", compiledSchematic);

        for (Map.Entry<LootTableType, RandomCollection<LootTable>> loot : lootTables.entrySet()) {
            for (Map.Entry<Double, LootTable> entry : loot.getValue().getMap().entrySet()) {
                config.set("LootTables." + loot.getKey().toString() + "." + entry.getValue().name(), entry.getKey());
            }
        }
        config.save(file);
    }

}
