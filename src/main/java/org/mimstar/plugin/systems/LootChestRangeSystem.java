package org.mimstar.plugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.TempAssetIdUtil;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.components.PlayerLoot;
import org.mimstar.plugin.resources.LootChestConfig;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class LootChestRangeSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        commandBuffer.run(deferredStore -> {
            try {
                Player player = archetypeChunk.getComponent(index, Player.getComponentType());
                PlayerLoot playerLoot = archetypeChunk.getComponent(index, Loot4Everyone.get().getPlayerLootcomponentType());
                PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());

                if (player == null || playerLoot == null || playerRef == null) return;

                playerLoot.incrementTimer();

                if (playerLoot.getTimer() < 20) {
                    return;
                }

                playerLoot.resetTimer();


                Vector3d playerPos = player.getTransformComponent().getPosition();

                var world = player.getWorld();
                var chunkStore = world.getChunkStore();
                if (chunkStore == null) return;

                LootChestConfig lootChestConfig = chunkStore.getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

                long nowSeconds = Instant.now().getEpochSecond();

                long nextLootReset = lootChestConfig.getNextLootReset();

                if (nextLootReset != -1 && nowSeconds >= nextLootReset) {

                    LocalDateTime nextResetDateTime = LocalDateTime.now()
                            .plusDays(lootChestConfig.getNextLootResetDaysInterval())
                            .plusHours(lootChestConfig.getNextLootResetHoursInterval())
                            .plusMinutes(lootChestConfig.getNextLootResetMinutesInterval())
                            .plusSeconds(lootChestConfig.getNextLootResetSecondsInterval());

                    long newResetTimestamp = nextResetDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

                    lootChestConfig.setNextLootReset(newResetTimestamp);

                    LootResetManager.runGlobalReset(world.getEntityStore().getStore());
                }

                var spatialResourceType = BlockStateModule.get().getItemContainerSpatialResourceType();
                var spatial = chunkStore.getStore().getResource(spatialResourceType);
                if (spatial == null) return;

                ObjectList<Ref<ChunkStore>> objectList = SpatialResource.getThreadLocalReferenceList();
                spatial.getSpatialStructure().collect(playerPos, 5.0, objectList);

                if (!objectList.isEmpty()) {
                    Ref<ChunkStore> ref = objectList.get(0);
                    if (ref.isValid()) {
                        BlockModule.BlockStateInfo blockInfo = ref.getStore().getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                        if (blockInfo != null) {
                            Ref<ChunkStore> chunkRef = blockInfo.getChunkRef();
                            if (chunkRef != null && chunkRef.isValid()) {
                                ItemContainerState itemContainerState = ref.getStore().getComponent(ref,
                                        BlockStateModule.get().getComponentType(ItemContainerState.class));

                                if (itemContainerState != null) {
                                    LootChestTemplate lootChestTemplate = itemContainerState.getReference().getStore()
                                            .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

                                    int x = itemContainerState.getBlockX();
                                    int y = itemContainerState.getBlockY();
                                    int z = itemContainerState.getBlockZ();

                                    if (lootChestTemplate != null && lootChestTemplate.hasTemplate(x, y, z)) {

                                        if (!playerLoot.isDiscovered(x, y, z, world.getName())) {
                                            String dropListName = lootChestTemplate.getDropList(x, y, z);

                                            if ("undefined".equals(dropListName)) {
                                                ChunkGenerator chunkGenerator = (ChunkGenerator) world.getChunkStore().getGenerator();
                                                int seed = (int) world.getWorldConfig().getSeed();

                                                ZoneBiomeResult result = chunkGenerator.getZoneBiomeResultAt(seed, x, z); //Ex: Zone1_Tier1
                                                String zoneName = result.getZoneResult().getZone().name();

                                                if (zoneName.contains("Tier5")){
                                                    int randomTier = ThreadLocalRandom.current().nextInt(1,5);
                                                    zoneName = zoneName.replace("Tier5","Tier" + randomTier);
                                                }

                                                dropListName = zoneName.replace("_", "_Encounters_");

                                                lootChestTemplate.setDropList(x, y, z, dropListName);
                                            }

                                            playerLoot.setDiscovered(x, y, z, world.getName(), true);

                                            if (lootChestConfig.isMessageAppear()){
                                                EventTitleUtil.showEventTitleToPlayer(playerRef, Message.raw("New loot chest discovered!"), Message.raw(dropListName), true);
                                                int soundEventIndex = TempAssetIdUtil.getSoundEventIndex("SFX_Memories_Unlock_Local");
                                                if (soundEventIndex > 0) {
                                                    SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (lootChestConfig.isParticlesAppear()) {
                    ObjectList<Ref<ChunkStore>> objectList1 = SpatialResource.getThreadLocalReferenceList();
                    spatial.getSpatialStructure().collect(playerPos, 15.0, objectList1);
                    if (!objectList1.isEmpty()) {
                        for (Ref<ChunkStore> ref : objectList1) {
                            if (ref.isValid()) {
                                BlockModule.BlockStateInfo blockInfo = ref.getStore().getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                                if (blockInfo != null) {
                                    Ref<ChunkStore> chunkRef = blockInfo.getChunkRef();
                                    if (chunkRef != null && chunkRef.isValid()) {
                                        ItemContainerState itemContainerState = ref.getStore().getComponent(ref,
                                                BlockStateModule.get().getComponentType(ItemContainerState.class));

                                        if (itemContainerState != null) {

                                            int x = itemContainerState.getBlockX();
                                            int y = itemContainerState.getBlockY();
                                            int z = itemContainerState.getBlockZ();

                                            LootChestTemplate lootChestTemplate = itemContainerState.getReference().getStore()
                                                    .getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

                                            if (lootChestTemplate != null && lootChestTemplate.hasTemplate(x, y, z)) {

                                                if (playerLoot.isFirstTime(x, y, z, world.getName())) {
                                                    double particle_x = itemContainerState.getBlockX() + 0.5;
                                                    double particle_y = itemContainerState.getBlockY() + 1.5;
                                                    double particle_z = itemContainerState.getBlockZ() + 0.5;

                                                    String color = lootChestConfig.getParticlesColor();

                                                    if (color.startsWith("#")) {
                                                        color = color.substring(1);
                                                    }

                                                    byte r = (byte) Integer.parseInt(color.substring(0, 2), 16);
                                                    byte g = (byte) Integer.parseInt(color.substring(2, 4), 16);
                                                    byte b = (byte) Integer.parseInt(color.substring(4, 6), 16);

                                                    ParticleUtil.spawnParticleEffect("Chest_Sparks", new Vector3d(particle_x, particle_y, particle_z), 0, 0, 0, 1, new Color(r,g,b), Collections.singletonList(player.getReference()), commandBuffer);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}