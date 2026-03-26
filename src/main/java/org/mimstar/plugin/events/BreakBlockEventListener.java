package org.mimstar.plugin.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
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

        if (isProtectedChest(player, target.getX(), target.getY(), target.getZ())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(target.getX(), target.getZ());
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

            if (chunkRef == null) return;

            BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
            if (blockComponentChunk == null) return;

            int blockInColumnIndex = ChunkUtil.indexBlockInColumn(target.getX(), target.getY(), target.getZ());
            Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
            return;
        }

        if (isProtectedChest(player, target.getX(), target.getY() + 1, target.getZ())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(target.getX(), target.getZ());
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

            if (chunkRef == null) return;

            BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
            if (blockComponentChunk == null) return;

            int blockInColumnIndex = ChunkUtil.indexBlockInColumn(target.getX(), target.getY() + 1, target.getZ());
            Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
        }

        if (isProtectedChest(player, target.getX() + 1, target.getY() + 1, target.getZ())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(target.getX(), target.getZ());
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

            if (chunkRef == null) return;

            BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
            if (blockComponentChunk == null) return;

            int blockInColumnIndex = ChunkUtil.indexBlockInColumn(target.getX() + 1, target.getY() + 1, target.getZ());
            Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

            if (blockRef == null) return;

            ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

            if (lootChestConfig.isCanPlayerBreakLootChests() && itemContainerState != null && itemContainerState.getWindows().isEmpty()){
                return;
            }

            breakBlockEvent.setCancelled(true);
        }

        if (isProtectedChest(player, target.getX(), target.getY() + 1, target.getZ() + 1)) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            ChunkStore chunkStore = player.getWorld().getChunkStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(target.getX(), target.getZ());
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

            if (chunkRef == null) return;

            BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
            if (blockComponentChunk == null) return;

            int blockInColumnIndex = ChunkUtil.indexBlockInColumn(target.getX(), target.getY() + 1, target.getZ() + 1);
            Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

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
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

        if (chunkRef == null) return false;

        BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) return false;

        int blockInColumnIndex = ChunkUtil.indexBlockInColumn(x, y, z);
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

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