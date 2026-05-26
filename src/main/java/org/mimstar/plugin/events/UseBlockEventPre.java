package org.mimstar.plugin.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3i;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.OpenedContainerComponent;
import org.mimstar.plugin.components.PlayerLoot;
import org.mimstar.plugin.resources.LootChestConfig;
import org.mimstar.plugin.resources.LootChestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

        ChunkStore chunkStore = player.getWorld().getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(target.x(), target.z());
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);

        if (chunkRef == null) return;

        BlockComponentChunk blockComponentChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) return;

        int blockInColumnIndex = ChunkUtil.indexBlockInColumn(target.x(), target.y(), target.z());
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockInColumnIndex);

        if (blockRef == null) return;

        ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

        if (itemContainerState != null) {
            LootChestTemplate lootChestTemplate = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

            if (useBlockEventPre.getInteractionType().toString().equals("Use") && lootChestTemplate.hasTemplate(target.x(), target.y(), target.z())) {

                if (!itemContainerState.getWindows().isEmpty()) {
                    useBlockEventPre.setCancelled(true);
                    return;
                }

                OpenedContainerComponent existing = store.getComponent(playerRef, Loot4Everyone.get().getContainerComponentType());
                if (existing != null){
                    useBlockEventPre.setCancelled(true);
                    return;
                }

                OpenedContainerComponent monitor = new OpenedContainerComponent(target.x(), target.y(), target.z());
                commandBuffer.addComponent(playerRef, Loot4Everyone.get().getContainerComponentType(), monitor);

                LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
                PlayerLoot playerLoot = store.getComponent(playerRef, Loot4Everyone.get().getPlayerLootcomponentType());

                if (!lootChestConfig.isLootChestRandom()) {
                    if (lootChestTemplate.getTemplate(target.x(), target.y(), target.z()).isEmpty()) {
                        String droplist = lootChestTemplate.getDropList(target.x(),target.y(),target.z());
                        if (droplist != null && !droplist.equals("undefined")) {
                            if (!droplist.equals("custom")) {
                                if (droplist.contains("Tier5")) {
                                    int randomTier = ThreadLocalRandom.current().nextInt(1, 5);
                                    droplist = droplist.replace("Tier5", "Tier" + randomTier);
                                    lootChestTemplate.setDropList(target.x(), target.y(), target.z(), droplist);
                                }

                                List<ItemStack> stacks = ItemModule.get().getRandomItemDrops(droplist);

                                if (!stacks.isEmpty()) {
                                    short capacity = itemContainerState.getItemContainer().getCapacity();
                                    List<Short> slots = new ArrayList<>();
                                    for (short s = 0; s < capacity; s++) {
                                        slots.add(s);
                                    }
                                    Collections.shuffle(slots, ThreadLocalRandom.current());

                                    ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                                    if (clearTransaction.succeeded()) {

                                        for (int idx = 0; idx < stacks.size() && idx < slots.size(); idx++) {
                                            short slot = slots.get(idx);
                                            itemContainerState.getItemContainer().setItemStackForSlot(slot, stacks.get(idx));
                                        }
                                    } else {
                                        useBlockEventPre.setCancelled(true);
                                    }
                                } else {
                                    useBlockEventPre.setCancelled(true);
                                }
                            } else {
                                List<ItemStack> customStacks = lootChestTemplate.getTemplate(target.x(), target.y(), target.z());

                                if (customStacks != null && !customStacks.isEmpty()) {
                                    short capacity = itemContainerState.getItemContainer().getCapacity();
                                    List<Short> slots = new ArrayList<>();
                                    for (short s = 0; s < capacity; s++) {
                                        slots.add(s);
                                    }

                                    Collections.shuffle(slots, ThreadLocalRandom.current());

                                    ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                                    if (clearTransaction.succeeded()) {

                                        for (int idx = 0; idx < customStacks.size() && idx < slots.size(); idx++) {
                                            ItemStack originalStack = customStacks.get(idx);

                                            int maxAmount = originalStack.getQuantity();
                                            int randomAmount = ThreadLocalRandom.current().nextInt(maxAmount) + 1;

                                            if (ThreadLocalRandom.current().nextDouble() > 0.25) {
                                                short slot = slots.get(idx);

                                                ItemStack droppedStack = new ItemStack(
                                                        originalStack.getItemId(),
                                                        randomAmount,
                                                        originalStack.getDurability(),
                                                        originalStack.getMaxDurability(),
                                                        originalStack.getMetadata()
                                                );

                                                itemContainerState.getItemContainer().setItemStackForSlot(slot, droppedStack);
                                            }
                                        }
                                    } else {
                                        useBlockEventPre.setCancelled(true);
                                    }
                                } else {
                                    useBlockEventPre.setCancelled(true);
                                }
                            }
                        }

                        List<ItemStack> items = new ArrayList<>();
                        for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++) {
                            items.add(itemContainerState.getItemContainer().getItemStack((short) i));
                        }

                        lootChestTemplate.saveTemplate(target.x(), target.y(), target.z(), items, lootChestTemplate.getDropList(target.x(), target.y(), target.z()));
                    } else {
                        applyPersistentLoot(itemContainerState, playerLoot, lootChestTemplate, target, player.getWorld().getName());
                    }
                }
                else {
                    if (playerLoot != null && !playerLoot.isFirstTime(target.x(), target.y(), target.z(), player.getWorld().getName())) {
                        List<ItemStack> items = playerLoot.getInventory(target.x(), target.y(), target.z(), player.getWorld().getName());
                        for (int i = 0; i < itemContainerState.getItemContainer().getCapacity(); i++) {
                            itemContainerState.getItemContainer().setItemStackForSlot((short) i, items.get(i));
                        }
                    } else {
                        String droplist = lootChestTemplate.getDropList(target.x(),target.y(),target.z());
                        if (droplist != null && !droplist.equals("undefined")) {
                            if (!droplist.equals("custom")) {

                                if (droplist.contains("Tier5")){
                                    int randomTier = ThreadLocalRandom.current().nextInt(1,5);
                                    droplist = droplist.replace("Tier5","Tier" + randomTier);
                                    lootChestTemplate.setDropList(target.x(), target.y(), target.z(), droplist);
                                }

                                List<ItemStack> stacks = ItemModule.get().getRandomItemDrops(droplist);
                                if (!stacks.isEmpty()) {
                                    short capacity = itemContainerState.getItemContainer().getCapacity();
                                    List<Short> slots = new ArrayList<>();
                                    for (short s = 0; s < capacity; s++) {
                                        slots.add(s);
                                    }
                                    Collections.shuffle(slots, ThreadLocalRandom.current());

                                    ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                                    if (clearTransaction.succeeded()) {

                                        for (int idx = 0; idx < stacks.size() && idx < slots.size(); idx++) {
                                            short slot = slots.get(idx);
                                            itemContainerState.getItemContainer().setItemStackForSlot(slot, stacks.get(idx));
                                        }
                                    } else {
                                        useBlockEventPre.setCancelled(true);
                                    }
                                } else {
                                    useBlockEventPre.setCancelled(true);
                                }
                            }
                            else{
                                List<ItemStack> customStacks = lootChestTemplate.getTemplate(target.x(), target.y(), target.z());

                                if (customStacks != null && !customStacks.isEmpty()) {
                                    short capacity = itemContainerState.getItemContainer().getCapacity();
                                    List<Short> slots = new ArrayList<>();
                                    for (short s = 0; s < capacity; s++) {
                                        slots.add(s);
                                    }

                                    Collections.shuffle(slots, ThreadLocalRandom.current());

                                    ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                                    if (clearTransaction.succeeded()) {

                                        for (int idx = 0; idx < customStacks.size() && idx < slots.size(); idx++) {
                                            ItemStack originalStack = customStacks.get(idx);

                                            int maxAmount = originalStack.getQuantity();
                                            int randomAmount = ThreadLocalRandom.current().nextInt(maxAmount) + 1;

                                            if (ThreadLocalRandom.current().nextDouble() > 0.25) {
                                                short slot = slots.get(idx);

                                                ItemStack droppedStack = new ItemStack(
                                                        originalStack.getItemId(),
                                                        randomAmount,
                                                        originalStack.getDurability(),
                                                        originalStack.getMaxDurability(),
                                                        originalStack.getMetadata()
                                                );

                                                itemContainerState.getItemContainer().setItemStackForSlot(slot, droppedStack);
                                            }
                                        }
                                    } else {
                                        useBlockEventPre.setCancelled(true);
                                    }
                                } else {
                                    useBlockEventPre.setCancelled(true);
                                }
                            }
                        }
                        else{
                            useBlockEventPre.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
    private void applyPersistentLoot(ItemContainerBlock state, PlayerLoot playerLoot, LootChestTemplate template, Vector3i pos, String worldName) {
        List<ItemStack> items;
        if (playerLoot != null && !playerLoot.isFirstTime(pos.x(), pos.y(), pos.z(), worldName)) {
            items = playerLoot.getInventory(pos.x(), pos.y(), pos.z(), worldName);
        } else {
            items = template.getTemplate(pos.x(), pos.y(), pos.z());
        }

        for (int i = 0; i < state.getItemContainer().getCapacity() && i < items.size(); i++) {
            state.getItemContainer().setItemStackForSlot((short) i, items.get(i));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}