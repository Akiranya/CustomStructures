package com.ryandw11.structure.lootchest;

import org.bukkit.persistence.PersistentDataContainer;
import com.ryandw11.structure.structure.Structure;
import org.bukkit.block.Container;

/**
 * <p>This class manages the content creation of loot containers in structures.
 *
 * <p>The general notion of the loot generation system is that: <b>loots are generated upon a player opening the
 * containers, rather than being generated upon structure generation.</b> This notion is pretty much like the vanilla
 * loot chest system. The advantage of this notion is that we can "modify" the loot contents even if the structure is
 * already generated. Furthermore, the loot contents will be always "fresh" if the loot contents contain "Custom Items"
 * which may/should vary between different players. Other cool features are also easily to implement such as "auto
 * refilling content" and "unbreakable loot containers" since all loot containers are identifiable by reading the
 * {@link PersistentDataContainer} of containers.
 *
 * <p>To implement this, we first store specific tags in the containers during structure generation (see
 * {@link LootChestTag} for the details of tags). Then, we populate the containers with specific contents depending on
 * the previously stored tags whenever a player opens it.
 */
public interface LootChestPopulator {

    static LootChestPopulator instance() {
        return new LootChestPopulatorImpl();
    }

    /**
     * Writes tags (i.e. {@link LootChestTag}) into given container. The tags should contain necessary information for
     * the desired loot table to be populated in the container when calling
     * {@link #populateContents(org.bukkit.block.Container)}.
     *
     * @param structure the structure the given container belongs to
     * @param container the container to write tag in
     * @see #populateContents(Container)
     */
    void writeTags(Structure structure, Container container);

    /**
     * Replaces the contents of a container with a loot table from a structure. The contents to be populated depend on
     * the loot chest tags ({@link LootChestTag}) stored in the container. The contents of given container will not be
     * modified if there is no LootChestTag stored in the container.
     *
     * @param container the container to populate contents in
     * @see #writeTags(Structure, Container)
     */
    void populateContents(Container container);

}
