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
            .addField(new KeyedCodec<>("IsParticlesAppear", Codec.BOOLEAN)
                    ,(data, value) -> data.isParticlesAppear = value,
                    data -> data.isParticlesAppear)
            .addField(new KeyedCodec<>("ParticlesColor", Codec.STRING)
                    ,(data, value) -> data.particlesColor = value,
                    data -> data.particlesColor)
            .addField(new KeyedCodec<>("LootResetMode", Codec.INTEGER)
                    ,(data, value ) -> data.lootResetMode = value,
                    data -> data.lootResetMode)
            .addField(new KeyedCodec<>("NextLootResetDaysInterval", Codec.INTEGER)
                    ,(data, value ) -> data.nextLootResetDaysInterval = value,
                    data -> data.nextLootResetDaysInterval)
            .addField(new KeyedCodec<>("NextLootResetHoursInterval", Codec.INTEGER)
                    ,(data, value ) -> data.nextLootResetHoursInterval = value,
                    data -> data.nextLootResetHoursInterval)
            .addField(new KeyedCodec<>("NextLootResetMinutesInterval", Codec.INTEGER)
                    ,(data, value ) -> data.nextLootResetMinutesInterval = value,
                    data -> data.nextLootResetMinutesInterval)
            .addField(new KeyedCodec<>("NextLootResetSecondsInterval", Codec.INTEGER)
                    ,(data, value ) -> data.nextLootResetSecondsInterval = value,
                    data -> data.nextLootResetSecondsInterval)
            .addField(new KeyedCodec<>("NextLootReset", Codec.LONG)
                    ,(data, value ) -> data.nextLootReset = value,
                    data -> data.nextLootReset)
            .build();

    private boolean canPlayerBreakLootChests;
    private boolean isLootRandom;
    private boolean isMessageAppear;
    private boolean isParticlesAppear;
    private String particlesColor;
    private int lootResetMode;
    private int nextLootResetDaysInterval;
    private int nextLootResetHoursInterval;
    private int nextLootResetMinutesInterval;
    private int nextLootResetSecondsInterval;
    private long nextLootReset;

    public LootChestConfig(){
        this.canPlayerBreakLootChests = false;
        this.isLootRandom = true;
        this.isMessageAppear = true;
        this.isParticlesAppear = true;
        this.particlesColor = "#ffffff00";
        this.lootResetMode = 0;
        this.nextLootResetDaysInterval = 0;
        this.nextLootResetHoursInterval = 0;
        this.nextLootResetMinutesInterval = 0;
        this.nextLootResetSecondsInterval = 0;
        this.nextLootReset = -1;
    }

    public LootChestConfig(LootChestConfig other){
        this.canPlayerBreakLootChests = other.canPlayerBreakLootChests;
        this.isLootRandom = other.isLootRandom;
        this.isMessageAppear = other.isMessageAppear;
        this.isParticlesAppear = other.isParticlesAppear;
        this.particlesColor = other.particlesColor;
        this.lootResetMode = other.lootResetMode;
        this.nextLootResetDaysInterval = other.nextLootResetDaysInterval;
        this.nextLootResetHoursInterval = other.nextLootResetHoursInterval;
        this.nextLootResetMinutesInterval = other.nextLootResetMinutesInterval;
        this.nextLootResetSecondsInterval = other.nextLootResetSecondsInterval;
        this.nextLootReset = other.nextLootReset;
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

    public boolean isParticlesAppear(){ return isParticlesAppear; }

    public void setParticlesAppear(boolean new_value){ isParticlesAppear = new_value; }

    public String getParticlesColor(){ return particlesColor; }

    public void setParticlesColor(String new_value) { particlesColor = new_value; }

    public int getLootResetMode() { return lootResetMode; }

    public void setLootResetMode(int new_value) { this.lootResetMode = new_value; }

    public int getNextLootResetDaysInterval() {
        return nextLootResetDaysInterval;
    }

    public void setNextLootResetDaysInterval(int new_value) {
        this.nextLootResetDaysInterval = new_value;
    }

    public int getNextLootResetHoursInterval() {
        return nextLootResetHoursInterval;
    }

    public void setNextLootResetHoursInterval(int new_value) {
        this.nextLootResetHoursInterval = new_value;
    }

    public int getNextLootResetMinutesInterval() {
        return nextLootResetMinutesInterval;
    }

    public void setNextLootResetMinutesInterval(int new_value) {
        this.nextLootResetMinutesInterval = new_value;
    }

    public int getNextLootResetSecondsInterval() {
        return nextLootResetSecondsInterval;
    }

    public void setNextLootResetSecondsInterval(int new_value) {
        this.nextLootResetSecondsInterval = new_value;
    }

    public long getNextLootReset() {
        return nextLootReset;
    }

    public void setNextLootReset(long new_value) {
        this.nextLootReset = new_value;
    }
}
