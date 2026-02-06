package org.mimstar.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SetAutoResetLootChestCommand extends AbstractAsyncCommand {
    public SetAutoResetLootChestCommand(){
        super("setautoresetlc","A command to set nextLootResetInterval in Loot_chest_config resource of the world.");
    }

    RequiredArg<Integer> intArg = this.withRequiredArg("value", "Value of the rule", ArgTypes.INTEGER);

    OptionalArg<World> worldArg = this.withOptionalArg("world","ID of the world where the config is stored", ArgTypes.WORLD);

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        World world = null;

        if (commandContext.isPlayer()) {
            UUID player_uuid = commandContext.sender().getUuid();
            PlayerRef playerRef = Universe.get().getPlayer(player_uuid);
            if (playerRef == null){
                commandContext.sendMessage(Message.raw("Player not found"));
                return CompletableFuture.completedFuture(null);
            }
            world = Universe.get().getWorld(playerRef.getWorldUuid());
        }

        if (world == null || commandContext.provided(worldArg)){
            if (commandContext.provided(worldArg)){
                world = worldArg.get(commandContext);
            }
            else{
                commandContext.sendMessage(Message.raw("Please provide a world ID"));
                return CompletableFuture.completedFuture(null);
            }
        }

        World finalWorld = world;
        return this.runAsync(commandContext, () -> {
            LootChestConfig lootChestConfig = finalWorld.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
            int value = intArg.get(commandContext);

            if (value < 0){
                value = 0;
            }

            lootChestConfig.setNextLootResetInterval(value);

            if (value > 0) {
                int currentEpochDay = (int) LocalDate.now().toEpochDay();

                lootChestConfig.setNextLootReset(currentEpochDay + value);
            }
            else{
                lootChestConfig.setNextLootReset(-1);
            }

            commandContext.sendMessage(Message.raw("Rule set to " + value));
        }, world);
    }
}
