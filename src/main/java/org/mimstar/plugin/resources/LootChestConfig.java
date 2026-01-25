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
            .addField(new KeyedCodec<>("IsMessageAppear", Codec.BOOLEAN)
                    ,(data, value) -> data.isMessageAppear = value,
                    data -> data.isMessageAppear)
            .build();

    private boolean canPlayerBreakLootChests;
    private boolean isLootRandom;
    private boolean isMessageAppear;

    public LootChestConfig(){
        this.canPlayerBreakLootChests = false;
        this.isLootRandom = true;
        this.isMessageAppear = true;
    }

    public LootChestConfig(LootChestConfig other){
        this.canPlayerBreakLootChests = other.canPlayerBreakLootChests;
        this.isLootRandom = other.isLootRandom;
        this.isMessageAppear = other.isMessageAppear;
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

    public boolean isMessageAppear(){
        return isMessageAppear;
    }

    public void setMessageAppear(boolean new_value){
        isMessageAppear = new_value;
    }
}
