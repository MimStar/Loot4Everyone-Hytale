package org.mimstar.plugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetBreakRuleCommand extends AbstractPlayerCommand {

    public SetBreakRuleCommand(){
        super("setbreakrule","A command to set CanPlayerBreakLootChests in Loot_chest_config resource of the world.");
    }

    RequiredArg<Boolean> boolArg = this.withRequiredArg("value", "Value of the rule", ArgTypes.BOOLEAN);

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        LootChestConfig lootChestConfig = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
        boolean value = boolArg.get(commandContext);
        lootChestConfig.setCanPlayerBreakLootChests(value);
        Player player = store.getComponent(ref, Player.getComponentType());
        player.sendMessage(Message.raw("Rule set to " + value));

    }
}
