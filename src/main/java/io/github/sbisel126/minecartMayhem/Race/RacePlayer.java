package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartHandler;
import org.bukkit.entity.Player;

// represents Player information within a race
public class RacePlayer {
    int CartColor;
    Player player;
    String Username;
    MinecartHandler minecart;
    public Integer StartX;
    public Integer StartY;
    public Integer StartZ;

    public RacePlayer(Player player, DatabaseHandler db) {
        // get player's preferred cart color
        this.CartColor = db.GetPlayerBoatColor(player);
        this.Username = player.getName();
        this.player = player;
    }
    public void setMinecart(MinecartHandler minecart) {
        this.minecart = minecart;
    }

    public String GetUsername() {
        return Username;
    }

    public void SetStartPos(Integer x, Integer y, Integer z) {
        this.StartX = x;
        this.StartY = y;
        this.StartZ = z;
    }
    public Player GetPlayer() {
        return player;
    }
}
