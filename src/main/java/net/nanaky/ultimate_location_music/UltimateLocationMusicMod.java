package net.nanaky.ultimate_location_music;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimateLocationMusicMod implements ModInitializer {
    public static final String MOD_ID = "ultimate_location_music";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModSounds.register();
        LOGGER.info("Ultimate Location Music initialized.");
    }
}