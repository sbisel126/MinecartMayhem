package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import org.bukkit.entity.Player;

// represents Player information within a race
public class RacePlayer {
    int CurrentPosition;
    // LastCheckpoint;
    int CurrentLap = 0;
    int CartColor = 0;
    String Username;
    public RacePlayer(Player player, DatabaseHandler db) {
        // get player's preferred cart color
        this.CartColor = db.GetPlayerBoatColor(player);
        this.Username = player.getName();
    }

    public String GetUsername() {
        return Username;
    }
}
