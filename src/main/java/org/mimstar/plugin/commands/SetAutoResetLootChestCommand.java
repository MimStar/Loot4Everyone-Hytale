package org.mimstar.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class SetAutoResetLootChestCommand extends AbstractAsyncCommand {
    public SetAutoResetLootChestCommand() {
        super("setautoresetlc", "Set the next loot reset interval (Days/Hours/Minutes/Seconds). No args to disable.");
    }

    // Define optional arguments for precise control
    OptionalArg<Integer> daysArg = this.withOptionalArg("days", "Days", ArgTypes.INTEGER);
    OptionalArg<Integer> hoursArg = this.withOptionalArg("hours", "Hours", ArgTypes.INTEGER);
    OptionalArg<Integer> minutesArg = this.withOptionalArg("minutes", "Minutes", ArgTypes.INTEGER);
    OptionalArg<Integer> secondsArg = this.withOptionalArg("seconds", "Seconds", ArgTypes.INTEGER);
    OptionalArg<World> worldArg = this.withOptionalArg("world", "World ID", ArgTypes.WORLD);

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        World world = null;

        if (commandContext.provided(worldArg)) {
            world = worldArg.get(commandContext);
        } else if (commandContext.isPlayer()) {
            world = Universe.get().getWorld(commandContext.sender().getUuid());
        }

        if (world == null) {
            commandContext.sendMessage(Message.raw("Please provide a world ID or run this as a player."));
            return CompletableFuture.completedFuture(null);
        }

        World finalWorld = world;
        return this.runAsync(commandContext, () -> {
            LootChestConfig config = finalWorld.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            int d = commandContext.provided(daysArg) ? Math.max(0, daysArg.get(commandContext)) : 0;
            int h = commandContext.provided(hoursArg) ? Math.max(0, hoursArg.get(commandContext)) : 0;
            int m = commandContext.provided(minutesArg) ? Math.max(0, minutesArg.get(commandContext)) : 0;
            int s = commandContext.provided(secondsArg) ? Math.max(0, secondsArg.get(commandContext)) : 0;

            boolean hasTime = commandContext.provided(daysArg) || commandContext.provided(hoursArg) ||
                    commandContext.provided(minutesArg) || commandContext.provided(secondsArg);

            if (!hasTime) {
                config.setNextLootResetDaysInterval(0);
                config.setNextLootResetHoursInterval(0);
                config.setNextLootResetMinutesInterval(0);
                config.setNextLootResetSecondsInterval(0);
                config.setNextLootReset(-1);
                commandContext.sendMessage(Message.raw("Auto-reset deactivated."));
            } else {
                config.setNextLootResetDaysInterval(d);
                config.setNextLootResetHoursInterval(h);
                config.setNextLootResetMinutesInterval(m);
                config.setNextLootResetSecondsInterval(s);

                LocalDateTime target = LocalDateTime.now()
                        .plusDays(d)
                        .plusHours(h)
                        .plusMinutes(m)
                        .plusSeconds(s);

                long epochSeconds = target.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                config.setNextLootReset((int) epochSeconds);

                commandContext.sendMessage(Message.raw(String.format(
                        "Loot reset set for: %dd %dh %dm %ds. Next reset at: %s",
                        d, h, m, s, target
                )));
            }
        }, world);
    }
}
