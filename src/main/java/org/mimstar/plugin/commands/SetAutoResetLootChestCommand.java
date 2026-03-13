package org.mimstar.plugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

public class SetAutoResetLootChestCommand extends AbstractAsyncCommand {
    public SetAutoResetLootChestCommand() {
        super("setautoresetlc", "Set the next loot reset interval (Mode/Days/Hours/Minutes/Seconds). No args to disable.");
    }

    OptionalArg<Integer> modeArg = this.withOptionalArg("mode", "Reset Mode (0=False, 1=Game Time, 2=Real Time)", ArgTypes.INTEGER);
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
            commandContext.sendMessage(Message.raw("Please provide a world ID or run this as a player"));
            return CompletableFuture.completedFuture(null);
        }

        World finalWorld = world;
        return this.runAsync(commandContext, () -> {
            LootChestConfig config = finalWorld.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

            boolean anyArgProvided = commandContext.provided(modeArg) || commandContext.provided(daysArg) ||
                    commandContext.provided(hoursArg) || commandContext.provided(minutesArg) ||
                    commandContext.provided(secondsArg);

            if (!anyArgProvided) {
                config.setLootResetMode(0);
                config.setNextLootResetDaysInterval(0);
                config.setNextLootResetHoursInterval(0);
                config.setNextLootResetMinutesInterval(0);
                config.setNextLootResetSecondsInterval(0);
                config.setNextLootReset(-1);
                commandContext.sendMessage(Message.raw("Auto-reset deactivated"));
                return;
            }

            int mode = commandContext.provided(modeArg) ? modeArg.get(commandContext) : config.getLootResetMode();
            int d = commandContext.provided(daysArg) ? Math.max(0, daysArg.get(commandContext)) : config.getNextLootResetDaysInterval();
            int h = commandContext.provided(hoursArg) ? Math.max(0, hoursArg.get(commandContext)) : config.getNextLootResetHoursInterval();
            int m = commandContext.provided(minutesArg) ? Math.max(0, minutesArg.get(commandContext)) : config.getNextLootResetMinutesInterval();
            int s = commandContext.provided(secondsArg) ? Math.max(0, secondsArg.get(commandContext)) : config.getNextLootResetSecondsInterval();

            config.setLootResetMode(mode);
            config.setNextLootResetDaysInterval(d);
            config.setNextLootResetHoursInterval(h);
            config.setNextLootResetMinutesInterval(m);
            config.setNextLootResetSecondsInterval(s);

            if (mode == 0 || (d == 0 && h == 0 && m == 0 && s == 0)) {
                config.setNextLootReset(-1);
                commandContext.sendMessage(Message.raw("Auto-reset deactivated"));
                return;
            }

            LocalDateTime baseTime;
            String modeName;

            if (mode == 1) {
                WorldTimeResource worldTimeResource = finalWorld.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
                baseTime = worldTimeResource.getGameDateTime();
                modeName = "Game Time";
            } else {
                baseTime = LocalDateTime.now();
                modeName = "Real Time";
            }

            LocalDateTime target = baseTime
                    .plusDays(d)
                    .plusHours(h)
                    .plusMinutes(m)
                    .plusSeconds(s);

            long epochSeconds = target.atZone(ZoneId.systemDefault()).toEpochSecond();
            config.setNextLootReset(epochSeconds);

            commandContext.sendMessage(Message.raw(String.format(
                    "Loot reset [%s] set for: %dd %dh %dm %ds. Next reset at: %s",
                    modeName, d, h, m, s, target
            )));

        }, world);
    }
}
