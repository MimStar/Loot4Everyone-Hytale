package org.mimstar.plugin.resources;

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
            .addField(new KeyedCodec<>("IsLootRandom", Codec.BOOLEAN)
                    ,(data, value) -> data.isLootRandom = value,
                    data -> data.isLootRandom)
            .build();

    private boolean canPlayerBreakLootChests;
    private boolean isLootRandom;

    public LootChestConfig(){
        this.canPlayerBreakLootChests = false;
        this.isLootRandom = true;
    }

    public LootChestConfig(LootChestConfig other){
        this.canPlayerBreakLootChests = other.canPlayerBreakLootChests;
        this.isLootRandom = other.isLootRandom;
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

    public boolean isLootChestRandom(){
        return isLootRandom;
    }

    public void setLootRandom(boolean new_value){
        isLootRandom = new_value;
    }
}
