package net.nanaky.ultimate_location_music;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class LocationSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    public enum Phase { FADE_IN, SUSTAIN, FADE_OUT, GHOST, DEAD }

    private Phase phase;
    private int   fadeTick;
    private int   ghostTick;

    private final float targetVolume;
    private final int   reviveFadeInTicks;
    private final int   fadeOutTicks;
    private final int   ghostDurationTicks;

    private float targetPitch;
    private static final float PITCH_LERP_IN_SPEED  = 1.0f / 20f;
    private static final float PITCH_LERP_OUT_SPEED = 1.0f / 10f;

    public LocationSoundInstance(SoundEvent event, boolean loop, float volume,
                                 int reviveFadeInTicks, int fadeOutTicks, int ghostDurationTicks) {
        super(event.location(), SoundSource.MUSIC, RandomSource.create());
        this.targetVolume       = volume;
        this.looping            = loop;
        this.relative           = true;
        this.reviveFadeInTicks  = reviveFadeInTicks;
        this.fadeOutTicks       = fadeOutTicks;
        this.ghostDurationTicks = ghostDurationTicks;
        this.pitch              = 1.0f;
        this.targetPitch        = 1.0f;

        if (reviveFadeInTicks > 0) {
            this.volume   = 0f;
            this.phase    = Phase.FADE_IN;
            this.fadeTick = 0;
        } else {
            this.volume   = volume;
            this.phase    = Phase.SUSTAIN;
            this.fadeTick = 0;
        }
    }

    @Override
    public void tick() {
        float phaseVolume;
        switch (phase) {
            case FADE_IN -> {
                fadeTick++;
                phaseVolume = Math.min(1f, (float) fadeTick / Math.max(1, reviveFadeInTicks));
                if (fadeTick >= reviveFadeInTicks) {
                    phaseVolume = 1f;
                    phase = Phase.SUSTAIN;
                }
            }
            case FADE_OUT -> {
                fadeTick--;
                phaseVolume = Math.max(0f, (float) fadeTick / Math.max(1, fadeOutTicks));
                if (fadeTick <= 0) {
                    phaseVolume = 0f;
                    phase     = Phase.GHOST;
                    ghostTick = ghostDurationTicks;
                }
            }
            case GHOST -> {
                phaseVolume = 0f;
                if (--ghostTick <= 0) phase = Phase.DEAD;
            }
            default -> phaseVolume = 1f; // SUSTAIN
        }

        volume = targetVolume * phaseVolume;

        if (Math.abs(pitch - targetPitch) > 0.001f) {
            float speed = targetPitch < pitch ? PITCH_LERP_OUT_SPEED : PITCH_LERP_IN_SPEED;
            pitch += (targetPitch - pitch) * speed;
        } else {
            pitch = targetPitch;
        }
    }

    public void setTargetPitch(float newPitch) {
        targetPitch = newPitch;
    }

    public void beginFadeOut(boolean useFade) {
        if (phase == Phase.DEAD || phase == Phase.GHOST) return;
        if (!useFade || fadeOutTicks <= 0) {
            volume    = 0f;
            phase     = Phase.GHOST;
            ghostTick = ghostDurationTicks;
            return;
        }
        fadeTick = (phase == Phase.FADE_IN)
                ? (int)(((float) fadeTick / Math.max(1, reviveFadeInTicks)) * fadeOutTicks)
                : fadeOutTicks;
        phase = Phase.FADE_OUT;
    }

    public void revive(boolean useFadeIn) {
        if (phase == Phase.DEAD) return;
        looping = true;
        if (useFadeIn && reviveFadeInTicks > 0) {
            fadeTick = 0;
            phase    = Phase.FADE_IN;
        } else {
            volume = targetVolume;
            phase  = Phase.SUSTAIN;
        }
    }

    public boolean isRevivable() { return phase == Phase.GHOST || phase == Phase.FADE_OUT; }
    public Phase   getPhase()    { return phase; }

    @Override public boolean isStopped()      { return phase == Phase.DEAD; }
    @Override public boolean canStartSilent() { return true; }
    @Override public boolean canPlaySound()   { return true; }
}