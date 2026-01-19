package org.mimstar.plugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class Loot4Everyone extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static Loot4Everyone instance;

    private ComponentType<EntityStore, OpenedContainerComponent> containerComponentType;

    private ResourceType<ChunkStore, LootChestTemplate> lootChestTemplateComponentType;

    private ComponentType<EntityStore, PlayerLoot> playerLootcomponentType;

    public Loot4Everyone(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getChunkStoreRegistry().registerSystem(new LookupSystem(BlockStateModule.get().getComponentType(ItemContainerState.class)));
        this.getEntityStoreRegistry().registerSystem(new UseBlockEventPre());
        this.getEntityStoreRegistry().registerSystem(new BreakBlockEventListener());
        this.getEntityStoreRegistry().registerSystem(new DamageBlockEventListener());
        this.containerComponentType = this.getEntityStoreRegistry()
                .registerComponent(OpenedContainerComponent.class, OpenedContainerComponent::new);

        this.getEntityStoreRegistry().registerSystem(new ContainerMonitoringSystem(this.containerComponentType));

        this.lootChestTemplateComponentType = this.getChunkStoreRegistry().registerResource(LootChestTemplate.class, "LootChestTemplate", LootChestTemplate.CODEC);

        this.playerLootcomponentType = this.getEntityStoreRegistry().registerComponent(PlayerLoot.class, "PlayerLoot", PlayerLoot.CODEC);

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, e -> {

            var playerRef = e.getPlayerRef();
            var world = e.getPlayer().getWorld();

            world.execute(() -> {
                Store<EntityStore> entityStore = playerRef.getStore();
                entityStore.ensureComponent(playerRef, getPlayerLootcomponentType());
            });
        });
    }

    public ComponentType<EntityStore, OpenedContainerComponent> getContainerComponentType() {
        return containerComponentType;
    }

    public ResourceType<ChunkStore, LootChestTemplate> getlootChestTemplateResourceType(){
        return lootChestTemplateComponentType;
    }

    public ComponentType<EntityStore, PlayerLoot> getPlayerLootcomponentType(){
        return playerLootcomponentType;
    }

    public static Loot4Everyone get() {
        return instance;
    }

    private static class LookupSystem extends RefSystem<ChunkStore>{

        private final ComponentType<ChunkStore, ItemContainerState> componentType;
        @Nonnull
        private final Set<Dependency<ChunkStore>> dependencies;

        public LookupSystem(ComponentType<ChunkStore, ItemContainerState> componentType) {
            this.componentType = componentType;
            this.dependencies = Set.of(new SystemDependency(Order.BEFORE, BlockStateModule.LegacyBlockStateRefSystem.class));
        }

        @Override
        public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
            ItemContainerState itemContainerState = store.getComponent(ref, this.componentType);

            if (itemContainerState != null && itemContainerState.getDroplist() != null) {
                LootChestTemplate lootChestTemplate = commandBuffer.getResource(Loot4Everyone.get().getlootChestTemplateResourceType());

                if (lootChestTemplate != null) {
                    int x = itemContainerState.getBlockX();
                    int y = itemContainerState.getBlockY();
                    int z = itemContainerState.getBlockZ();

                    if (!lootChestTemplate.hasTemplate(x, y, z)) {
                        List<ItemStack> items = new ArrayList<>();
                        lootChestTemplate.saveTemplate(x, y, z, items);
                    }
                }
            }
        }

        @Override
        public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return this.componentType;
        }

        @Nonnull
        public Set<Dependency<ChunkStore>> getDependencies() {
            return this.dependencies;
        }
    }
}