package org.mimstar.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLoot implements Component<EntityStore> {

    public static final BuilderCodec<PlayerLoot> CODEC = BuilderCodec.builder(
                    PlayerLoot.class,
                    PlayerLoot::new
            )
            .addField(new KeyedCodec<>("Templates", new MapCodec<>(Codec.STRING, ConcurrentHashMap::new)),
                    (data, value) -> data.lootData = new ConcurrentHashMap<>(value),
                    data -> data.lootData)
            .build();

    private Map<String, String> lootData;

    public PlayerLoot() {
        this.lootData = new ConcurrentHashMap<>();
    }

    public PlayerLoot(PlayerLoot other) {
        this.lootData = new ConcurrentHashMap<>(other.lootData);
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PlayerLoot(this);
    }

    public boolean hasData(int x, int y, int z) {
        return lootData.containsKey(LootChestTemplate.getKey(x, y, z));
    }

    public List<ItemStack> getInventory(int x, int y, int z) {
        String json = lootData.get(LootChestTemplate.getKey(x, y, z));
        if (json == null) return new ArrayList<>();
        return LootChestTemplate.InventorySerializer.deserialize(json);
    }

    public void setInventory(int x, int y, int z, List<ItemStack> items) {
        String json = LootChestTemplate.InventorySerializer.serialize(items);
        lootData.put(LootChestTemplate.getKey(x, y, z), json);
    }
}