package net.nanaky.ultimate_location_music;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.util.ArrayList;
import java.util.List;

public class ModSounds {

    public static final List<SoundEvent> LOCATION_SOUNDS = new ArrayList<>(Constants.MAX_LOCATIONS);

    public static void register() {
        for (int i = 1; i <= Constants.MAX_LOCATIONS; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(
                    UltimateLocationMusicMod.MOD_ID, "location_" + i);
            SoundEvent event = SoundEvent.createVariableRangeEvent(id);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
            LOCATION_SOUNDS.add(event);
        }
        UltimateLocationMusicMod.LOGGER.info("Registered {} location sound events.", Constants.MAX_LOCATIONS);
    }

    public static SoundEvent forIndex(int songIndex) {
        if (songIndex < 1 || songIndex > LOCATION_SOUNDS.size()) return null;
        return LOCATION_SOUNDS.get(songIndex - 1);
    }
}
