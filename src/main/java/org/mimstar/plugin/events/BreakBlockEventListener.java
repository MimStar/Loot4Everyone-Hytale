package org.mimstar.plugin.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3i;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;
import org.mimstar.plugin.resources.LootChestTemplate;

public class BreakBlockEventListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
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

        if (isProtectedChest(player, target.x(), target.y(), target.z())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();

            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(player.getWorld(), target.x(), target.y(), target.z());

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
            return;
        }

        if (isProtectedChest(player, target.x(), target.y() + 1, target.z())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();

            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(player.getWorld(), target.x(), target.y() + 1, target.z());

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
        }

        if (isProtectedChest(player, target.x() + 1, target.y() + 1, target.z())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();

            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(player.getWorld(), target.x() + 1, target.y() + 1, target.z());

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
        }

        if (isProtectedChest(player, target.x(), target.y() + 1, target.z() + 1)) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();

            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(player.getWorld(), target.x(), target.y() + 1, target.z() + 1);

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
        }
    }

    /**
     * Helper method to check if a specific block coordinate contains a protected Loot Chest.
     */
    private boolean isProtectedChest(Player player, int x, int y, int z) {

        ChunkStore chunkStore = player.getWorld().getChunkStore();

        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(player.getWorld(), x, y, z);

        if (blockRef == null) return false;

        ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

        if (itemContainerState != null) {
            LootChestTemplate lootChestTemplate = player.getWorld().getChunkStore().getStore()
                    .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            return lootChestTemplate.hasTemplate(x, y, z);
        }
        return false;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}