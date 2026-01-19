package org.mimstar.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.*;

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

    public static String getDeprecatedKey(int x, int y, int z){
        return x + "," + y + "," + z;
    }

    public static String getKey(int x, int y, int z, String world_name) {
        return x + "," + y + "," + z + "," + world_name;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PlayerLoot(this);
    }

    public boolean hasDeprecatedData(int x, int y, int z){
        return lootData.containsKey(getDeprecatedKey(x, y, z));
    }

    public void replaceDeprecatedData(int x, int y, int z, String world_name){
        String json = lootData.get(getDeprecatedKey(x, y, z));
        if (json != null) {
            lootData.put(getKey(x, y, z, world_name), json);
            lootData.remove(getDeprecatedKey(x, y, z));
        }
    }

    public boolean hasData(int x, int y, int z, String world_name) {
        return lootData.containsKey(getKey(x, y, z, world_name));
    }

    public List<ItemStack> getInventory(int x, int y, int z, String world_name) {
        String json = lootData.get(getKey(x, y, z, world_name));
        if (json == null) return new ArrayList<>();
        return deserialize(json);
    }

    public void setInventory(int x, int y, int z, String world_name, List<ItemStack> items) {
        String json = serialize(items);
        lootData.put(getKey(x, y, z, world_name), json);
    }

    public static String serialize(List<ItemStack> items) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);

            if (stack != null) {
                BsonDocument doc = new BsonDocument();

                doc.append("id", new BsonString(stack.getItemId()));
                doc.append("q", new BsonInt32(stack.getQuantity()));

                doc.append("d", new BsonDouble(stack.getDurability()));
                doc.append("md", new BsonDouble(stack.getMaxDurability()));

                if (stack.getMetadata() != null) {
                    doc.append("meta", stack.getMetadata());
                }

                jsonBuilder.append(doc.toJson());

            } else {
                jsonBuilder.append("null");
            }

            if (i < items.size() - 1) {
                jsonBuilder.append(",");
            }
        }

        jsonBuilder.append("]");

        return jsonBuilder.toString();
    }


    public static List<ItemStack> deserialize(String json) {
        List<ItemStack> items = new ArrayList<>();

        try {
            BsonArray array = BsonArray.parse(json);

            for (BsonValue value : array) {
                if (value.isNull()) {
                    items.add(null);
                    continue;
                }

                if (value.isDocument()) {
                    BsonDocument doc = value.asDocument();

                    String itemId = doc.getString("id").getValue();
                    int quantity = doc.getInt32("q").getValue();
                    double durability = doc.getDouble("d").getValue();
                    double maxDurability = doc.getDouble("md").getValue();

                    BsonDocument metadata = null;
                    if (doc.containsKey("meta")) {
                        metadata = doc.getDocument("meta");
                    }

                    items.add(new ItemStack(itemId, quantity, durability, maxDurability, metadata));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to deserialize inventory BSON: " + e.getMessage());
        }

        return items;
    }
}