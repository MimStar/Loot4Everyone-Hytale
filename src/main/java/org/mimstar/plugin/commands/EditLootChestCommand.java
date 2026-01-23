package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EditLootChestCommand extends AbstractPlayerCommand {

    public EditLootChestCommand() {
        super("editlc", "A command to edit a loot container");
    }

    OptionalArg<String> dropListArg = this.withOptionalArg("dropList", "ID of the dropList you want to set on the loot container", ArgTypes.STRING);

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player executor = store.getComponent(ref, Player.getComponentType());

        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);
        if (targetBlock == null) {
            executor.sendMessage(Message.raw("Please look at a loot container!"));
            return;
        }

        BlockState state = world.getState(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), true);
        if (!(state instanceof ItemContainerState itemContainerState)) {
            executor.sendMessage(Message.raw("Please look at a loot container!"));
            return;
        }

        if (!itemContainerState.getWindows().isEmpty()){
            executor.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
            return;
        }

        LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
        if (!lootChestTemplate.hasTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {
            executor.sendMessage(Message.raw("No loot container found here. Use /generatelc first."));
            return;
        }

        if (dropListArg.provided(commandContext)) {
            String dropList = dropListArg.get(commandContext);
            ItemDropList itemDropList = ItemDropList.getAssetMap().getAsset(dropList);

            if (itemDropList != null) {
                List<ItemStack> newStacks = ItemModule.get().getRandomItemDrops(dropList);

                lootChestTemplate.saveTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), newStacks, dropList);

                executor.sendMessage(Message.raw("Loot container updated to: " + dropList));
            } else {
                executor.sendMessage(Message.raw("Invalid dropList."));
            }
        } else {
            String dropList = lootChestTemplate.getDropList(targetBlock.getX(),targetBlock.getY(),targetBlock.getZ());
            if (dropList != null && dropList.equals("undefined")){
                executor.sendMessage(Message.raw("Loot container don't have a dropList you can't edit it"));
            }
            else if (dropList != null && dropList.equals("custom")){
                List<ItemStack> items = lootChestTemplate.getTemplate(targetBlock.getX(),targetBlock.getY(),targetBlock.getZ());
                if (items != null){
                    if (itemContainerState.getItemContainer().getCapacity() >= items.size()){
                        ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                        if (clearTransaction.succeeded()) {
                            short index_container = 0;
                            for (ItemStack itemStack : items){
                                itemContainerState.getItemContainer().setItemStackForSlot(index_container, itemStack);
                                index_container++;
                            }
                            lootChestTemplate.removeTemplate(targetBlock.getX(),targetBlock.getY(),targetBlock.getZ());
                            executor.sendMessage(Message.raw("Loot container can be edited, don't forget to do /generatelc to regenerate it!"));
                        }
                        else{
                            executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                        }
                    }
                    else{
                        executor.sendMessage(Message.raw("Loot container can't contain the dropList"));
                    }
                }
                else{
                    executor.sendMessage(Message.raw("Loot container don't have items and can't be edited."));
                }
            }
            else if (dropList != null){
                List<ItemDrop> itemDropList = ItemDropList.getAssetMap().getAsset(dropList).getContainer().getAllDrops(new ObjectArrayList<>());
                if (itemDropList != null){
                    if (itemContainerState.getItemContainer().getCapacity() >= itemDropList.size()){
                        ClearTransaction clearTransaction = itemContainerState.getItemContainer().clear();
                        if (clearTransaction.succeeded()) {
                            short index_container = 0;
                            for (ItemDrop itemDrop : itemDropList) {
                                ItemStack itemStack = new ItemStack(itemDrop.getItemId(), itemDrop.getQuantityMax(), itemDrop.getMetadata());
                                itemContainerState.getItemContainer().setItemStackForSlot(index_container, itemStack);
                                index_container++;
                            }
                            lootChestTemplate.removeTemplate(targetBlock.getX(),targetBlock.getY(),targetBlock.getZ());
                            executor.sendMessage(Message.raw("Loot container can be edited, don't forget to do /generatelc to regenerate it!"));
                        }
                        else{
                            executor.sendMessage(Message.raw("There has been an error in the process. Please try again!"));
                        }
                    }
                    else{
                        executor.sendMessage(Message.raw("Loot container can't contain the dropList"));
                    }
                }
                else{
                    executor.sendMessage(Message.raw("Loot container have a dropList but can't be edited"));
                }
            }
            else{
                executor.sendMessage(Message.raw("Loot container don't have a dropList you can't edit it"));
            }
        }
    }
}
