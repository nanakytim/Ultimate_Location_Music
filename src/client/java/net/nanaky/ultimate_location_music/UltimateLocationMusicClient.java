package net.nanaky.ultimate_location_music;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class UltimateLocationMusicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> LocationMusicPlayer.tick());
        UltimateLocationMusicMod.LOGGER.info("Ultimate Location Music client ready.");
    }
}
