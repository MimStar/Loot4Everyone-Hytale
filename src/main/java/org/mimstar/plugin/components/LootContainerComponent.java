package org.mimstar.plugin.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.mimstar.plugin.resources.LootChestTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LootContainerComponent implements Component<ChunkStore> {
    public static final BuilderCodec<LootContainerComponent> CODEC = BuilderCodec.builder(LootContainerComponent.class, LootContainerComponent::new)
            .addField(new KeyedCodec<>("Items", new LootChestTemplate.ItemStackListCodec()),
                    (d, v) -> d.items = v, d -> d.items)
            .addField(new KeyedCodec<>("DropList", Codec.STRING),
                    (d, v) -> d.dropList = v, d -> d.dropList)
            .build();

    private List<ItemStack> items;
    private String dropList;

    public LootContainerComponent() {
        this.items = new ArrayList<>();
        this.dropList = "undefined";
    }

    public LootContainerComponent(List<ItemStack> items, String dropList) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.dropList = dropList != null ? dropList : "undefined";
    }

    public List<ItemStack> getItems() { return items; }
    public void setItems(List<ItemStack> items) { this.items = items; }

    public String getDropList() { return dropList; }
    public void setDropList(String dropList) { this.dropList = dropList; }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new LootContainerComponent(new ArrayList<>(this.items), this.dropList);
    }
}
