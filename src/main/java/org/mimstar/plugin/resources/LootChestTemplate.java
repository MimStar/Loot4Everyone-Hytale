package org.mimstar.plugin.resources;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.bson.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LootChestTemplate implements Resource<ChunkStore> {

    // --- 1. Data Object ---
    public static class ChestData {
        public List<ItemStack> items = new ArrayList<>();
        public String dropList = "undefined";

        public ChestData() {}

        public ChestData(List<ItemStack> items, String dropList) {
            this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
            this.dropList = dropList != null ? dropList : "undefined";
        }

        public ChestData(ChestData other) {
            this.items = new ArrayList<>();
            if (other.items != null) {
                for (ItemStack stack : other.items) {
                    if (stack != null) {
                        this.items.add(new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getDurability(), stack.getMaxDurability(), stack.getMetadata()));
                    } else {
                        this.items.add(null);
                    }
                }
            }
            this.dropList = other.dropList;
        }

        public static final Codec<ChestData> INTERNAL_CODEC = BuilderCodec.builder(ChestData.class, ChestData::new)
                .addField(new KeyedCodec<>("Items", new ItemStackListCodec()), (d, v) -> d.items = v, d -> d.items)
                .addField(new KeyedCodec<>("DropList", Codec.STRING), (d, v) -> d.dropList = v, d -> d.dropList)
                .build();
    }

    //Migration Codec
    public static class LegacyChestDataCodec implements Codec<ChestData> {

        @Nonnull
        @Override
        public ChestData decode(@Nonnull BsonValue value, @Nonnull ExtraInfo extraInfo) {
            if (value.isString()) {
                return parseLegacyJson(value.asString().getValue());
            } else if (value.isDocument()) {
                return ChestData.INTERNAL_CODEC.decode(value, extraInfo);
            }
            return new ChestData();
        }

        @Nonnull
        @Override
        public ChestData decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
            reader.consumeWhiteSpace();

            int firstChar = reader.peek();

            if (firstChar == '"') {
                String jsonString = reader.readString();
                return parseLegacyJson(jsonString);
            }
            else if (firstChar == '[') {
                List<ItemStack> items = new ItemStackListCodec().decodeJson(reader, extraInfo);
                return new ChestData(items, "undefined");
            }
            else {
                return ChestData.INTERNAL_CODEC.decodeJson(reader, extraInfo);
            }
        }

        private ChestData parseLegacyJson(String json) {
            if (json == null || json.isEmpty()) return new ChestData();
            try {
                String trimmed = json.trim();
                if (trimmed.startsWith("[")) {
                    List<ItemStack> items = ItemStackListCodec.deserializeBsonArray(BsonArray.parse(json));
                    return new ChestData(items, "undefined");
                }
                BsonDocument doc = BsonDocument.parse(json);
                List<ItemStack> items = new ArrayList<>();
                String dropList = "undefined";

                if (doc.containsKey("items")) items = ItemStackListCodec.deserializeBsonArray(doc.getArray("items"));
                if (doc.containsKey("dropList")) dropList = doc.getString("dropList").getValue();

                return new ChestData(items, dropList);
            } catch (Exception e) {
                System.err.println("Error migrating loot template: " + e.getMessage());
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

    // --- 3. Main Codec ---
    public static final BuilderCodec<LootChestTemplate> CODEC = BuilderCodec.builder(
                    LootChestTemplate.class,
                    LootChestTemplate::new
            )
            .addField(new KeyedCodec<>("Templates", new MapCodec<>(new LegacyChestDataCodec(), ConcurrentHashMap::new)),
                    (data, value) -> data.templates = new ConcurrentHashMap<>(value),
                    data -> data.templates)
            .build();

    private Map<String, ChestData> templates;

    public LootChestTemplate() {
        this.templates = new ConcurrentHashMap<>();
    }

    public LootChestTemplate(LootChestTemplate other) {
        this.templates = new ConcurrentHashMap<>();
        for (Map.Entry<String, ChestData> entry : other.templates.entrySet()) {
            this.templates.put(entry.getKey(), new ChestData(entry.getValue()));
        }
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
        ChestData data = templates.get(getKey(x, y, z));
        return data != null ? data.items : new ArrayList<>();
    }

    public String getDropList(int x, int y, int z) {
        ChestData data = templates.get(getKey(x, y, z));
        return data != null ? data.dropList : "undefined";
    }

    public void setDropList(int x, int y, int z, String dropList) {
        ChestData data = templates.get(getKey(x, y, z));
        if (data != null) {
            data.dropList = dropList != null ? dropList : "undefined";
        }
    }

    public void saveTemplate(int x, int y, int z, List<ItemStack> items, String dropList) {
        templates.put(getKey(x, y, z), new ChestData(items, dropList));
    }

    public void removeTemplate(int x, int y, int z){
        templates.remove(getKey(x, y, z));
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
                int c = reader.peek();

                if (c == 'n' || c == 'N') {
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
            String id = "";
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
                    case "meta" -> meta = RawJsonReader.readBsonDocument(reader);
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