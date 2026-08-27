package org.mimstar.plugin.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3i;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.PlayerLoot;
import org.mimstar.plugin.resources.LootChestConfig;
import org.mimstar.plugin.resources.LootChestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BreakBlockEventListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final int BATCH_SIZE = 20;

    public BreakBlockEventListener() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl BreakBlockEvent breakBlockEvent) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;

        Vector3i target = breakBlockEvent.getTargetBlock();
        int x = target.x(), y = target.y(), z = target.z();
        World world = player.getWorld();

        LootChestTemplate lootChestTemplate = world.getChunkStore().getStore()
                .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
        boolean targetIsLootChest = lootChestTemplate.hasTemplate(x, y, z);

        Vector3i[] positionsToCheck = {
                new Vector3i(x, y, z),
                new Vector3i(x, y + 1, z),
                new Vector3i(x + 1, y + 1, z),
                new Vector3i(x - 1, y + 1, z),
                new Vector3i(x, y + 1, z + 1),
                new Vector3i(x, y + 1, z - 1)
        };

        boolean shouldCancel = false;

        for (Vector3i pos : positionsToCheck) {
            if (isProtectedChest(player, pos.x(), pos.y(), pos.z())) {
                LootChestConfig lootChestConfig = world.getChunkStore().getStore()
                        .getResource(Loot4Everyone.get().getLootChestConfigResourceType());
                ChunkStore chunkStore = world.getChunkStore();
                Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, pos.x(), pos.y(), pos.z());
                if (blockRef == null) continue;

                ItemContainerBlock itemContainerState = chunkStore.getStore()
                        .getComponent(blockRef, ItemContainerBlock.getComponentType());

                if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()) {
                    continue;
                } else {
                    shouldCancel = true;
                    break;
                }
            }
        }

        if (shouldCancel) {
            breakBlockEvent.setCancelled(true);
            return;
        }

        if (targetIsLootChest) {
            removeLootChest(world, target, store);
        }
    }

    /**
     * Helper method to check if a specific block coordinate contains a protected Loot Chest.
     */
    private boolean isProtectedChest(Player player, int x, int y, int z) {
        World world = player.getWorld();
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, x, y, z);
        if (blockRef == null) return false;

        ItemContainerBlock itemContainerState = chunkStore.getStore()
                .getComponent(blockRef, ItemContainerBlock.getComponentType());

        if (itemContainerState != null) {
            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore()
                    .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            return lootChestTemplate.hasTemplate(x, y, z);
        }
        return false;
    }

    /**
     * Removes the loot chest template and cleans up player data for the given block.
     */
    private void removeLootChest(World world, Vector3i blockPos, Store<EntityStore> store) {
        LootChestTemplate lootChestTemplate = world.getChunkStore().getStore()
                .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
        lootChestTemplate.removeTemplate(blockPos.x(), blockPos.y(), blockPos.z());

        cleanupPlayerData(blockPos, world.getName(), store);
    }

    /**
     * Cleans up the PlayerLoot component for all players (online and offline).
     * This mirrors the logic from DeleteLootChestCommand but without sending messages.
     */
    private void cleanupPlayerData(Vector3i targetBlock, String worldName, Store<EntityStore> store) {
        PlayerStorage storage = Universe.get().getPlayerStorage();
        Set<UUID> allPlayers;
        try {
            allPlayers = storage.getPlayers();
        } catch (IOException e) {
            Loot4Everyone.LOGGER.atSevere().log("Error retrieving player list for cleanup: " + e.getMessage());
            return;
        }

        Consumer<PlayerLoot> resetAction = (playerLoot) -> {
            if (!playerLoot.isFirstTime(targetBlock.x(), targetBlock.y(), targetBlock.z(), worldName)) {
                playerLoot.resetChest(targetBlock.x(), targetBlock.y(), targetBlock.z(), worldName);
            }
        };

        List<UUID> offlinePlayers = new ArrayList<>();

        for (UUID uuid : allPlayers) {
            PlayerRef targetRef = Universe.get().getPlayer(uuid);
            if (targetRef != null && targetRef.isValid()) {
                Ref<EntityStore> targetEntityRef = targetRef.getReference();
                if (targetEntityRef != null && targetEntityRef.isValid()) {
                    PlayerLoot component = store.getComponent(targetEntityRef,
                            Loot4Everyone.get().getPlayerLootcomponentType());
                    if (component != null) {
                        resetAction.accept(component);
                    }
                }
            } else {
                offlinePlayers.add(uuid);
            }
        }

        if (!offlinePlayers.isEmpty()) {
            AtomicInteger processedCount = new AtomicInteger(0);
            recursiveBatchProcess(offlinePlayers.iterator(), storage, resetAction, processedCount);
        }
    }

    /**
     * Recursively processes offline players in batches.
     */
    private void recursiveBatchProcess(Iterator<UUID> playerIterator,
                                       PlayerStorage storage,
                                       Consumer<PlayerLoot> action,
                                       AtomicInteger counter) {
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE && playerIterator.hasNext(); i++) {
            UUID uuid = playerIterator.next();

            CompletableFuture<Void> future = storage.load(uuid)
                    .thenApply(holder -> {
                        PlayerLoot loot = holder.getComponent(Loot4Everyone.get().getPlayerLootcomponentType());
                        if (loot != null) {
                            action.accept(loot);
                        }
                        return holder;
                    })
                    .thenCompose(holder -> storage.save(uuid, holder, true))
                    .thenRun(counter::incrementAndGet)
                    .exceptionally(ex -> {
                        Loot4Everyone.LOGGER.atSevere().log("Error processing offline player " + uuid + ": " + ex.getMessage());
                        return null;
                    });

            batchFutures.add(future);
        }

        if (batchFutures.isEmpty()) {
            // All done
            return;
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> recursiveBatchProcess(playerIterator, storage, action, counter));
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}