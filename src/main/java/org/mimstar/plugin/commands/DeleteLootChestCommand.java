package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;

public class DeleteLootChestCommand extends AbstractPlayerCommand {
    public DeleteLootChestCommand() {
        super("deletelc", "A command to delete a loot container");
    }
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

            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (lootChestTemplate.hasTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {
                lootChestTemplate.removeTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
                executor.sendMessage(Message.raw("Loot container deleted!"));
            }
            else{
                executor.sendMessage(Message.raw("Please look at a loot container!"));
            }
        }
        else{
            executor.sendMessage(Message.raw("Please loot at a loot container!"));
        }
    }

}
