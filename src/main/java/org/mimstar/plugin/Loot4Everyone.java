package org.mimstar.plugin;

import com.hypixel.hytale.builtin.adventure.stash.StashGameplayConfig;
import com.hypixel.hytale.builtin.adventure.stash.StashPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.mimstar.plugin.commands.*;
import org.mimstar.plugin.components.OpenedContainerComponent;
import org.mimstar.plugin.components.PlayerLoot;
import org.mimstar.plugin.events.*;
import org.mimstar.plugin.resources.LootChestConfig;
import org.mimstar.plugin.resources.LootChestTemplate;
import org.mimstar.plugin.systems.ContainerMonitoringSystem;
import org.mimstar.plugin.systems.LootChestRangeSystem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class Loot4Everyone extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static Loot4Everyone instance;

    private ComponentType<EntityStore, OpenedContainerComponent> containerComponentType;

    private ResourceType<ChunkStore, LootChestTemplate> lootChestTemplateComponentType;

    private ResourceType<ChunkStore, LootChestConfig> lootChestConfigResourceType;

    private ComponentType<EntityStore, PlayerLoot> playerLootcomponentType;

    public Loot4Everyone(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getChunkStoreRegistry().registerSystem(new Loot4Everyone.LookupSystem(ItemContainerBlock.getComponentType()));
        this.getEntityStoreRegistry().registerSystem(new UseBlockEventPre());
        this.getEntityStoreRegistry().registerSystem(new BreakBlockEventListener());
        this.getEntityStoreRegistry().registerSystem(new DamageBlockEventListener());
        this.getEntityStoreRegistry().registerSystem(new PlaceBlockEventListener());
        this.getEventRegistry().registerGlobal(StartWorldEvent.class, StartWorldEventListener::onStartWorldEvent);
        this.containerComponentType = this.getEntityStoreRegistry()
                .registerComponent(OpenedContainerComponent.class, OpenedContainerComponent::new);

        this.getEntityStoreRegistry().registerSystem(new ContainerMonitoringSystem(this.containerComponentType));

        this.getEntityStoreRegistry().registerSystem(new LootChestRangeSystem());

        this.lootChestTemplateComponentType = this.getChunkStoreRegistry().registerResource(LootChestTemplate.class, "LootChestTemplate", LootChestTemplate.CODEC);

        this.lootChestConfigResourceType = this.getChunkStoreRegistry().registerResource(LootChestConfig.class, "Loot_chest_config", LootChestConfig.CODEC);

        this.playerLootcomponentType = this.getEntityStoreRegistry().registerComponent(PlayerLoot.class, "PlayerLoot", PlayerLoot.CODEC);

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, e -> {

            var playerRef = e.getPlayerRef();
            var world = e.getPlayer().getWorld();

            world.execute(() -> {
                Store<EntityStore> entityStore = playerRef.getStore();
                entityStore.ensureComponent(playerRef, getPlayerLootcomponentType());
            });
        });

        this.getCommandRegistry().registerCommand(new SetBreakRuleCommand());
        this.getCommandRegistry().registerCommand(new ResetLootChestCommand());
        this.getCommandRegistry().registerCommand(new SetAutoResetLootChestCommand());
        this.getCommandRegistry().registerCommand(new SetRandomRuleCommand());
        this.getCommandRegistry().registerCommand(new GenerateLootChestCommand());
        this.getCommandRegistry().registerCommand(new DeleteLootChestCommand());
        this.getCommandRegistry().registerCommand(new EditLootChestCommand());
        this.getCommandRegistry().registerCommand(new SetMessageRuleCommand());
        this.getCommandRegistry().registerCommand(new SetParticleRuleCommand());
        this.getCommandRegistry().registerCommand(new LootConfigCommand());
    }

    public ComponentType<EntityStore, OpenedContainerComponent> getContainerComponentType() {
        return containerComponentType;
    }

    public ResourceType<ChunkStore, LootChestTemplate> getlootChestTemplateResourceType(){
        return lootChestTemplateComponentType;
    }

    public ResourceType<ChunkStore, LootChestConfig> getLootChestConfigResourceType() {
        return lootChestConfigResourceType;
    }

    public ComponentType<EntityStore, PlayerLoot> getPlayerLootcomponentType(){
        return playerLootcomponentType;
    }

    public static Loot4Everyone get() {
        return instance;
    }

    private static class LookupSystem extends RefSystem<ChunkStore>{

        @Nonnull
        private final ComponentType<ChunkStore, ItemContainerBlock> itemContainerStateComponentType;

        @Nonnull
        private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType = BlockModule.BlockStateInfo.getComponentType();

        @Nonnull
        private final Query<ChunkStore> query;

        public LookupSystem(@Nonnull ComponentType<ChunkStore, ItemContainerBlock> itemContainerStateComponentType) {
            this.itemContainerStateComponentType = itemContainerStateComponentType;
            this.query = Query.and(itemContainerStateComponentType, this.blockStateInfoComponentType);
        }

        @Override
        public Query<ChunkStore> getQuery() {
            return this.query;
        }

        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

            ItemContainerBlock itemContainerStateComponent = store.getComponent(ref, this.itemContainerStateComponentType);

            assert itemContainerStateComponent != null;

            BlockModule.BlockStateInfo blockStateInfo = store.getComponent(ref, this.blockStateInfoComponentType);

            assert blockStateInfo != null;

            LootChestTemplate lootChestTemplate = commandBuffer.getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

            assert lootChestTemplate != null;

            if (itemContainerStateComponent.getDroplist() != null) {
                WorldChunk wc = blockStateInfo.getSectionRef().getStore().getComponent(blockStateInfo.getSectionRef(), WorldChunk.getComponentType());
                int x = ChunkUtil.xFromIndex(blockStateInfo.getIndex());
                int y = ChunkUtil.yFromIndex(blockStateInfo.getIndex());
                int z = ChunkUtil.zFromIndex(blockStateInfo.getIndex());
                int worldX = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), x);
                int worldZ = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), z);
                String droplist = itemContainerStateComponent.getDroplist();

                if (!lootChestTemplate.hasTemplate(worldX, y, worldZ)) {
                    List<ItemStack> items = new ArrayList<>();
                    lootChestTemplate.saveTemplate(worldX, y, worldZ, items, droplist);

                    itemContainerStateComponent.setDroplist(null);
                }

            }
        }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

        }
    }
}