package net.nanaky.ultimate_location_music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class LocationMusicPlayer {

    private static final List<LocationSoundInstance> instances  = new ArrayList<>();
    private static final List<Boolean>                exhausted = new ArrayList<>();
    private static int activePriority = -1;

    private static int     fluidPitchDelayTick = 0;
    private static boolean wasInFluid          = false;
    private static final int FLUID_PITCH_DELAY = 5;

    private static boolean battleMusicClassMissing  = false;

    private static boolean isBattleMusicAudible() {
        if (battleMusicClassMissing) return false;
        try {
            Class<?> clazz = Class.forName("net.nanaky.ultimate_battle_music.music.MusicManager");
            var field = clazz.getField("audible");
            return field.getBoolean(null);
        } catch (ClassNotFoundException e) {
            battleMusicClassMissing = true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void tick() {
    Minecraft   mc     = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    ModConfig   cfg    = ModConfig.get();

    if (player == null || !cfg.enabled) {
        stopAll(mc.getSoundManager());
        return;
    }

    syncInstanceList(cfg);

    boolean inFluid = player.isUnderWater() || player.isEyeInFluid(FluidTags.LAVA);
    if (inFluid != wasInFluid) {
        fluidPitchDelayTick = 0;
        wasInFluid = inFluid;
    } else if (fluidPitchDelayTick < FLUID_PITCH_DELAY) {
        fluidPitchDelayTick++;
    }
    float targetPitch = (fluidPitchDelayTick >= FLUID_PITCH_DELAY)
            ? (inFluid ? cfg.underwaterPitch : 1.0f)
            : (wasInFluid ? cfg.underwaterPitch : 1.0f);

    for (LocationSoundInstance inst : instances) {
        if (inst != null && !inst.isStopped()) {
            inst.setTargetPitch(targetPitch);
            if (inst.isFinishedNaturally()) inst.tick();
        }
    }

    Vec3    pos       = player.position();
    String  playerDim = player.level().dimension().identifier().toString();
    boolean useFade   = cfg.useFade;

    int zoneIndex = -1;
    for (int i = 0; i < cfg.locations.size(); i++) {
        LocationEntry e = cfg.locations.get(i);
        if (!e.enabled) continue;
        if (!e.dimension.isEmpty() && !e.dimension.equals(playerDim)) continue;
        if (Math.abs(pos.x - e.x) <= e.radiusX
         && Math.abs(pos.y - e.y) <= e.radiusY
         && Math.abs(pos.z - e.z) <= e.radiusZ) {
            zoneIndex = i;
            break;
        }
    }

    for (int i = 0; i < exhausted.size(); i++) {
        if (i != zoneIndex) exhausted.set(i, false);
    }

    boolean priorityStolen = isBattleMusicAudible() || isAnyMusicPlaying(mc);

    int newPriority = zoneIndex;
    if (newPriority >= 0 && !cfg.overrideMusic && priorityStolen) {
        newPriority = -1;
    }

    for (int i = 0; i < cfg.locations.size(); i++) {
        LocationSoundInstance inst = instances.get(i);

        if (inst != null && inst.isStopped()) {
            instances.set(i, null);
            inst = null;
        }

        LocationEntry entry       = cfg.locations.get(i);
        boolean       isExhausted = exhausted.get(i);

        if (i == newPriority) {
            mc.getMusicManager().stopPlaying();

            if (!isExhausted) {
                if (inst != null) {
                    if (inst.isRevivable()) {
                        inst.revive(useFade, cfg.reviveFadeInTicks);

                    } else if (inst.isFinishedNaturally()) {

                    } else if (!mc.getSoundManager().isActive(inst)) {
                        LocationSoundInstance.Phase phase = inst.getPhase();
                        if (phase == LocationSoundInstance.Phase.SUSTAIN
                         || phase == LocationSoundInstance.Phase.FADE_IN) {
                            if (entry.loop) {
                                instances.set(i, null);
                                inst = null;
                            } else {
                                inst.markFinishedNaturally();
                                inst.beginFadeOut(false);
                                exhausted.set(i, true);
                            }
                        }
                    }
                }

                if (instances.get(i) == null) {
                    SoundEvent event = ModSounds.forIndex(entry.songIndex);
                    if (event != null) {
                        LocationSoundInstance fresh = new LocationSoundInstance(
                                event, entry.volume,
                                useFade ? cfg.fadeOutTicks : 0,
                                cfg.ghostDurationTicks
                        );
                        fresh.setTargetPitch(targetPitch);
                        instances.set(i, fresh);
                        mc.getSoundManager().play(fresh);
                    }
                }
            }

        } else {
            if (inst != null) {
                LocationSoundInstance.Phase phase = inst.getPhase();
                if (phase == LocationSoundInstance.Phase.FADE_IN
                 || phase == LocationSoundInstance.Phase.SUSTAIN) {
                    if (priorityStolen && i == activePriority) {
                        inst.beginExitFadeOut(20);
                    } else {
                        inst.beginExitFadeOut(useFade);
                    }
                }
            }
        }
    }

    activePriority = newPriority;
}

    private static void syncInstanceList(ModConfig cfg) {
        while (instances.size() < cfg.locations.size()) {
            instances.add(null);
            exhausted.add(false);
        }
        while (instances.size() > cfg.locations.size()) {
            LocationSoundInstance inst = instances.remove(instances.size() - 1);
            exhausted.remove(exhausted.size() - 1);
            if (inst != null && !inst.isStopped()) inst.beginFadeOut(false);
        }
    }

    public static void stopAll(SoundManager ignored) {
        for (LocationSoundInstance inst : instances) {
            if (inst != null && !inst.isStopped()) inst.killImmediately();
        }
        instances.clear();
        exhausted.clear();
        activePriority = -1;
        fluidPitchDelayTick = 0;
        wasInFluid          = false;
    }

    private static boolean isAnyMusicPlaying(Minecraft mc) {
        try {
            var field = mc.getMusicManager().getClass().getDeclaredField("currentMusic");
            field.setAccessible(true);
            Object current = field.get(mc.getMusicManager());
            if (current != null && mc.getSoundManager().isActive(
                    (net.minecraft.client.resources.sounds.SoundInstance) current)) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            var field = mc.getSoundManager().getClass().getDeclaredField("soundEngine");
            field.setAccessible(true);
            Object engine = field.get(mc.getSoundManager());
            var instancesField = engine.getClass().getDeclaredField("instanceToChannel");
            instancesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.client.resources.sounds.SoundInstance, ?> map =
                    (java.util.Map<net.minecraft.client.resources.sounds.SoundInstance, ?>) instancesField.get(engine);
            for (net.minecraft.client.resources.sounds.SoundInstance s : map.keySet()) {
                if (s.getSource() == SoundSource.MUSIC || s.getSource() == SoundSource.RECORDS) {
                    if (!(s instanceof LocationSoundInstance) && s.getVolume() > 0.01f
                            && s.getIdentifier().getPath().startsWith("music_disc.")) return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }
}