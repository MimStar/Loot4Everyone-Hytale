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

        if (!defaultWorld.getName().equals(event.getWorld().getName())) {
            LootChestConfig default_lootChestConfig = defaultWorld.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
            LootChestConfig lootChestConfig = event.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            lootChestConfig.setNextLootResetInterval(0);
            lootChestConfig.setNextLootReset(-1);
            lootChestConfig.setLootRandom(default_lootChestConfig.isLootChestRandom());
            lootChestConfig.setCanPlayerBreakLootChests(default_lootChestConfig.isCanPlayerBreakLootChests());
            lootChestConfig.setParticlesAppear(default_lootChestConfig.isParticlesAppear());
            lootChestConfig.setParticlesColor(default_lootChestConfig.getParticlesColor());
            lootChestConfig.setMessageAppear(default_lootChestConfig.isMessageAppear());
        }
    }
}
