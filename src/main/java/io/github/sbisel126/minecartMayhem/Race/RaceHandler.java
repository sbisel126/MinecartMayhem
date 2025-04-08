package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

// represents a Race and it's relevant information
public class RaceHandler {

    List<RacePlayer> players = new ArrayList<>();
    DatabaseHandler db;
    MinecartMayhem plugin;

    public RaceHandler(MinecartMayhem plugin) {
        this.db = plugin.db;
        this.plugin = plugin;
    }

    // When calls, adds the player to the race.
    // also creates an instance of RacePlayer for the player
    public void AddPlayer(Player player) {
        var NewRacePlayer = new RacePlayer(player, this.db);
        this.players.add(NewRacePlayer);
        // for testing purposes let's just shove them in their respective cart
        MinecartHandler MinecartHandler = new MinecartHandler(plugin);
        MinecartHandler.PutPlayerInCart(player, NewRacePlayer.CartColor);
    }

    // removes player from the Race Object.
    public void RemovePlayer(Player player) {
        this.players.removeIf(racePlayer -> racePlayer.GetUsername().equals(player.getName()));
    }
}