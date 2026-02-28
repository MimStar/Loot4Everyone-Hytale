package org.mimstar.plugin.events;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;

public class StartWorldEventListener {

    public static void onStartWorldEvent(StartWorldEvent event){
        World defaultWorld = Universe.get().getDefaultWorld();

        if (defaultWorld == null){
            return;
        }

        if (!defaultWorld.isAlive()){
            return;
        }

        if (!defaultWorld.getName().equals(event.getWorld().getName())) {
            LootChestConfig defaultLootChestConfig = defaultWorld.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
            LootChestConfig lootChestConfig = event.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            lootChestConfig.setNextLootResetInterval(0);
            lootChestConfig.setNextLootReset(-1);
            lootChestConfig.setLootRandom(defaultLootChestConfig.isLootChestRandom());
            lootChestConfig.setCanPlayerBreakLootChests(defaultLootChestConfig.isCanPlayerBreakLootChests());
            lootChestConfig.setParticlesAppear(defaultLootChestConfig.isParticlesAppear());
            lootChestConfig.setParticlesColor(defaultLootChestConfig.getParticlesColor());
            lootChestConfig.setMessageAppear(defaultLootChestConfig.isMessageAppear());
        }
    }
}
