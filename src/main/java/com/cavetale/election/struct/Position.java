package com.cavetale.election.struct;

import com.cavetale.core.connect.Connect;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Serializable Location.
 */
@Data
public final class Position {
    private String server = "cavetale";
    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final float pitch;
    public final float yaw;

    public Position(final Location location) {
        this.server = Connect.get().getServerName();
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

    public boolean isOnThisServer() {
        if (server == null) server = "cavetale";
        return Connect.get().getServerName().equals(server);
    }

    public String getServer() {
        return server != null
            ? server
            : "cavetale";
    }
}
