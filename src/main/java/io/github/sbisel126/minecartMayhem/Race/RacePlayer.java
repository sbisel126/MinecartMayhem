package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import org.bukkit.entity.Player;

// represents Player information within a race
public class RacePlayer {
    int CartColor;
    Player player;
    String Username;
    public RacePlayer(Player player, DatabaseHandler db) {
        // get player's preferred cart color
        this.CartColor = db.GetPlayerBoatColor(player);
        this.Username = player.getName();
        this.player = player;
    }

    public String GetUsername() {
        return Username;
    }
}
