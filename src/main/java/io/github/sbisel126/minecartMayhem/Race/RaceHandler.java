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

    public Boolean RaceInProgress = false;
    // these are the list of Checkpoints for each map
    // we just load these manually later
    public List<Checkpoint> Checkpoints = new ArrayList<>();
    List<RacePlayer> players = new ArrayList<>();
    DatabaseHandler db;
    MinecartMayhem plugin;
    String MapName;
    int RealPlayerCount = 0;
    int CompletedRacerCount = 0;
    // for the race timer
    private int elapsedTime = 0;
    private BukkitRunnable raceTimer;

    public RaceHandler(MinecartMayhem plugin, String MapName) {
        this.db = plugin.db;
        this.plugin = plugin;
        this.MapName = MapName;
        SetCheckpoints();
    }

    // When calls, adds the player to the race.
    // also creates an instance of RacePlayer for the player
    public void AddPlayers(List<RacePlayer> players) {
        this.players = new ArrayList<>(players);
        StartRace();
    }

    public void RemovePlayer(Player player) {
        // nuke the relevant RacePlayer object in the players list
        for (int i = 0; i < players.size(); i++) {
            RacePlayer racePlayer = players.get(i);
            if (racePlayer != null && racePlayer.GetUsername().equalsIgnoreCase(player.getName())) {
                players.remove(i);
                break;
            }
        }
    }

    public void StartRace() {
        RaceInProgress = true;
        for (RacePlayer racePlayer : players) {
            if (racePlayer != null) {
                RealPlayerCount++;
                displayRaceStartGraphic(racePlayer);
            }
        }

        // Delay to match the countdown (3 seconds = 60 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (RacePlayer racePlayer : players) {
                if (racePlayer != null) {
                    racePlayer.minecart.setFrozenBoat(false);
                    racePlayer.setRacing(true);
                    racePlayer.player.sendMessage("Race started!");
                }
            }
            startRaceTimer();
        }, 60L); // Delay matches "GO!" graphic timing
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
                if (elapsedTime > 800) {
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

    public int scoreRace() {
        int totalScore = 100;

        // catch edge case where elapsedTime can be 0.  Avoids a divide-by-zero.
        if (this.elapsedTime <= 0) {
            totalScore = 0;
        } else {
            totalScore *= (int) Math.floor(((1024.00 / this.elapsedTime) * 256.00));
        }
        return totalScore;
    }

    public boolean isHighScore(Player player, Integer score) {
        ArrayList<Integer> scores = db.GetMapTopScores(MapName);
        if (!scores.isEmpty()) {
            if (score > db.GetMapTopScores(MapName).getLast()) {
                db.InsertHighScore(player, MapName, score);
                return true;
            } else {
                return false;
            }
        } else {
            db.InsertHighScore(player, MapName, score);
            return true;
        }
    }

    private void displayRaceStartGraphic(RacePlayer p) {
        Player player = p.player;
        // Play the first sound immediately
        player.showTitle(Title.title(Component.text("3", NamedTextColor.RED), Component.text("")));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1F, 0.67F);

        // After 1 second (20 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("2", NamedTextColor.RED), Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 0.67F);
        }, 20L);

        // After 2 seconds (40 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("1", NamedTextColor.RED), Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 0.67F);
        }, 40L);

        // After 3 seconds (60 ticks) – start the race
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.showTitle(Title.title(Component.text("Go!", NamedTextColor.GREEN), Component.text("")));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1.33F);
            player.sendMessage("Race started!");

            //unfreeze boats and set them to racing
            for (RacePlayer racePlayer : players) {
                if (racePlayer != null) {
                    racePlayer.minecart.setFrozenBoat(false);
                    // this activates checkpoint checking, so it's important.
                    racePlayer.setRacing(true);
                }
            }
            //start timer
        }, 60L);
    }

    public void SetCheckpoints() {
        // each map has a different set of checkpoints, which we have to manually define
        // kinda yucky!
        if (Objects.equals(this.MapName, "grass")) {
            // finish line is always id 0
            Checkpoints.add(new Checkpoint(plugin, 0, 261, -59, -53, 272, -55, -53));
            Checkpoints.add(new Checkpoint(plugin, 1, 261, -59, -90, 272, -55, -90));
            Checkpoints.add(new Checkpoint(plugin, 3, 261, -59, -31, 272, -55, -31));
            Checkpoints.add(new Checkpoint(plugin, 4, 272, -59, -161, 274, -55, -168));
            Checkpoints.add(new Checkpoint(plugin, 5, 312, -59, -168, 312, -55, -161));
            Checkpoints.add(new Checkpoint(plugin, 6, 487, -59, -94, 272, -55, -94));
            Checkpoints.add(new Checkpoint(plugin, 7, 470, -59, 41, 470, -55, 57));
            Checkpoints.add(new Checkpoint(plugin, 8, 387, -59, 57, 387, -55, 41));
        } else if (Objects.equals(this.MapName, "sand")) {
            Checkpoints.add((new Checkpoint(plugin, 0, -264, -60, 55, -278, -54, 55)));
            Checkpoints.add(new Checkpoint(plugin, 1, -265, -60, 139, -277, -54, 139));
            Checkpoints.add(new Checkpoint(plugin, 2, -215, -60, 185, -215, -54, 193));
            Checkpoints.add(new Checkpoint(plugin, 3, -188, -55, 230, -180, -50, 230));
            Checkpoints.add(new Checkpoint(plugin, 4, -145, -60, 157, -137, -54, 157));
            Checkpoints.add(new Checkpoint(plugin, 5, -138, -60, 49, -144, -54, 49));
            Checkpoints.add(new Checkpoint(plugin, 6, -153, -60, -2, -153, -54, -13));
            Checkpoints.add(new Checkpoint(plugin, 7, -219, -60, -19, -219, -54, -14));
            Checkpoints.add(new Checkpoint(plugin, 8, -259, -60, -11, -277, -54, -11));
        }
    }
}