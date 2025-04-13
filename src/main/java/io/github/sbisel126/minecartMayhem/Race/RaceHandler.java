package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
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
    public Boolean RaceInProgress = false;
    int RealPlayerCount = 0;

    int CompletedRacerCount = 0;

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

    public void StartRace() {
        // toggle RaceInProgress to prevent users from joining the race
        RaceInProgress = true;

        //display start of race graphic
        for (RacePlayer racePlayer : players) {
            if (racePlayer != null) {
                RealPlayerCount++;
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
        this.raceTimer = new BukkitRunnable() {
            @Override
            public void run() {
                elapsedTime++;

                // We can check if everyone has finished the race
                if (CompletedRacerCount == RealPlayerCount) {
                    // trigger end of race function call
                    EndRace();
                    return;
                }

                // in addition, here is where we can tap in and add an x min race kill-switch
                if (elapsedTime > 300) {
                    // kick everyone who is still racing out
                    // they didn't finish, they don't get a score.

                    for (RacePlayer p : players) {
                        if (p != null && p.isRacing) {
                            p.player.sendMessage("You took too long! You have been disqualified.");
                            p.setRacing(false);

                            // send them to the hub and disable their boat
                            p.minecart.stopBoatControl();
                            p.player.teleport(new Location(p.player.getWorld(), -24, -60, 574));
                        }
                    }

                    // end of race logic function call goes here
                    EndRace();
                }
            }
        };
        raceTimer.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
    }

    private void EndRace() {
        stopRaceTimer();
        // generate the leaderboard
        players.getFirst().player.sendMessage("done");
        // reset the race state to default
        // allow player joins
        this.RaceInProgress = false;
        // clear out the players
        this.players.clear();
        // reset the player count
        this.RealPlayerCount = 0;
        // reset the completed racer count
        this.CompletedRacerCount = 0;
    }

    // getCurrentTime returns the elapsed time in seconds
    public int getCurrentRaceTime() {
        return elapsedTime;
    }

    public void stopRaceTimer() {
        if (this.raceTimer != null) {
            this.raceTimer.cancel();
        }
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
            Checkpoints.add(new Checkpoint(plugin, 4, 272, -59, -161, 274, -59, -168));
            Checkpoints.add(new Checkpoint(plugin, 5, 312, -59, -168, 312, -59, -161));
            Checkpoints.add(new Checkpoint(plugin, 6, 487, -59, -94, 272, -59, -94));
            Checkpoints.add(new Checkpoint(plugin, 7, 470, -59, 41, 470, -59, 57));
            Checkpoints.add(new Checkpoint(plugin, 8, 387, -59, 57, 387, -59, 41));
            Checkpoints.add(new Checkpoint(plugin, 9, 282, -63, 57, 282, -63, 42));
            Checkpoints.add(new Checkpoint(plugin, 10, 264, -63, 25, 273, -63, 25));
        }
    }
}