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

    private static final List<LocationSoundInstance> instances = new ArrayList<>();
    private static int activePriority = -1;

    private static int     fluidPitchDelayTick = 0;
    private static boolean wasInFluid          = false;
    private static final int FLUID_PITCH_DELAY = 25;

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
            if (inst != null && !inst.isStopped()) inst.setTargetPitch(targetPitch);
        }

        Vec3   pos       = player.position();
        String playerDim = player.level().dimension().identifier().toString();
        boolean useFade  = cfg.useFade;

        int newPriority = -1;
        for (int i = 0; i < cfg.locations.size(); i++) {
            LocationEntry e = cfg.locations.get(i);
            if (!e.enabled) continue;
            if (!e.dimension.isEmpty() && !e.dimension.equals(playerDim)) continue;
            if (Math.abs(pos.x - e.x) <= e.radiusX
             && Math.abs(pos.y - e.y) <= e.radiusY
             && Math.abs(pos.z - e.z) <= e.radiusZ) {
                newPriority = i;
                break;
            }
        }

        if (newPriority >= 0 && !cfg.overrideMusic && isAnyMusicPlaying(mc)) {
            newPriority = -1;
        }

        for (int i = 0; i < cfg.locations.size(); i++) {
            LocationSoundInstance inst = instances.get(i);

            if (inst != null && inst.isStopped()) {
                instances.set(i, null);
                inst = null;
            }

            if (i == newPriority) {
                mc.getMusicManager().stopPlaying();
                if (inst != null && !cfg.locations.get(i).loop && activePriority != i) {
                    LocationSoundInstance.Phase phase = inst.getPhase();
                    if (phase == LocationSoundInstance.Phase.GHOST
                     || phase == LocationSoundInstance.Phase.FADE_OUT) {
                        inst.beginFadeOut(false);
                        instances.set(i, null);
                        inst = null;
                    }
                }

                if (inst != null && inst.isRevivable()) {
                    inst.revive(useFade);
                } else if (inst != null && !inst.isLooping() && !mc.getSoundManager().isActive(inst)) {
                    inst.beginFadeOut(false);
                } else if (inst == null) {
                    SoundEvent event = ModSounds.forIndex(cfg.locations.get(i).songIndex);
                    if (event != null) {
                        LocationEntry e = cfg.locations.get(i);
                        LocationSoundInstance fresh = new LocationSoundInstance(
                                event, e.loop, e.volume,
                                useFade ? cfg.reviveFadeInTicks : 0,
                                useFade ? cfg.fadeOutTicks      : 0,
                                cfg.ghostDurationTicks
                        );
                        fresh.setTargetPitch(targetPitch);
                        instances.set(i, fresh);
                        mc.getSoundManager().play(fresh);
                    }
                }

            } else {
                if (inst != null) {
                    LocationSoundInstance.Phase phase = inst.getPhase();
                    if (phase == LocationSoundInstance.Phase.FADE_IN
                     || phase == LocationSoundInstance.Phase.SUSTAIN) {
                        inst.beginFadeOut(useFade);
                    }
                }
            }
        }

        activePriority = newPriority;
    }

    private static void syncInstanceList(ModConfig cfg) {
        while (instances.size() < cfg.locations.size()) instances.add(null);
        while (instances.size() > cfg.locations.size()) {
            LocationSoundInstance inst = instances.remove(instances.size() - 1);
            if (inst != null && !inst.isStopped()) inst.beginFadeOut(false);
        }
    }

    public static void stopAll(SoundManager ignored) {
        for (LocationSoundInstance inst : instances) {
            if (inst != null && !inst.isStopped()) inst.beginFadeOut(false);
        }
        instances.clear();
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
                            && !s.getIdentifier().getPath().startsWith("block.note_block.")) return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }
}