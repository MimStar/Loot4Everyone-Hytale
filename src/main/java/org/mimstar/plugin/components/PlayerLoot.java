package org.mimstar.plugin.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLoot implements Component<EntityStore> {

    public static class ChestData {
        public List<ItemStack> items = new ArrayList<>();
        public boolean discovered = false;

        public ChestData() {}

        public ChestData(List<ItemStack> items, boolean discovered) {
            this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
            this.discovered = discovered;
        }

        public ChestData(ChestData other) {
            this.items = new ArrayList<>(other.items);
            this.discovered = other.discovered;
        }

        public static final Codec<ChestData> INTERNAL_CODEC = BuilderCodec.builder(ChestData.class, ChestData::new)
                .addField(new KeyedCodec<>("Items", new ItemStackListCodec()), (d, v) -> d.items = v, d -> d.items)
                .addField(new KeyedCodec<>("Discovered", Codec.BOOLEAN), (d, v) -> d.discovered = v, d -> d.discovered)
                .build();
    }

    public static class LegacyChestDataCodec implements Codec<ChestData> {
        @Nonnull
        @Override
        public ChestData decode(@Nonnull BsonValue value, @Nonnull ExtraInfo extraInfo) {
            if (value.isString()) return parseLegacyJson(value.asString().getValue());
            else if (value.isArray()) {
                List<ItemStack> items = ItemStackListCodec.deserializeBsonArray(value.asArray());
                return new ChestData(items, !items.isEmpty());
            }
            else if (value.isDocument()) return ChestData.INTERNAL_CODEC.decode(value, extraInfo);
            return new ChestData();
        }

        @Nonnull
        @Override
        public ChestData decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
            reader.consumeWhiteSpace();
            if (reader.peekFor('"')) return parseLegacyJson(reader.readString());
            else if (reader.peekFor('[')) {
                List<ItemStack> items = new ItemStackListCodec().decodeJson(reader, extraInfo);
                return new ChestData(items, !items.isEmpty());
            }
            else return ChestData.INTERNAL_CODEC.decodeJson(reader, extraInfo);
        }

        private ChestData parseLegacyJson(String json) {
            if (json == null || json.isEmpty()) return new ChestData();
            try {
                String trimmed = json.trim();
                if (trimmed.startsWith("[")) {
                    List<ItemStack> items = ItemStackListCodec.deserializeBsonArray(BsonArray.parse(json));
                    return new ChestData(items, !items.isEmpty());
                }
                BsonDocument doc = BsonDocument.parse(json);
                List<ItemStack> items = new ArrayList<>();
                boolean discovered = false;

                if (doc.containsKey("Items")) {
                    items = ItemStackListCodec.deserializeBsonArray(doc.getArray("Items"));
                    if (!doc.containsKey("Discovered")) discovered = !items.isEmpty();
                }
                if (doc.containsKey("Discovered")) discovered = doc.getBoolean("Discovered").getValue();

                return new ChestData(items, discovered);
            } catch (Exception e) {
                System.err.println("Error migrating player loot data: " + e.getMessage());
                return new ChestData();
            }
        }

        @Nonnull
        @Override
        public BsonValue encode(@Nonnull ChestData data, @Nonnull ExtraInfo extraInfo) {
            return ChestData.INTERNAL_CODEC.encode(data, extraInfo);
        }

        @Nonnull
        @Override
        public Schema toSchema(@Nonnull SchemaContext context) {
            return ChestData.INTERNAL_CODEC.toSchema(context);
        }
    }

    public static final BuilderCodec<PlayerLoot> CODEC = BuilderCodec.builder(PlayerLoot.class, PlayerLoot::new)
            .addField(new KeyedCodec<>("Templates", new MapCodec<>(new LegacyChestDataCodec(), ConcurrentHashMap::new)),
                    (data, value) -> data.lootData = new ConcurrentHashMap<>(value),
                    data -> data.lootData)
            .build();

    private Map<String, ChestData> lootData;

    public PlayerLoot() {
        this.lootData = new ConcurrentHashMap<>();
    }

    public PlayerLoot(PlayerLoot other) {
        this.lootData = new ConcurrentHashMap<>();
        for (Map.Entry<String, ChestData> entry : other.lootData.entrySet()) {
            this.lootData.put(entry.getKey(), new ChestData(entry.getValue()));
        }
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PlayerLoot(this);
    }

    public static String getDeprecatedKey(int x, int y, int z){
        return x + "," + y + "," + z;
    }

    public static String getKey(int x, int y, int z, String world_name) {
        return x + "," + y + "," + z + "," + world_name;
    }

    public boolean hasDeprecatedData(int x, int y, int z){
        return lootData.containsKey(getDeprecatedKey(x, y, z));
    }

    public void replaceDeprecatedData(int x, int y, int z, String world_name){
        String oldKey = getDeprecatedKey(x, y, z);
        ChestData data = lootData.get(oldKey);

        if (data != null) {
            lootData.put(getKey(x, y, z, world_name), data);
            lootData.remove(oldKey);
        }
    }

    public void resetChest(int x, int y, int z, String world_name) {

        lootData.remove(getKey(x,y,z,world_name));

        lootData.remove(getDeprecatedKey(x,y,z));
    }

    public void resetAllChests() {
        lootData.clear();
    }

    public boolean isFirstTime(int x, int y, int z, String world_name) {
        String key = getKey(x, y, z, world_name);

        if (!lootData.containsKey(key) && hasDeprecatedData(x, y, z)) {
            replaceDeprecatedData(x, y, z, world_name);
        }

        ChestData data = lootData.get(key);

        return data == null || data.items.isEmpty();
    }


    public boolean isDiscovered(int x, int y, int z, String world_name) {
        if (!lootData.containsKey(getKey(x, y, z, world_name)) && hasDeprecatedData(x, y, z)) {
            replaceDeprecatedData(x, y, z, world_name);
        }
        ChestData data = lootData.get(getKey(x, y, z, world_name));
        return data != null && data.discovered;
    }

    public void setDiscovered(int x, int y, int z, String world_name, boolean discovered) {
        String key = getKey(x, y, z, world_name);
        lootData.compute(key, (k, v) -> {
            if (v == null) {
                return new ChestData(new ArrayList<>(), discovered);
            }
            v.discovered = discovered;
            return v;
        });
    }

    public List<ItemStack> getInventory(int x, int y, int z, String world_name) {
        if (!lootData.containsKey(getKey(x, y, z, world_name)) && hasDeprecatedData(x, y, z)) {
            replaceDeprecatedData(x, y, z, world_name);
        }

        ChestData data = lootData.get(getKey(x, y, z, world_name));
        return data != null ? new ArrayList<>(data.items) : new ArrayList<>();
    }

    public void setInventory(int x, int y, int z, String world_name, List<ItemStack> items) {
        String key = getKey(x, y, z, world_name);
        lootData.compute(key, (k, v) -> {
            if (v == null) {
                return new ChestData(new ArrayList<>(items), true);
            }
            v.items = new ArrayList<>(items);
            return v;
        });
    }

    public static class ItemStackListCodec implements Codec<List<ItemStack>> {
        @Nonnull
        @Override
        public List<ItemStack> decode(@Nonnull BsonValue bsonValue, @Nonnull ExtraInfo extraInfo) {
            return deserializeBsonArray(bsonValue.asArray());
        }

        @Nonnull
        @Override
        public List<ItemStack> decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
            List<ItemStack> list = new ArrayList<>();
            reader.expect('[');
            reader.consumeWhiteSpace();
            if (reader.tryConsume(']')) return list;

            while (true) {
                reader.consumeWhiteSpace();
                if (reader.peekFor('n') || reader.peekFor('N')) {
                    reader.readNullValue();
                    list.add(null);
                } else {
                    list.add(decodeItemStackJson(reader));
                }
                reader.consumeWhiteSpace();
                if (reader.tryConsume(']')) break;
                reader.expect(',');
            }
            return list;
        }

        private ItemStack decodeItemStackJson(RawJsonReader reader) throws IOException {
            reader.expect('{');
            String id = "air";
            int q = 1;
            double d = 0;
            double md = 0;
            BsonDocument meta = null;

            reader.consumeWhiteSpace();
            if (reader.tryConsume('}')) return new ItemStack("air", 0, 0, 0, null);

            while (true) {
                reader.consumeWhiteSpace();
                String key = reader.readString();
                reader.consumeWhiteSpace();
                reader.expect(':');
                reader.consumeWhiteSpace();

                switch (key) {
                    case "id" -> id = reader.readString();
                    case "q" -> q = reader.readIntValue();
                    case "d" -> d = reader.readDoubleValue();
                    case "md" -> md = reader.readDoubleValue();
                    default -> reader.skipValue();
                }

                reader.consumeWhiteSpace();
                if (reader.tryConsume('}')) break;
                reader.expect(',');
            }
            return new ItemStack(id, q, d, md, meta);
        }

        public static List<ItemStack> deserializeBsonArray(BsonArray array) {
            List<ItemStack> items = new ArrayList<>();
            if (array == null) return items;
            for (BsonValue value : array) {
                if (value.isNull()) { items.add(null); continue; }
                if (value.isDocument()) {
                    try {
                        BsonDocument doc = value.asDocument();
                        String itemId = doc.getString("id").getValue();
                        int quantity = doc.getInt32("q").getValue();
                        double durability = doc.getDouble("d").getValue();
                        double maxDurability = doc.getDouble("md").getValue();
                        BsonDocument metadata = doc.containsKey("meta") ? doc.getDocument("meta") : null;
                        items.add(new ItemStack(itemId, quantity, durability, maxDurability, metadata));
                    } catch (Exception ignored) {}
                }
            }
            return items;
        }

        @Nonnull
        @Override
        public BsonValue encode(@Nonnull List<ItemStack> items, @Nonnull ExtraInfo extraInfo) {
            BsonArray array = new BsonArray();
            for (ItemStack stack : items) {
                if (stack != null) {
                    BsonDocument doc = new BsonDocument();
                    doc.append("id", new BsonString(stack.getItemId()));
                    doc.append("q", new BsonInt32(stack.getQuantity()));
                    doc.append("d", new BsonDouble(stack.getDurability()));
                    doc.append("md", new BsonDouble(stack.getMaxDurability()));
                    if (stack.getMetadata() != null) doc.append("meta", stack.getMetadata());
                    array.add(doc);
                } else {
                    array.add(new BsonNull());
                }
            }
            return array;
        }

        @Nonnull
        @Override
        public Schema toSchema(@Nonnull SchemaContext context) {
            return new ArraySchema();
        }
    }
}