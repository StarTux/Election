package com.cavetale.election.struct;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Serializable Location.
 */
@Value
public final class Position {
    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final float pitch;
    public final float yaw;

    public Position(final Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.pitch = location.getPitch();
        this.yaw = location.getYaw();
    }

    public Location toLocation() {
        World w = Bukkit.getServer().getWorld(world);
        if (w == null) {
            throw new IllegalStateException("World not found: " + world);
        }
        return new Location(w, x, y, z, yaw, pitch);
    }
}
