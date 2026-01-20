package org.mimstar.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class LootChestConfig implements Resource<ChunkStore> {

    public static final BuilderCodec<LootChestConfig> CODEC = BuilderCodec.builder(
                    LootChestConfig.class,
                    LootChestConfig::new
            )
            .addField(new KeyedCodec<>("CanPlayerBreakLootChests", Codec.BOOLEAN)
                    ,(data, value) -> data.canPlayerBreakLootChests = value,
                    data -> data.canPlayerBreakLootChests)
            .build();

    private boolean canPlayerBreakLootChests;

    public LootChestConfig(){
        this.canPlayerBreakLootChests = false;
    }

    public LootChestConfig(LootChestConfig other){
        this.canPlayerBreakLootChests = other.canPlayerBreakLootChests;
    }

    @Nullable
    @Override
    public Resource<ChunkStore> clone() {
        return new LootChestConfig(this);
    }

    public boolean isCanPlayerBreakLootChests(){
        return canPlayerBreakLootChests;
    }

    public void setCanPlayerBreakLootChests(boolean new_value){
        canPlayerBreakLootChests = new_value;
    }
}
