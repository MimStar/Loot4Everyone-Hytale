package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GenerateLootChestCommand extends AbstractPlayerCommand {

    public GenerateLootChestCommand() {
        super("generatelc", "A command to generate a loot container");
    }

    OptionalArg<String> dropListArg = this.withOptionalArg("dropList", "ID of the dropList you want to set on the loot container", ArgTypes.STRING);

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player executor = store.getComponent(ref, Player.getComponentType());
        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);
        if (targetBlock == null){
            executor.sendMessage(Message.raw("Please loot at a block that can store items!"));
            return;
        }
        BlockState state = world.getState(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), true);
        if (state instanceof ItemContainerState itemContainerState) {

            if (!itemContainerState.getWindows().isEmpty()){
                executor.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
                return;
            }

            targetBlock = new Vector3i(itemContainerState.getBlockX(),itemContainerState.getBlockY(),itemContainerState.getBlockZ());

            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (lootChestTemplate.hasTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {
                executor.sendMessage(Message.raw("This loot container already exists!"));
                return;
            }

            if (dropListArg.provided(commandContext)) {
                String dropList = dropListArg.get(commandContext);
                ItemDropList itemDropList = ItemDropList.getAssetMap().getAsset(dropList);
                if (itemDropList != null && itemDropList.getContainer() != null) {
                    List<ItemStack> stacks = ItemModule.get().getRandomItemDrops(dropList);
                    if (!stacks.isEmpty()) {
                        short capacity = itemContainerState.getItemContainer().getCapacity();
                        List<Short> slots = new ArrayList<>();
                        for (short s = 0; s < capacity; s++) {
                            slots.add(s);
                        }

                        Random rnd = new Random(targetBlock.hashCode());
                        Collections.shuffle(slots, rnd);

                        List<ItemStack> inventoryRandom = new ArrayList<>();

                        ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                        if (clearTransaction.succeeded()) {

                            for (int idx = 0; idx < stacks.size() && idx < slots.size(); idx++) {
                                inventoryRandom.add(stacks.get(idx));
                            }
                            lootChestTemplate.saveTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), inventoryRandom, dropList);
                            executor.sendMessage(Message.raw("Your loot container has been generated based on the dropList you've provided!"));
                        } else {
                            executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                        }
                    } else {
                        executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                    }
                } else {
                    executor.sendMessage(Message.raw("DropList is invalid"));
                }
            }
            else{
                ItemContainer container = itemContainerState.getItemContainer();
                List<ItemStack> currentItems = new ArrayList<>();
                boolean isEmpty = true;

                for (short i = 0; i < container.getCapacity(); i++) {
                    ItemStack stack = container.getItemStack(i);
                    if (stack != null) {
                        currentItems.add(stack);
                        isEmpty = false;
                    }
                }

                if (isEmpty){
                    ChunkGenerator chunkGenerator = (ChunkGenerator) world.getChunkStore().getGenerator();
                    int seed = (int) world.getWorldConfig().getSeed();

                    ZoneBiomeResult result = chunkGenerator.getZoneBiomeResultAt(seed, targetBlock.getX(), targetBlock.getZ());
                    String zoneName = result.getZoneResult().getZone().name(); // Ex: Zone1_Tier1

                    String dropListName = zoneName.replace("_", "_Encounters_");

                    List<ItemStack> stacks = ItemModule.get().getRandomItemDrops(dropListName);
                    if (!stacks.isEmpty()) {
                        short capacity = itemContainerState.getItemContainer().getCapacity();
                        List<Short> slots = new ArrayList<>();
                        for (short s = 0; s < capacity; s++) {
                            slots.add(s);
                        }

                        Collections.shuffle(slots, ThreadLocalRandom.current());

                        List<ItemStack> inventoryRandom = new ArrayList<>();

                        ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                        if (clearTransaction.succeeded()) {
                            for (int idx = 0; idx < stacks.size() && idx < slots.size(); idx++) {
                                inventoryRandom.add(stacks.get(idx));
                            }
                            lootChestTemplate.saveTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), inventoryRandom, dropListName);
                            executor.sendMessage(Message.raw("Your loot container has been generated based on the dropList of the zone!"));
                        } else {
                            executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                        }
                    } else {
                        executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                    }
                }
                else{
                    lootChestTemplate.saveTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), currentItems,"custom");
                    executor.sendMessage(Message.raw("Your loot container has been generated based on items inside!"));
                }
            }
        }
        else{
            executor.sendMessage(Message.raw("Please loot at a block that can store items!"));
        }
    }
}
