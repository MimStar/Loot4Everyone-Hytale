package org.mimstar.plugin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class DamageBlockEventListener extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    public DamageBlockEventListener() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl DamageBlockEvent damageBlockEvent) {

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;

        Vector3i target = damageBlockEvent.getTargetBlock();
        if (isProtectedChest(player, target.getX(), target.getY(), target.getZ())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            BlockState blockState = player.getWorld().getState(target.getX(), target.getY(), target.getZ(), true);

            if (lootChestConfig != null && lootChestConfig.isCanPlayerBreakLootChests() && blockState instanceof ItemContainerState itemContainerState && itemContainerState.getWindows().isEmpty()){
                return;
            }

            damageBlockEvent.setCancelled(true);
            return;
        }

        if (isProtectedChest(player, target.getX(), target.getY() + 1, target.getZ())) {

            LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            BlockState blockState = player.getWorld().getState(target.getX(), target.getY() + 1, target.getZ(), true);

            if (lootChestConfig != null && lootChestConfig.isCanPlayerBreakLootChests() && blockState instanceof ItemContainerState itemContainerState && itemContainerState.getWindows().isEmpty()){
                return;
            }

            damageBlockEvent.setCancelled(true);
        }
    }

    /**
     * Helper method to check if a specific block coordinate contains a protected Loot Chest.
     */
    private boolean isProtectedChest(Player player, int x, int y, int z) {

        BlockState blockState = player.getWorld().getState(x, y, z, true);

        if (blockState instanceof ItemContainerState itemContainerState) {
            LootChestTemplate lootChestTemplate = itemContainerState.getReference().getStore()
                    .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

            return lootChestTemplate != null && lootChestTemplate.hasTemplate(x, y, z);
        }
        return false;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}