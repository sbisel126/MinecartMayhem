package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// represents a Race and it's relevant information
public class RaceHandler {

    List<RacePlayer> players = new ArrayList<>();
    DatabaseHandler db;
    MinecartMayhem plugin;
    String MapName;

    // for the race timer
    private int elapsedTime = 0;
    private BukkitRunnable raceTimer;

    // these are the list of Checkpoints for each map
    // we just load these manually later
    public List<Checkpoint> Checkpoints = new ArrayList<>();

    public RaceHandler(MinecartMayhem plugin, String MapName) {
        this.db = plugin.db;
        this.plugin = plugin;
        this.MapName = MapName;
        SetCheckpoints();
    }

    // When calls, adds the player to the race.
    // also creates an instance of RacePlayer for the player
    public void AddPlayers(List<RacePlayer> players) {
        this.players = players;
        StartRace();
    }

    // removes player from the Race Object.
    public void RemovePlayer(Player player) {
        this.players.removeIf(racePlayer -> racePlayer.GetUsername().equals(player.getName()));
    }

    public void StartRace() {
        //display start of race graphic
        for (RacePlayer racePlayer : players) {
            if (racePlayer != null) {
                displayRaceStartGraphic(racePlayer);
            }
        }
        //unfreeze boats and set them to racing
        for (RacePlayer racePlayer : players) {
            if (racePlayer != null) {
                racePlayer.minecart.setFrozenBoat(false);
                // this activates checkpoint checking, so it's important.
                racePlayer.setRacing(true);
            }
        }
        //start timer
        startRaceTimer();
    }

    // starts the auto-incrementing timer
    private void startRaceTimer() {
        elapsedTime = 0; // Reset the timer
        raceTimer = new BukkitRunnable() {
            @Override
            public void run() {
                elapsedTime++;
            }
        };
        raceTimer.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
    }

    // getCurrentTime returns the elapsed time in seconds
    public int getCurrentRaceTime() {
        return elapsedTime;
    }

    public int stopRaceTimer() {
        if (raceTimer != null) {
            raceTimer.cancel();
            return elapsedTime;
        }
        return 0;
    }

    private void displayRaceStartGraphic(RacePlayer p) {
        Player player = p.player;
        // Play the first sound immediately
        player.showTitle(Title.title(Component.text("3", NamedTextColor.RED),Component.text("")));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 0.67F);

        // After 1 second (20 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("2", NamedTextColor.RED),Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 0.67F);
        }, 20L);

        // After 2 seconds (40 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("1", NamedTextColor.RED),Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 0.67F);
        }, 40L);

        // After 3 seconds (60 ticks) – start the race
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("Go!", NamedTextColor.GREEN),Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1.33F);
            player.sendMessage("Race started!");
        }, 60L);
    }

    public void SetCheckpoints() {
        // each map has a different set of checkpoints, which we have to manually define
        // kinda yucky!
        if (Objects.equals(this.MapName, "grass")) {
            // finish line
            Checkpoints.add(new Checkpoint(plugin, 0, 261, -59, -53, 272, -59, -53));
            Checkpoints.add(new Checkpoint(plugin, 1, 261, -59, -90, 272, -59, -90));
            Checkpoints.add(new Checkpoint(plugin, 3, 261, -59, -31, 272, -59, -31));
        }
    }
}