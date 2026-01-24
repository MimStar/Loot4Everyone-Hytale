package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.PlayerLoot;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ResetLootChestCommand extends AbstractPlayerCommand {
    private static final int BATCH_SIZE = 20;

    public ResetLootChestCommand() {
        super("resetlc", "A command to reset one or multiple loot chests for specified players");
    }

    OptionalArg<String> playerArg = this.withOptionalArg("player", "Name or UUID of the player", ArgTypes.STRING);
    OptionalArg<Boolean> boolArg = this.withOptionalArg("all", "True to reset all loot chests", ArgTypes.BOOLEAN);

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String playerIdentifier = playerArg.provided(commandContext) ? playerArg.get(commandContext) : null;
        boolean resetAllChests = boolArg.provided(commandContext) && boolArg.get(commandContext);

        Player executor = store.getComponent(ref, Player.getComponentType());
        String worldName = executor.getWorld().getName();

        Consumer<PlayerLoot> resetAction;

        if (!resetAllChests) {
            Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);

            BlockState state = world.getState(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), true);
            if (!(state instanceof ItemContainerState itemContainerState)) {
                executor.sendMessage(Message.raw("Please look at a loot container!"));
                return;
            }

            if (!itemContainerState.getWindows().isEmpty()) {
                executor.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
                return;
            }

            targetBlock = new Vector3i(itemContainerState.getBlockX(),itemContainerState.getBlockY(),itemContainerState.getBlockZ());

            LootChestTemplate lootChestTemplate = executor.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (targetBlock == null || !lootChestTemplate.hasTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {
                executor.sendMessage(Message.raw("Please look at a valid loot chest to reset it (or use --all true for all chests)."));
                return;
            }

            final Vector3i finalTargetBlock = targetBlock;
            resetAction = (playerLoot) -> {
                if (!playerLoot.isFirstTime(finalTargetBlock.getX(), finalTargetBlock.getY(), finalTargetBlock.getZ(), worldName)) {
                    playerLoot.resetChest(finalTargetBlock.getX(), finalTargetBlock.getY(), finalTargetBlock.getZ(), worldName);
                }
            };
        } else {
            resetAction = PlayerLoot::resetAllChests;
        }

        Set<UUID> targetsToProcess = new HashSet<>();
        PlayerStorage storage = Universe.get().getPlayerStorage();

        try {
            if (playerIdentifier != null) {
                final UUID[] targetUuid = {null};
                try {
                    targetUuid[0] = UUID.fromString(playerIdentifier);
                    targetsToProcess.add(targetUuid[0]);
                } catch (IllegalArgumentException e) {
                    String searchName = playerIdentifier.toLowerCase();

                    PlayerRef targetPlayerRef = Universe.get().getPlayerByUsername(playerIdentifier, NameMatching.EXACT);
                    if (targetPlayerRef != null) {
                        targetUuid[0] = targetPlayerRef.getUuid();
                        targetsToProcess.add(targetUuid[0]);
                    } else {
                        //executor.sendMessage(Message.raw("Player '" + playerIdentifier + "' is not online. Searching offline players..."));

                        Set<UUID> allPlayerUuids = storage.getPlayers();
                        List<CompletableFuture<UUID>> searchFutures = new ArrayList<>();

                        for (UUID uuid : allPlayerUuids) {
                            searchFutures.add(
                                    storage.load(uuid)
                                            .thenApply(holder -> {
                                                Nameplate nameplate = holder.getComponent(Nameplate.getComponentType());
                                                if (nameplate != null && nameplate.getText().toLowerCase().equals(searchName)) {
                                                    return uuid;
                                                }
                                                return null;
                                            })
                                            .exceptionally(ex -> null)
                            );
                        }

                        // Wait for all searches to complete
                        CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0]))
                                .thenAccept(v -> {
                                    for (CompletableFuture<UUID> future : searchFutures) {
                                        try {
                                            UUID foundUuid = future.get();
                                            if (foundUuid != null) {
                                                targetUuid[0] = foundUuid;
                                                break;
                                            }
                                        } catch (Exception _) {
                                        }
                                    }

                                    if (targetUuid[0] != null) {
                                        targetsToProcess.add(targetUuid[0]);
                                        processTargets(executor, storage, resetAction, targetsToProcess,store);
                                    } else {
                                        executor.sendMessage(Message.raw("Could not find player " + playerIdentifier));
                                    }
                                });
                        return;
                    }
                }
            } else {
                targetsToProcess.addAll(storage.getPlayers());
            }
        } catch (IOException e) {
            executor.sendMessage(Message.raw("Error retrieving player list: " + e.getMessage()));
            return;
        }

        processTargets(executor, storage, resetAction, targetsToProcess,store);
    }

    private void processTargets(Player executor, PlayerStorage storage, Consumer<PlayerLoot> resetAction, Set<UUID> targetsToProcess, Store<EntityStore> store) {
        //executor.sendMessage(Message.raw("Starting loot reset for " + targetsToProcess.size() + " players..."));

        List<UUID> offlinePlayers = new ArrayList<>();
        int onlineCount = 0;

        for (UUID uuid : targetsToProcess) {
            PlayerRef targetRef = Universe.get().getPlayer(uuid);

            if (targetRef != null && targetRef.isValid()) {
                Ref<EntityStore> targetEntityRef = targetRef.getReference();
                if (targetEntityRef != null && targetEntityRef.isValid()) {
                    PlayerLoot component = store.getComponent(targetEntityRef, Loot4Everyone.get().getPlayerLootcomponentType());
                    if (component != null) {
                        resetAction.accept(component);
                        onlineCount++;
                    }
                }
            } else {
                offlinePlayers.add(uuid);
            }
        }
        /*
        if (onlineCount > 0) {
            executor.sendMessage(Message.raw("Reset complete for " + onlineCount + " online players."));
        }
        */

        if (!offlinePlayers.isEmpty()) {
            //executor.sendMessage(Message.raw("Processing " + offlinePlayers.size() + " offline players in background..."));
            AtomicInteger processedCount = new AtomicInteger(0);
            recursiveBatchProcess(offlinePlayers.iterator(), storage, resetAction, executor, processedCount);
        } else {
            //executor.sendMessage(Message.raw("All requested operations completed."));
        }

        executor.sendMessage(Message.raw("Reset complete!"));
    }

    private void recursiveBatchProcess(Iterator<UUID> playerIterator, PlayerStorage storage, Consumer<PlayerLoot> action, Player executor, AtomicInteger counter) {
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        // Fill the current batch
        for (int i = 0; i < BATCH_SIZE && playerIterator.hasNext(); i++) {
            UUID uuid = playerIterator.next();

            CompletableFuture<Void> future = storage.load(uuid)
                    .thenApply(holder -> {
                        // Modify the data
                        PlayerLoot loot = holder.getComponent(Loot4Everyone.get().getPlayerLootcomponentType());
                        if (loot != null) {
                            action.accept(loot);
                        }
                        return holder;
                    })
                    .thenCompose(holder -> storage.save(uuid, holder))
                    .thenRun(counter::incrementAndGet)
                    .exceptionally(ex -> null);

            batchFutures.add(future);
        }

        if (batchFutures.isEmpty()) {
            //executor.sendMessage(Message.raw("Offline processing complete. Total processed: " + counter.get()));
            return;
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> recursiveBatchProcess(playerIterator, storage, action, executor, counter));
    }
}