package com.ryandw11.structure.lootchest;

import com.google.gson.Gson;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * A custom {@link PersistentDataType} of {@link LootChestTag}.
 */
public final class LootChestTagType implements PersistentDataType<String, LootChestTag> {

    private static final Gson GSON = new Gson();

    public static final LootChestTagType INSTANCE = new LootChestTagType();

    @NotNull
    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @NotNull
    @Override
    public Class<LootChestTag> getComplexType() {
        return LootChestTag.class;
    }

    @NotNull
    @Override
    public String toPrimitive(@NotNull LootChestTag complex, @NotNull PersistentDataAdapterContext context) {
        return GSON.toJson(complex);
    }

    @NotNull
    @Override
    public LootChestTag fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return GSON.fromJson(primitive, LootChestTagImpl.class);
    }

}
