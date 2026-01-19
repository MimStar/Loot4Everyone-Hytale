package org.mimstar.plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.bson.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LootChestTemplate implements Resource<ChunkStore> {

    // UPDATE: Use ConcurrentHashMap in the Codec definition
    public static final BuilderCodec<LootChestTemplate> CODEC = BuilderCodec.builder(
                    LootChestTemplate.class,
                    LootChestTemplate::new
            )
            .addField(new KeyedCodec<>("Templates", new MapCodec<>(Codec.STRING, ConcurrentHashMap::new)),
                    (data, value) -> data.templates = new ConcurrentHashMap<>(value),
                    data -> data.templates)
            .build();

    private Map<String, String> templates;

    public LootChestTemplate() {
        this.templates = new ConcurrentHashMap<>();
    }

    public LootChestTemplate(LootChestTemplate other) {
        this.templates = new ConcurrentHashMap<>(other.templates);
    }

    @Nullable
    @Override
    public Resource<ChunkStore> clone() {
        return new LootChestTemplate(this);
    }

    public static String getKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public boolean hasTemplate(int x, int y, int z) {
        return templates.containsKey(getKey(x, y, z));
    }

    public List<ItemStack> getTemplate(int x, int y, int z) {
        String json = templates.get(getKey(x, y, z));

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        return InventorySerializer.deserialize(json);
    }

    public void saveTemplate(int x, int y, int z, List<ItemStack> items) {
        String json = InventorySerializer.serialize(items);
        templates.put(getKey(x, y, z), json);
    }

    public static class InventorySerializer {
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
}