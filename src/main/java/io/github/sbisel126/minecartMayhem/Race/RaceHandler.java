package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

// represents a Race and it's relevant information
public class RaceHandler {
    List<RacePlayer> players = new ArrayList<RacePlayer>();
    DatabaseHandler db;
    public RaceHandler(DatabaseHandler db) {
        this.db = db;
    }

    // When calls, adds the player to the race.
    // also creates an instance of RacePlayer for the player
    public void AddPlayer(Player player) {
        var NewRacePlayer = new RacePlayer(player, this.db);
        this.players.add(NewRacePlayer);
    }

    // removes player from the Race Object.
    public void RemovePlayer(Player player) {
        this.players.removeIf(racePlayer -> racePlayer.GetUsername().equals(player.getName()));
    }
}