package org.mimstar.plugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;

public class UseBlockEventPre extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    public UseBlockEventPre() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl UseBlockEvent.Pre useBlockEventPre) {

        Ref<EntityStore> playerRef = useBlockEventPre.getContext().getEntity();

        Player player = store.getComponent(useBlockEventPre.getContext().getEntity(), Player.getComponentType());

        Vector3i target = useBlockEventPre.getTargetBlock();
        BlockState blockType = player.getWorld().getState(target.getX(), target.getY(), target.getZ(),true);

        if (blockType instanceof ItemContainerState itemContainerState){
            LootChestTemplate lootChestTemplate = itemContainerState.getReference().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (useBlockEventPre.getInteractionType().toString().equals("Use") && lootChestTemplate != null && lootChestTemplate.hasTemplate(target.getX(),target.getY(), target.getZ())){

                if (!itemContainerState.getWindows().isEmpty()){
                    useBlockEventPre.setCancelled(true);
                    return;
                }

                OpenedContainerComponent monitor = new OpenedContainerComponent(target.getX(), target.getY(), target.getZ());

                commandBuffer.addComponent(playerRef, Loot4Everyone.get().getContainerComponentType(), monitor);

                if (lootChestTemplate.getTemplate(target.getX(),target.getY(),target.getZ()).isEmpty()){

                    List<ItemStack> items = new ArrayList<>();
                    for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++) {
                        items.add(itemContainerState.getItemContainer().getItemStack((short) i));
                    }

                    lootChestTemplate.saveTemplate(target.getX(), target.getY(), target.getZ(), items);
                }
                else{
                    PlayerLoot playerLoot = store.getComponent(playerRef,Loot4Everyone.get().getPlayerLootcomponentType());
                    if (playerLoot != null && playerLoot.hasData(target.getX(), target.getY(), target.getZ())){
                        List<ItemStack> items = playerLoot.getInventory(target.getX(), target.getY(), target.getZ());
                        for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++){
                            itemContainerState.getItemContainer().setItemStackForSlot((short) i, items.get(i));
                        }
                    }
                    else{
                        List<ItemStack> items = lootChestTemplate.getTemplate(target.getX(),target.getY(),target.getZ());
                        for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++){
                            itemContainerState.getItemContainer().setItemStackForSlot((short) i, items.get(i));
                        }
                    }
                }
            }
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
