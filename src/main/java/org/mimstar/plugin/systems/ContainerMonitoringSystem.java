package org.mimstar.plugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.OpenedContainerComponent;
import org.mimstar.plugin.components.PlayerLoot;

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

        commandBuffer.run(deferredStore -> {

            if (index >= archetypeChunk.size()) {
                return;
            }

            Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
            OpenedContainerComponent monitor = archetypeChunk.getComponent(index, containerComponentType);
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());

            World world = player.getWorld();
            ChunkStore chunkStore = world.getChunkStore();

            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, monitor.getX(), monitor.getY(), monitor.getZ());
            if (blockRef != null) {

                ItemContainerBlock itemContainerBlock = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

                if (itemContainerBlock != null) {
                    boolean stillOpen = !itemContainerBlock.getWindows().isEmpty();

                    if (!stillOpen) {
                        handleContainerClosure(playerRef, monitor, player, itemContainerBlock, deferredStore, commandBuffer);
                    }
                } else {
                    commandBuffer.removeComponent(playerRef, containerComponentType);
                }
            }
        });
    }

    private void handleContainerClosure(Ref<EntityStore> playerRef, OpenedContainerComponent monitor, Player player,
                                        ItemContainerBlock container, Store<EntityStore> deferredStore, CommandBuffer<EntityStore> commandBuffer) {

        PlayerLoot playerLoot = deferredStore.getComponent(playerRef, Loot4Everyone.get().getPlayerLootcomponentType());

        if (playerLoot != null) {
            int x = monitor.getX();
            int y = monitor.getY();
            int z = monitor.getZ();
            String worldName = player.getWorld().getName();

            if (playerLoot.hasDeprecatedData(x, y, z)) {
                playerLoot.replaceDeprecatedData(x, y, z, worldName);
            }

            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < container.getItemContainer().getCapacity(); i++) {
                items.add(container.getItemContainer().getItemStack((short) i));
            }

            playerLoot.setInventory(x, y, z, worldName, items);
            commandBuffer.replaceComponent(playerRef, Loot4Everyone.get().getPlayerLootcomponentType(), playerLoot);

            container.getItemContainer().clear();
        }

        commandBuffer.removeComponent(playerRef, containerComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), containerComponentType);
    }
}
