package org.mimstar.plugin.pages;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.mimstar.plugin.Loot4Everyone;
import org.mimstar.plugin.resources.LootChestConfig;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LootConfigPage extends InteractiveCustomUIPage<LootConfigPage.LootConfigData> {

    public LootConfigPage(PlayerRef playerRef){
        super(playerRef, CustomPageLifetime.CanDismiss,LootConfigData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/LootConfigPage.ui");
        List<DropdownEntryInfo> entries = new ArrayList<>();
        Player player = store.getComponent(ref, Player.getComponentType());
        LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("true"),"true"));
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("false"),"false"));

        uiCommandBuilder.set("#CanPlayerBreakLootChestsDropdown.Entries",entries);
        uiCommandBuilder.set("#CanPlayerBreakLootChestsDropdown.Value",String.valueOf(lootChestConfig.isCanPlayerBreakLootChests()));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,"#CanPlayerBreakLootChestsDropdown", EventData.of("Config","CanPlayerBreakLootChests").append("@DropdownValue","#CanPlayerBreakLootChestsDropdown.Value"), false);

        uiCommandBuilder.set("#IsLootRandomDropdown.Entries",entries);
        uiCommandBuilder.set("#IsLootRandomDropdown.Value",String.valueOf(lootChestConfig.isLootChestRandom()));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,"#IsLootRandomDropdown", EventData.of("Config","IsLootRandom").append("@DropdownValue","#IsLootRandomDropdown.Value"), false);

        uiCommandBuilder.set("#IsMessageAppearDropdown.Entries",entries);
        uiCommandBuilder.set("#IsMessageAppearDropdown.Value",String.valueOf(lootChestConfig.isMessageAppear()));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,"#IsMessageAppearDropdown", EventData.of("Config","IsMessageAppear").append("@DropdownValue","#IsMessageAppearDropdown.Value"), false);

        uiCommandBuilder.set("#IsParticlesAppearDropdown.Entries",entries);
        uiCommandBuilder.set("#IsParticlesAppearDropdown.Value",String.valueOf(lootChestConfig.isParticlesAppear()));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,"#IsParticlesAppearDropdown", EventData.of("Config","IsParticlesAppear").append("@DropdownValue","#IsParticlesAppearDropdown.Value"), false);

        uiCommandBuilder.set("#NextLootResetIntervalDaysNumberField.Value",lootChestConfig.getNextLootResetDaysInterval());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NextLootResetIntervalDaysNumberField",new EventData().append("@Days","#NextLootResetIntervalDaysNumberField.Value"),false);

        uiCommandBuilder.set("#NextLootResetIntervalHoursNumberField.Value",lootChestConfig.getNextLootResetHoursInterval());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NextLootResetIntervalHoursNumberField",new EventData().append("@Hours","#NextLootResetIntervalHoursNumberField.Value"),false);

        uiCommandBuilder.set("#NextLootResetIntervalMinutesNumberField.Value",lootChestConfig.getNextLootResetMinutesInterval());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NextLootResetIntervalMinutesNumberField",new EventData().append("@Minutes","#NextLootResetIntervalMinutesNumberField.Value"),false);

        uiCommandBuilder.set("#NextLootResetIntervalSecondsNumberField.Value",lootChestConfig.getNextLootResetSecondsInterval());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NextLootResetIntervalSecondsNumberField",new EventData().append("@Seconds","#NextLootResetIntervalSecondsNumberField.Value"),false);

        uiCommandBuilder.set("#ParticlesColorPicker.Value",lootChestConfig.getParticlesColor());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged,"#ParticlesColorPicker",new EventData().append("@Color","#ParticlesColorPicker.Value"),false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull LootConfigData data) {
        super.handleDataEvent(ref, store, data);
        Player player = store.getComponent(ref, Player.getComponentType());
        LootChestConfig lootChestConfig = player.getWorld().getChunkStore().getStore().getResource(Loot4Everyone.get().getLootChestConfigResourceType());

        if (data.config != null) {
            boolean value = Boolean.parseBoolean(data.dropdownValue);
            switch (data.config) {
                case "CanPlayerBreakLootChests" -> lootChestConfig.setCanPlayerBreakLootChests(value);
                case "IsLootRandom" -> lootChestConfig.setLootRandom(value);
                case "IsMessageAppear" -> lootChestConfig.setMessageAppear(value);
                case "IsParticlesAppear" -> lootChestConfig.setParticlesAppear(value);
            }
        }

        if (data.color != null) {
            lootChestConfig.setParticlesColor(data.color);
        }

        if (data.days != null) lootChestConfig.setNextLootResetDaysInterval(data.days);
        if (data.hours != null) lootChestConfig.setNextLootResetHoursInterval(data.hours);
        if (data.minutes != null) lootChestConfig.setNextLootResetMinutesInterval(data.minutes);
        if (data.seconds != null) lootChestConfig.setNextLootResetSecondsInterval(data.seconds);

        // Now calculate the total epoch based on the updated config values
        int d = lootChestConfig.getNextLootResetDaysInterval();
        int h = lootChestConfig.getNextLootResetHoursInterval();
        int m = lootChestConfig.getNextLootResetMinutesInterval();
        int s = lootChestConfig.getNextLootResetSecondsInterval();

        if (d == 0 && h == 0 && m == 0 && s == 0) {
            lootChestConfig.setNextLootReset(-1);
        } else {
            LocalDateTime nextResetDateTime = LocalDateTime.now()
                    .plusDays(d)
                    .plusHours(h)
                    .plusMinutes(m)
                    .plusSeconds(s);

            long nextResetEpoch = nextResetDateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            lootChestConfig.setNextLootReset(nextResetEpoch);
        }
    }

    public static class LootConfigData {
        static final String KEY_CONFIG = "Config";
        static final String KEY_DROPDOWN_VALUE_QUERY = "@DropdownValue";
        static final String KEY_COLOR = "@Color";
        static final String KEY_DAYS = "@Days";
        static final String KEY_HOURS = "@Hours";
        static final String KEY_MINUTES = "@Minutes";
        static final String KEY_SECONDS = "@Seconds";

        public static final BuilderCodec<LootConfigData> CODEC = BuilderCodec.<LootConfigData>builder(LootConfigData.class,LootConfigData::new)
                .addField(new KeyedCodec<>(KEY_CONFIG, Codec.STRING), (lootConfigData, s) -> lootConfigData.config = s, lootConfigData -> lootConfigData.config)
                .addField(new KeyedCodec<>(KEY_DROPDOWN_VALUE_QUERY, Codec.STRING), (lootConfigData, s) -> lootConfigData.dropdownValue = s, lootConfigData -> lootConfigData.dropdownValue)
                .addField(new KeyedCodec<>(KEY_COLOR, Codec.STRING), (lootConfigData, s) -> lootConfigData.color = s, lootConfigData -> lootConfigData.color)
                .addField(new KeyedCodec<>(KEY_DAYS, Codec.INTEGER), (lootConfigData, s) -> lootConfigData.days = s, lootConfigData -> lootConfigData.days)
                .addField(new KeyedCodec<>(KEY_HOURS, Codec.INTEGER), (lootConfigData, s) -> lootConfigData.hours = s, lootConfigData -> lootConfigData.hours)
                .addField(new KeyedCodec<>(KEY_MINUTES, Codec.INTEGER), (lootConfigData, s) -> lootConfigData.minutes = s, lootConfigData -> lootConfigData.minutes)
                .addField(new KeyedCodec<>(KEY_SECONDS, Codec.INTEGER), (lootConfigData, s) -> lootConfigData.seconds = s, lootConfigData -> lootConfigData.seconds)
                .build();

        private String config;
        private String dropdownValue;
        private String color;
        private Integer days;
        private Integer hours;
        private Integer minutes;
        private Integer seconds;
    }
}
