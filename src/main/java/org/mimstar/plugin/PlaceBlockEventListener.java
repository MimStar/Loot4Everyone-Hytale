package org.mimstar.plugin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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