package org.mimstar.plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.joml.Vector3i;
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
        Vector3i targetBlock = TargetUtil.getTargetBlock(ref, 10.0, store);

        if (targetBlock == null) {
            commandContext.sendMessage(Message.raw("Please look at a block that can store items!"));
            return;
        }

        ChunkStore chunkStore = world.getChunkStore();

        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, targetBlock.x(), targetBlock.y(), targetBlock.z());

        if (blockRef == null) return;

        ItemContainerBlock itemContainerState = chunkStore.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());

        if (itemContainerState != null) {

            if (!itemContainerState.getWindows().isEmpty()) {
                commandContext.sendMessage(Message.raw("Someone is looking at the loot container, try again later."));
                return;
            }

            LootChestTemplate lootChestTemplate = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());
            if (lootChestTemplate.hasTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z())) {

                lootChestTemplate.removeTemplate(targetBlock.x(), targetBlock.y(), targetBlock.z());

                cleanupPlayerData(commandContext, targetBlock, world.getName(), store);
            } else {
                commandContext.sendMessage(Message.raw("Please look at a registered loot container!"));
            }
        } else {
            commandContext.sendMessage(Message.raw("Please look at a loot container!"));
        }
    }

    private void cleanupPlayerData(CommandContext context, Vector3i targetBlock, String worldName, Store<EntityStore> store) {
        PlayerStorage storage = Universe.get().getPlayerStorage();
        Set<UUID> allPlayers;
        try {
            allPlayers = storage.getPlayers();
        } catch (IOException e) {
            context.sendMessage(Message.raw("Error retrieving player list for cleanup: " + e.getMessage()));
            return;
        }

        Consumer<PlayerLoot> resetAction = (playerLoot) -> {

            if (!playerLoot.isFirstTime(targetBlock.x(), targetBlock.y(), targetBlock.z(), worldName)) {
                playerLoot.resetChest(targetBlock.x(), targetBlock.y(), targetBlock.z(), worldName);
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
            recursiveBatchProcess(offlinePlayers.iterator(), storage, resetAction, context, processedCount);
        } else {
            context.sendMessage(Message.raw("Loot container deleted!"));
        }
    }

    private void recursiveBatchProcess(Iterator<UUID> playerIterator, PlayerStorage storage, Consumer<PlayerLoot> action, CommandContext context, AtomicInteger counter) {
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
                    .thenCompose(holder -> storage.save(uuid, holder,true))
                    .thenRun(counter::incrementAndGet)
                    .exceptionally(ex -> null);

            batchFutures.add(future);
        }

        if (batchFutures.isEmpty()) {
            context.sendMessage(Message.raw("Loot container deleted!"));
            return;
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> recursiveBatchProcess(playerIterator, storage, action, context, counter));
    }
}