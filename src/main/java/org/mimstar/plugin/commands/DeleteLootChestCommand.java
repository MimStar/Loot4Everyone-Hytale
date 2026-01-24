package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
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

public class DeleteLootChestCommand extends AbstractPlayerCommand {
    private static final int BATCH_SIZE = 20;

    public DeleteLootChestCommand() {
        super("deletelc", "A command to delete a loot container");
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player executor = store.getComponent(ref, Player.getComponentType());
        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);

        if (targetBlock == null) {
            executor.sendMessage(Message.raw("Please look at a block that can store items!"));
            return;
        }

        BlockState state = world.getState(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), true);
        if (state instanceof ItemContainerState itemContainerState) {

            if (!itemContainerState.getWindows().isEmpty()) {
                executor.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
                return;
            }

            targetBlock = new Vector3i(itemContainerState.getBlockX(),itemContainerState.getBlockY(),itemContainerState.getBlockZ());

            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (lootChestTemplate.hasTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {

                lootChestTemplate.removeTemplate(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());

                cleanupPlayerData(executor, targetBlock, world.getName(), store);
            } else {
                executor.sendMessage(Message.raw("Please look at a registered loot container!"));
            }
        } else {
            executor.sendMessage(Message.raw("Please look at a loot container!"));
        }
    }

    private void cleanupPlayerData(Player executor, Vector3i targetBlock, String worldName, Store<EntityStore> store) {
        PlayerStorage storage = Universe.get().getPlayerStorage();
        Set<UUID> allPlayers;
        try {
            allPlayers = storage.getPlayers();
        } catch (IOException e) {
            executor.sendMessage(Message.raw("Error retrieving player list for cleanup: " + e.getMessage()));
            return;
        }

        Consumer<PlayerLoot> resetAction = (playerLoot) -> {

            if (!playerLoot.isFirstTime(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), worldName)) {
                playerLoot.resetChest(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), worldName);
            }
        };

        List<UUID> offlinePlayers = new ArrayList<>();
        //int onlineProcessed = 0;

        for (UUID uuid : allPlayers) {
            PlayerRef targetRef = Universe.get().getPlayer(uuid);
            if (targetRef != null && targetRef.isValid()) {
                Ref<EntityStore> targetEntityRef = targetRef.getReference();
                if (targetEntityRef != null && targetEntityRef.isValid()) {
                    PlayerLoot component = store.getComponent(targetEntityRef, Loot4Everyone.get().getPlayerLootcomponentType());
                    if (component != null) {
                        resetAction.accept(component);
                        //onlineProcessed++;
                    }
                }
            } else {
                offlinePlayers.add(uuid);
            }
        }

        if (!offlinePlayers.isEmpty()) {
            AtomicInteger processedCount = new AtomicInteger(0);
            recursiveBatchProcess(offlinePlayers.iterator(), storage, resetAction, executor, processedCount);
        } else {
            executor.sendMessage(Message.raw("Loot container deleted!"));
        }
    }

    private void recursiveBatchProcess(Iterator<UUID> playerIterator, PlayerStorage storage, Consumer<PlayerLoot> action, Player executor, AtomicInteger counter) {
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE && playerIterator.hasNext(); i++) {
            UUID uuid = playerIterator.next();

            CompletableFuture<Void> future = storage.load(uuid)
                    .thenApply(holder -> {
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
            executor.sendMessage(Message.raw("Loot container deleted!"));
            return;
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> recursiveBatchProcess(playerIterator, storage, action, executor, counter));
    }
}