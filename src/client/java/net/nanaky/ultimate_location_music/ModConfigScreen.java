package net.nanaky.ultimate_location_music;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ModConfigScreen {

    public static Screen build(Screen parent) {
        ModConfig cfg = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.ultimate_location_music.title"))
                .setDoesConfirmSave(false)
                .setSavingRunnable(() -> {
                    ModConfig.save();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getSoundManager() != null) {
                        LocationMusicPlayer.stopAll(mc.getSoundManager());
                    }
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ═════════════════════════════════════════════════════════════════════
        // TAB 1 — General
        // ═════════════════════════════════════════════════════════════════════
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("config.ultimate_location_music.category.general"));

        // ── Master toggle ─────────────────────────────────────────────────────
        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("config.ultimate_location_music.enabled"), cfg.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.ultimate_location_music.enabled.tooltip"))
                .setSaveConsumer(v -> cfg.enabled = v)
                .build());

        // ── Override vanilla music ────────────────────────────────────────────
        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("config.ultimate_location_music.override_music"), cfg.overrideMusic)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.ultimate_location_music.override_music.tooltip"))
                .setSaveConsumer(v -> cfg.overrideMusic = v)
                .build());

        // ── Underwater pitch ──────────────────────────────────────────────────
        general.addEntry(eb.startIntSlider(
                        Component.translatable("config.ultimate_location_music.fluid_pitch"),
                        (int)(cfg.underwaterPitch * 100), 50, 200)
                .setDefaultValue(75)
                .setTooltip(Component.translatable("config.ultimate_location_music.fluid_pitch.tooltip"))
                .setSaveConsumer(v -> cfg.underwaterPitch = v / 100f)
                .build());

        // ═════════════════════════════════════════════════════════════════════
        // TAB 2 — Fade & Ghost
        // ═════════════════════════════════════════════════════════════════════
        ConfigCategory fade = builder.getOrCreateCategory(
                Component.translatable("config.ultimate_location_music.category.fade"));

        // ── Use fade ──────────────────────────────────────────────────────────
        fade.addEntry(eb.startBooleanToggle(
                        Component.translatable("config.ultimate_location_music.use_fade"), cfg.useFade)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.ultimate_location_music.use_fade.tooltip"))
                .setSaveConsumer(v -> cfg.useFade = v)
                .build());

        // ── Fade in ───────────────────────────────────────────────────────────
        fade.addEntry(eb.startIntField(
                        Component.translatable("config.ultimate_location_music.fade_in_ticks"), cfg.reviveFadeInTicks)
                .setDefaultValue(40)
                .setMin(0)
                .setTooltip(Component.translatable("config.ultimate_location_music.fade_in_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.reviveFadeInTicks = Math.max(1, v))
                .build());

        // ── Fade out ──────────────────────────────────────────────────────────
        fade.addEntry(eb.startIntField(
                        Component.translatable("config.ultimate_location_music.fade_out_ticks"), cfg.fadeOutTicks)
                .setDefaultValue(60)
                .setMin(0)
                .setTooltip(Component.translatable("config.ultimate_location_music.fade_out_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.fadeOutTicks = Math.max(1, v))
                .build());

        // ── Ghost duration ────────────────────────────────────────────────────
        fade.addEntry(eb.startIntField(
                        Component.translatable("config.ultimate_location_music.ghost_ticks"), cfg.ghostDurationTicks)
                .setDefaultValue(100)
                .setMin(0)
                .setTooltip(Component.translatable("config.ultimate_location_music.ghost_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.ghostDurationTicks = Math.max(1, v))
                .build());

        // ═════════════════════════════════════════════════════════════════════
        // TAB 3 — Locations
        // ═════════════════════════════════════════════════════════════════════
        ConfigCategory locations = builder.getOrCreateCategory(
                Component.translatable("config.ultimate_location_music.category.locations"));

        // ── Add location ──────────────────────────────────────────────────────
        if (cfg.canAddLocation()) {
            locations.addEntry(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.add_location"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("config.ultimate_location_music.add_location.tooltip"))
                    .setSaveConsumer(v -> {
                        if (v) {
                            cfg.addLocation();
                            ModConfig.save();
                            Minecraft.getInstance().gui.setScreen(build(parent));
                        }
                    })
                    .build());
        } else {
            locations.addEntry(eb.startTextDescription(
                    Component.translatable("config.ultimate_location_music.max_reached")).build());
        }

        // ── Per-location collapsible sub-categories ───────────────────────────
        List<LocationEntry> locationList = cfg.locations;
        for (int i = 0; i < locationList.size(); i++) {
            final int     idx   = i;
            LocationEntry entry = locationList.get(i);

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("\u266B " + (i + 1) + ". " + entry.name));

            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.enabled"), entry.enabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> entry.enabled = v)
                    .build());

            sub.add(eb.startStrField(
                            Component.translatable("config.ultimate_location_music.location.name"), entry.name)
                    .setDefaultValue("New Location")
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.name.tooltip"))
                    .setSaveConsumer(v -> entry.name = v.isEmpty() ? "Location " + (idx + 1) : v)
                    .build());

            sub.add(eb.startIntSlider(
                            Component.translatable("config.ultimate_location_music.location.song_index"),
                            entry.songIndex, 1, ModConfig.MAX_LOCATIONS)
                    .setDefaultValue(1)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.song_index.tooltip"))
                    .setSaveConsumer(v -> entry.songIndex = v)
                    .build());

            // ── Volume ────────────────────────────────────────────────────────
            sub.add(eb.startIntSlider(
                            Component.translatable("config.ultimate_location_music.location.volume"),
                            (int)(entry.volume * 100), 0, 100)
                    .setDefaultValue(50)
                    .setSaveConsumer(v -> entry.volume = v / 100f)
                    .build());

            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.loop"), entry.loop)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.loop.tooltip"))
                    .setSaveConsumer(v -> entry.loop = v)
                    .build());

            sub.add(eb.startStrField(
                            Component.translatable("config.ultimate_location_music.location.dimension"), entry.dimension)
                    .setDefaultValue("minecraft:overworld")
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.dimension.tooltip"))
                    .setSaveConsumer(v -> entry.dimension = v)
                    .build());

            // ── X ─────────────────────────────────────────────────────────────
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.x"), entry.x)
                    .setDefaultValue(0.0)
                    .setSaveConsumer(v -> entry.x = v)
                    .build());
            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.use_current_x"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.use_current_x.tooltip"))
                    .setSaveConsumer(v -> {
                        if (v) { LocalPlayer p = Minecraft.getInstance().player;
                            if (p != null) entry.x = p.position().x; }
                    })
                    .build());

            // ── Y ─────────────────────────────────────────────────────────────
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.y"), entry.y)
                    .setDefaultValue(64.0)
                    .setSaveConsumer(v -> entry.y = v)
                    .build());
            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.use_current_y"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.use_current_y.tooltip"))
                    .setSaveConsumer(v -> {
                        if (v) { LocalPlayer p = Minecraft.getInstance().player;
                            if (p != null) entry.y = p.position().y; }
                    })
                    .build());

            // ── Z ─────────────────────────────────────────────────────────────
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.z"), entry.z)
                    .setDefaultValue(0.0)
                    .setSaveConsumer(v -> entry.z = v)
                    .build());
            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.use_current_z"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.use_current_z.tooltip"))
                    .setSaveConsumer(v -> {
                        if (v) { LocalPlayer p = Minecraft.getInstance().player;
                            if (p != null) entry.z = p.position().z; }
                    })
                    .build());

            // ── Radii ─────────────────────────────────────────────────────────
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.radius_x"), entry.radiusX)
                    .setDefaultValue(32.0)
                    .setMin(1.0)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.radius_x.tooltip"))
                    .setSaveConsumer(v -> entry.radiusX = Math.max(1.0, v))
                    .build());
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.radius_y"), entry.radiusY)
                    .setDefaultValue(16.0)
                    .setMin(1.0)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.radius_y.tooltip"))
                    .setSaveConsumer(v -> entry.radiusY = Math.max(1.0, v))
                    .build());
            sub.add(eb.startDoubleField(
                            Component.translatable("config.ultimate_location_music.location.radius_z"), entry.radiusZ)
                    .setDefaultValue(32.0)
                    .setMin(1.0)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.radius_z.tooltip"))
                    .setSaveConsumer(v -> entry.radiusZ = Math.max(1.0, v))
                    .build());

            // ── Remove ────────────────────────────────────────────────────────
            sub.add(eb.startBooleanToggle(
                            Component.translatable("config.ultimate_location_music.location.remove"), false)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("config.ultimate_location_music.location.remove.tooltip"))
                    .setSaveConsumer(v -> {
                        if (v) {
                            cfg.removeLocation(idx);
                            ModConfig.save();
                            Minecraft.getInstance().gui.setScreen(build(parent));
                        }
                    })
                    .build());

            locations.addEntry(sub.build());
        }

        return builder.build();
    }
}