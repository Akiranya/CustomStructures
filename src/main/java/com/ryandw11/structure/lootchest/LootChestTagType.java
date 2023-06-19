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

    @Override public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override public @NotNull Class<LootChestTag> getComplexType() {
        return LootChestTag.class;
    }

    @Override public @NotNull String toPrimitive(@NotNull LootChestTag complex, @NotNull PersistentDataAdapterContext context) {
        return GSON.toJson(complex);
    }

    @Override public @NotNull LootChestTag fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return GSON.fromJson(primitive, LootChestTagImpl.class);
    }

}
