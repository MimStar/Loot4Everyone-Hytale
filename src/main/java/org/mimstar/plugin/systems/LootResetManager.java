package org.mimstar.plugin.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.PlayerLoot;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class LootResetManager {

    /**
     * Resets all loot chests for all players (Online and Offline).
     * Runs synchronously.
     */
    public static void runGlobalReset(Store<EntityStore> store) {

        PlayerStorage storage = Universe.get().getPlayerStorage();
        Set<UUID> allPlayerUuids;

        try {
            allPlayerUuids = storage.getPlayers();
        } catch (IOException e) {
            Loot4Everyone.LOGGER.atSevere().log(e.getMessage());
            return;
        }

        Consumer<PlayerLoot> resetAction = PlayerLoot::resetAllChests;

        for (UUID uuid : allPlayerUuids) {
            PlayerRef targetRef = Universe.get().getPlayer(uuid);

            // 1. Process Online Players (Fast, In-Memory)
            if (targetRef != null && targetRef.isValid()) {
                Ref<EntityStore> targetEntityRef = targetRef.getReference();
                if (targetEntityRef != null && targetEntityRef.isValid()) {
                    PlayerLoot component = store.getComponent(targetEntityRef, Loot4Everyone.get().getPlayerLootcomponentType());
                    if (component != null) {
                        resetAction.accept(component);
                    }
                }
            }
            // 2. Process Offline Players (Synchronous Load/Save)
            else {
                try {
                    var holder = storage.load(uuid).join();

                    if (holder != null) {
                        PlayerLoot loot = holder.getComponent(Loot4Everyone.get().getPlayerLootcomponentType());
                        if (loot != null) {
                            resetAction.accept(loot);
                            storage.save(uuid, holder,true).join();
                        }
                    }
                } catch (Exception e) {
                    Loot4Everyone.LOGGER.atSevere().log("Error resetting loot for offline player " + uuid + ": " + e.getMessage());
                }
            }
        }
    }
}