package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import org.joml.Vector3i;
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

    OptionalArg<String> dropListArg = this.withOptionalArg("droplist", "ID of the dropList you want to set on the loot container", ArgTypes.STRING);

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player executor = store.getComponent(ref, Player.getComponentType());
        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);
        if (targetBlock == null){
            commandContext.sendMessage(Message.raw("Please loot at a block that can store items!"));
            return;
        }

        ChunkStore chunkStore = world.getChunkStore();

        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, targetBlock.x(), targetBlock.y(), targetBlock.z());

        if (blockRef == null) return;

        ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

        if (itemContainerState != null) {

            if (!itemContainerState.getWindows().isEmpty()){
                commandContext.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
                return;
            }

            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (lootChestTemplate.hasTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z())) {
                commandContext.sendMessage(Message.raw("This loot container already exists!"));
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
                            lootChestTemplate.saveTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z(), inventoryRandom, dropList);
                            commandContext.sendMessage(Message.raw("Your loot container has been generated based on the dropList you've provided!"));
                        } else {
                            commandContext.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                        }
                    } else {
                        commandContext.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                    }
                } else {
                    commandContext.sendMessage(Message.raw("DropList is invalid"));
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
                    IWorldGen iWorldGen = world.getChunkStore().getGenerator();
                    int seed = (int) world.getWorldConfig().getSeed();
                    String zoneName = "";
                    if (iWorldGen instanceof ChunkGenerator chunkGenerator) {
                        ZoneBiomeResult result = chunkGenerator.getZoneBiomeResultAt(seed, targetBlock.x(), targetBlock.z());
                        zoneName = result.getZoneResult().getZone().name(); // Ex: Zone1_Tier1
                    }

                    if (zoneName.contains("Tier5")){
                        int randomTier = ThreadLocalRandom.current().nextInt(1,5);
                        zoneName = zoneName.replace("Tier5","Tier" + randomTier);
                    }
                    else if (!zoneName.contains("Zone") || !zoneName.contains("Tier")){
                        int randomZone = ThreadLocalRandom.current().nextInt(1,5);
                        int randomTier = ThreadLocalRandom.current().nextInt(1,5);
                        zoneName = "Zone" + randomZone + "_Tier" + randomTier;
                    }

                    String dropListName = zoneName.replace("_", "_Encounters_");

                    List<ItemStack> items = new ArrayList<>();
                    lootChestTemplate.saveTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z(), items, dropListName);
                    commandContext.sendMessage(Message.raw("Your loot container has been generated based on the dropList of the zone!"));
                }
                else{
                    lootChestTemplate.saveTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z(), currentItems,"custom");
                    commandContext.sendMessage(Message.raw("Your loot container has been generated based on items inside!"));
                }
            }
        }
        else{
            commandContext.sendMessage(Message.raw("Please loot at a block that can store items!"));
        }
    }
}
