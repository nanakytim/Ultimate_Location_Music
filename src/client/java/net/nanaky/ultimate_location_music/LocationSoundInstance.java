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
    private       int   reviveFadeInTicks;
    private final int   fadeOutTicks;
    private final int   ghostDurationTicks;

    private boolean exitGhost        = false;
    private boolean finishedNaturally = false;

    private float targetPitch;
    private static final float PITCH_LERP_IN_SPEED  = 1.0f / 20f;
    private static final float PITCH_LERP_OUT_SPEED = 1.0f / 10f;

    public LocationSoundInstance(SoundEvent event, float volume,
                                 int fadeOutTicks, int ghostDurationTicks) {
        super(event.location(), SoundSource.MUSIC, RandomSource.create());
        this.targetVolume       = volume;
        this.looping            = false;
        this.relative           = true;
        this.reviveFadeInTicks  = 0;
        this.fadeOutTicks       = fadeOutTicks;
        this.ghostDurationTicks = ghostDurationTicks;
        this.pitch              = 1.0f;
        this.targetPitch        = 1.0f;
        this.volume             = volume;
        this.phase              = Phase.SUSTAIN;
        this.fadeTick           = 0;
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
            default -> phaseVolume = 1f;
        }

        volume = targetVolume * phaseVolume;

        if (Math.abs(pitch - targetPitch) > 0.001f) {
            float speed = targetPitch < pitch ? PITCH_LERP_OUT_SPEED : PITCH_LERP_IN_SPEED;
            pitch += (targetPitch - pitch) * speed;
        } else {
            pitch = targetPitch;
        }
    }

    public void setTargetPitch(float p) { targetPitch = p; }

    public void beginExitFadeOut(boolean useFade) {
        exitGhost = true;
        beginFadeOut(useFade);
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

    public void revive(boolean useFadeIn, int fadeInTicks) {
        if (phase == Phase.DEAD) return;
        exitGhost         = false;
        finishedNaturally = false;
        if (useFadeIn && fadeInTicks > 0) {
            this.reviveFadeInTicks = fadeInTicks;
            fadeTick = 0;
            phase    = Phase.FADE_IN;
        } else {
            volume = targetVolume;
            phase  = Phase.SUSTAIN;
        }
    }

    public void markFinishedNaturally() {
        finishedNaturally = true;
    }

    public void killImmediately() {
        volume = 0f;
        phase  = Phase.DEAD;
    }

    public boolean isFinishedNaturally() { return finishedNaturally; }
    public boolean isRevivable()         { return (phase == Phase.GHOST || phase == Phase.FADE_OUT) && exitGhost; }
    public Phase   getPhase()            { return phase; }

    @Override public boolean isStopped()      { return phase == Phase.DEAD; }
    @Override public boolean canStartSilent() { return true; }
    @Override public boolean canPlaySound()   { return true; }
}