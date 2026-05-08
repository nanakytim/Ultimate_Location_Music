package net.nanaky.ultimate_location_music;

public class LocationEntry {
    public boolean enabled   = true;
    public String  name      = "New Location";
    public boolean loop      = true;
    public String  dimension = "minecraft:overworld";
    public double  x         = 0.0;
    public double  y         = 64.0;
    public double  z         = 0.0;
    public double  radiusX   = 32.0;
    public double  radiusY   = 16.0;
    public double  radiusZ   = 32.0;
    public int     songIndex = 1;
    public float   volume    = 1.0f;

    public LocationEntry() {}

    public LocationEntry copy() {
        LocationEntry e = new LocationEntry();
        e.name      = this.name;
        e.enabled   = this.enabled;
        e.loop      = this.loop;
        e.dimension = this.dimension;
        e.x = this.x; e.y = this.y; e.z = this.z;
        e.radiusX   = this.radiusX;
        e.radiusY   = this.radiusY;
        e.radiusZ   = this.radiusZ;
        e.songIndex = this.songIndex;
        e.volume    = this.volume;
        return e;
    }

    @Override
    public String toString() {
        return String.format(
            "LocationEntry{name='%s', enabled=%b, loop=%b, dim='%s', pos=(%.1f,%.1f,%.1f), radius=(%.1f,%.1f,%.1f), song=%d, volume=%.2f}",
            name, enabled, loop, dimension, x, y, z, radiusX, radiusY, radiusZ, songIndex, volume);
    }
}