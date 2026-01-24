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

    private static final String DEFAULT_DROPLIST = "undefined";
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_DROPLIST = "DropList";
    private static final String KEY_ID = "id";
    private static final String KEY_Q = "q";
    private static final String KEY_D = "d";
    private static final String KEY_MD = "md";
    private static final String KEY_META = "meta";

    public static class ChestData {
        public List<ItemStack> items;
        public String dropList;

        public ChestData() {
            this.items = new ArrayList<>();
            this.dropList = DEFAULT_DROPLIST;
        }

        public ChestData(List<ItemStack> items, String dropList) {
            this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
            this.dropList = dropList != null ? dropList : DEFAULT_DROPLIST;
        }

        public ChestData(ChestData other) {
            this.items = new ArrayList<>(other.items);
            this.dropList = other.dropList;
        }

        public static final Codec<ChestData> INTERNAL_CODEC = BuilderCodec.builder(ChestData.class, ChestData::new)
                .addField(new KeyedCodec<>(KEY_ITEMS, new ItemStackListCodec()), (d, v) -> d.items = v, d -> d.items)
                .addField(new KeyedCodec<>(KEY_DROPLIST, Codec.STRING), (d, v) -> d.dropList = v, d -> d.dropList)
                .build();
    }

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
                return parseLegacyJson(reader.readString());
            } else if (firstChar == '[') {
                List<ItemStack> items = new ItemStackListCodec().decodeJson(reader, extraInfo);
                return new ChestData(items, DEFAULT_DROPLIST);
            } else {
                // Manual Streaming Parse (Fastest & Memory Efficient)
                List<ItemStack> items = new ArrayList<>();
                String dropList = DEFAULT_DROPLIST;

                reader.expect('{');
                reader.consumeWhiteSpace();

                if (reader.tryConsume('}')) return new ChestData(items, dropList);

                while (true) {
                    reader.consumeWhiteSpace();
                    String key = reader.readString(); // Consumes key
                    reader.consumeWhiteSpace();
                    reader.expect(':');
                    reader.consumeWhiteSpace();

                    switch (key) {
                        case KEY_ITEMS, "items" -> items = new ItemStackListCodec().decodeJson(reader, extraInfo);
                        case KEY_DROPLIST, "dropList" -> dropList = reader.readString();
                        default -> reader.skipValue();
                    }

                    reader.consumeWhiteSpace();
                    if (reader.tryConsume('}')) break;
                    reader.expect(',');
                }
                return new ChestData(items, dropList);
            }
        }

        private ChestData parseLegacyJson(String json) {
            if (json == null || json.isEmpty()) return new ChestData();
            try {
                String trimmed = json.trim();
                if (trimmed.startsWith("[")) {
                    List<ItemStack> items = ItemStackListCodec.deserializeBsonArray(BsonArray.parse(json));
                    return new ChestData(items, DEFAULT_DROPLIST);
                }
                BsonDocument doc = BsonDocument.parse(json);
                List<ItemStack> items = new ArrayList<>();
                String dropList = DEFAULT_DROPLIST;

                if (doc.containsKey("items")) items = ItemStackListCodec.deserializeBsonArray(doc.getArray("items"));
                else if (doc.containsKey(KEY_ITEMS)) items = ItemStackListCodec.deserializeBsonArray(doc.getArray(KEY_ITEMS));

                if (doc.containsKey("dropList")) dropList = doc.getString("dropList").getValue();
                else if (doc.containsKey(KEY_DROPLIST)) dropList = doc.getString(KEY_DROPLIST).getValue();

                return new ChestData(items, dropList);
            } catch (Exception e) {
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

    public static final BuilderCodec<LootChestTemplate> CODEC = BuilderCodec.builder(LootChestTemplate.class, LootChestTemplate::new)
            .addField(new KeyedCodec<>("Templates", new MapCodec<>(new LegacyChestDataCodec(), ConcurrentHashMap::new)),
                    (data, value) -> data.templates = new ConcurrentHashMap<>(value),
                    data -> data.templates)
            .build();

    private Map<String, ChestData> templates = new ConcurrentHashMap<>();

    public LootChestTemplate() {}

    public LootChestTemplate(LootChestTemplate other) {
        for (Map.Entry<String, ChestData> entry : other.templates.entrySet()) {
            this.templates.put(entry.getKey(), new ChestData(entry.getValue()));
        }
    }

    @Nullable
    @Override
    public Resource<ChunkStore> clone() {
        return new LootChestTemplate(this);
    }

    public static String getKey(int x, int y, int z) { return x + "," + y + "," + z; }
    public boolean hasTemplate(int x, int y, int z) { return templates.containsKey(getKey(x, y, z)); }
    public List<ItemStack> getTemplate(int x, int y, int z) {
        ChestData data = templates.get(getKey(x, y, z));
        return data != null ? data.items : new ArrayList<>();
    }
    public String getDropList(int x, int y, int z) {
        ChestData data = templates.get(getKey(x, y, z));
        return data != null ? data.dropList : DEFAULT_DROPLIST;
    }
    public void setDropList(int x, int y, int z, String dropList) {
        ChestData data = templates.get(getKey(x, y, z));
        if (data != null) data.dropList = dropList != null ? dropList : DEFAULT_DROPLIST;
    }
    public void saveTemplate(int x, int y, int z, List<ItemStack> items, String dropList) {
        templates.put(getKey(x, y, z), new ChestData(items, dropList));
    }
    public void removeTemplate(int x, int y, int z){ templates.remove(getKey(x, y, z)); }


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
            reader.consumeWhiteSpace();

            if (reader.tryConsume('}')) return new ItemStack("air", 0, 0, 0, null);

            String id = "";
            int q = 1;
            double d = 0;
            double md = 0;
            BsonDocument meta = null;

            while (true) {
                reader.consumeWhiteSpace();
                String key = reader.readString();
                reader.consumeWhiteSpace();
                reader.expect(':');
                reader.consumeWhiteSpace();

                switch (key) {
                    case KEY_ID -> {
                        id = reader.readString().intern();
                    }
                    case KEY_Q -> q = reader.readIntValue();
                    case KEY_D -> d = reader.readDoubleValue();
                    case KEY_MD -> md = reader.readDoubleValue();
                    case KEY_META -> meta = RawJsonReader.readBsonDocument(reader);
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
                        // Intern ID here as well
                        String itemId = doc.getString(KEY_ID).getValue().intern();
                        int quantity = doc.getInt32(KEY_Q).getValue();
                        double durability = doc.getDouble(KEY_D).getValue();
                        double maxDurability = doc.getDouble(KEY_MD).getValue();
                        BsonDocument metadata = doc.containsKey(KEY_META) ? doc.getDocument(KEY_META) : null;
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
                    doc.append(KEY_ID, new BsonString(stack.getItemId()));
                    doc.append(KEY_Q, new BsonInt32(stack.getQuantity()));
                    doc.append(KEY_D, new BsonDouble(stack.getDurability()));
                    doc.append(KEY_MD, new BsonDouble(stack.getMaxDurability()));
                    if (stack.getMetadata() != null) doc.append(KEY_META, stack.getMetadata());
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