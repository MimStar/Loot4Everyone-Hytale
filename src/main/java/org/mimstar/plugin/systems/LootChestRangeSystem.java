package org.mimstar.plugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.util.List;

public class LootChestRangeSystem extends EntityTickingSystem<EntityStore> {
    private int tickTimer = 0;

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        tickTimer++;
        if (tickTimer < 40) return;
        tickTimer = 0;

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        Vector3d playerPos = player.getTransformComponent().getPosition();

        // Get the Spatial Resource for Containers
        var world = player.getWorld();
        var spatial = world.getChunkStore().getStore().getResource(BlockStateModule.get().getItemContainerSpatialResourceType());
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
                        ItemContainerState itemContainerState = ref.getStore().getComponent(ref, BlockStateModule.get().getComponentType(ItemContainerState.class));

                        if (itemContainerState != null) {
                            LootChestTemplate lootChestTemplate = itemContainerState.getReference().getStore().getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

                            int x = itemContainerState.getBlockX();
                            int y = itemContainerState.getBlockY();
                            int z = itemContainerState.getBlockZ();

                            if (lootChestTemplate.hasTemplate(x, y, z)) {
                                PlayerLoot playerLoot = store.getComponent(player.getReference(), Loot4Everyone.get().getPlayerLootcomponentType());

                                if (!playerLoot.isDiscovered(x, y, z, player.getWorld().getName())) {

                                    String dropListName = lootChestTemplate.getDropList(x, y, z);

                                    if ("undefined".equals(dropListName)) {
                                        ChunkGenerator chunkGenerator = (ChunkGenerator) player.getWorld().getChunkStore().getGenerator();
                                        int seed = (int) player.getWorld().getWorldConfig().getSeed();

                                        ZoneBiomeResult result = chunkGenerator.getZoneBiomeResultAt(seed, x, z);
                                        String zoneName = result.getZoneResult().getZone().name(); // Ex: Zone1_Tier1

                                        dropListName = zoneName.replace("_", "_Encounters_");

                                        lootChestTemplate.setDropList(x, y, z, dropListName);
                                    }

                                    playerLoot.setDiscovered(x, y, z, player.getWorld().getName(), true);

                                    PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());

                                    if (playerRef != null) {
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
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}