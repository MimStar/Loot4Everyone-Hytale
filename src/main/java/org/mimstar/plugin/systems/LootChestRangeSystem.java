package org.mimstar.plugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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

public class LootChestRangeSystem extends EntityTickingSystem<EntityStore> {
    private int tickTimer = 0;

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        tickTimer++;
        if (tickTimer < 20) return;
        tickTimer = 0;

        commandBuffer.run(deferredStore -> {
            try {

                Player player = archetypeChunk.getComponent(index, Player.getComponentType());
                PlayerLoot playerLoot = archetypeChunk.getComponent(index, Loot4Everyone.get().getPlayerLootcomponentType());
                PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());

                if (player == null || playerLoot == null || playerRef == null) return;

                Vector3d playerPos = player.getTransformComponent().getPosition();

                var world = player.getWorld();
                var chunkStore = world.getChunkStore();
                if (chunkStore == null) return;

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

                                                ZoneBiomeResult result = chunkGenerator.getZoneBiomeResultAt(seed, x, z);
                                                String zoneName = result.getZoneResult().getZone().name();
                                                dropListName = zoneName.replace("_", "_Encounters_");

                                                lootChestTemplate.setDropList(x, y, z, dropListName);
                                            }

                                            playerLoot.setDiscovered(x, y, z, world.getName(), true);

                                            LootChestConfig lootChestConfig = world.getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

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