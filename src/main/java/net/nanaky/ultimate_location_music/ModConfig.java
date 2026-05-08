package net.nanaky.ultimate_location_music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) instance = new ModConfig();
        return instance;
    }

    // ── Master toggle ─────────────────────────────────────────────────────────
    public boolean enabled = true;

    // ── Vanilla music suppression ─────────────────────────────────────────────
    public boolean overrideMusic = false;

    // ── Underwater pitch ──────────────────────────────────────────────────────
    public float underwaterPitch = 0.75f;

    // ── Fade & ghost ──────────────────────────────────────────────────────────
    public boolean useFade            = true;
    public int     reviveFadeInTicks  = 40;
    public int     fadeOutTicks       = 60;
    public int     ghostDurationTicks = 100;

    // ── Locations ─────────────────────────────────────────────────────────────
    public List<LocationEntry> locations = new ArrayList<>();
    public static final int MAX_LOCATIONS = Constants.MAX_LOCATIONS;

    // ── Persistence ───────────────────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("ultimate_location_music.json");

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            instance = new ModConfig();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            instance = GSON.fromJson(reader, ModConfig.class);
            if (instance == null) instance = new ModConfig();
            if (instance.locations == null) instance.locations = new ArrayList<>();
        } catch (IOException e) {
            UltimateLocationMusicMod.LOGGER.error("Failed to load config: {}", e.getMessage());
            instance = new ModConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            UltimateLocationMusicMod.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public boolean canAddLocation() {
        return locations.size() < MAX_LOCATIONS;
    }

    public void addLocation() {
        if (!canAddLocation()) return;
        LocationEntry e = new LocationEntry();
        e.name      = "Location " + (locations.size() + 1);
        e.songIndex = locations.size() + 1;
        locations.add(e);
    }

    public void removeLocation(int index) {
        if (index >= 0 && index < locations.size()) locations.remove(index);
    }
}