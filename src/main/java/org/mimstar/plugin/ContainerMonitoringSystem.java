package org.mimstar.plugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ContainerMonitoringSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, OpenedContainerComponent> containerComponentType;

    public ContainerMonitoringSystem(ComponentType<EntityStore, OpenedContainerComponent> type) {
        this.containerComponentType = type;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        OpenedContainerComponent monitor = archetypeChunk.getComponent(index, containerComponentType);
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());

        World world = player.getWorld();
        BlockState blockState = world.getState(monitor.getX(), monitor.getY(), monitor.getZ(), true);

        boolean stillOpen = false;

        if (blockState instanceof ItemContainerState containerState) {
            if (!containerState.getWindows().isEmpty()) {
                stillOpen = true;
            }
        }

        if (!stillOpen) {
            PlayerLoot playerLoot = store.getComponent(playerRef, Loot4Everyone.get().getPlayerLootcomponentType());
            if (blockState instanceof ItemContainerState itemContainerState && playerLoot != null) {
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++) {
                    items.add(itemContainerState.getItemContainer().getItemStack((short) i));
                }
                playerLoot.setInventory(monitor.getX(), monitor.getY(), monitor.getZ(), items);
                commandBuffer.replaceComponent(playerRef,Loot4Everyone.get().getPlayerLootcomponentType(), playerLoot);
                itemContainerState.getItemContainer().clear();
            }
            commandBuffer.removeComponent(playerRef, containerComponentType);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), containerComponentType);
    }
}
