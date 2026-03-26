package org.mimstar.plugin.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestTemplate;

public class PlaceBlockEventListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    public PlaceBlockEventListener() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl PlaceBlockEvent event) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;

        ItemStack item = event.getItemInHand();

        if (item != null && item.getItemId().toLowerCase().contains("chest")) {
            Vector3i pos = event.getTargetBlock();

            if (isProtectedChest(player, pos.getX() + 1, pos.getY(), pos.getZ()) ||
                    isProtectedChest(player, pos.getX() - 1, pos.getY(), pos.getZ()) ||
                    isProtectedChest(player, pos.getX(), pos.getY(), pos.getZ() + 1) ||
                    isProtectedChest(player, pos.getX(), pos.getY(), pos.getZ() - 1)) {

                event.setCancelled(true);
            }
        }
    }

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