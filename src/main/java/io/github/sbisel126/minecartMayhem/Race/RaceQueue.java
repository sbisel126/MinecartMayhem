package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

//RaceQueue is the waiting zone that players get assigned before they get placed in the actual race
//RaceQueue ensures that >1 user is in a race before starting it
public class RaceQueue {
    MinecartMayhem plugin;
    RaceHandler race;
    Boolean active = false;
    private int countdownSeconds = 60;
    private final List<Player> playersInQueue = new ArrayList<>() {
    };

    public RaceQueue(MinecartMayhem plugin, RaceHandler race) {
        this.race = race;
        this.plugin = plugin;
    }

    public void AddPlayer(Player player) {
        playersInQueue.add(player);
        if (!active) {
            StartChecks();
        }
    }

    public void RemovePlayer(Player player) {
        playersInQueue.remove(player);
    }

    public void StartChecks() {
        if (active) return; // already running
        active = true;
        countdownSeconds = 60;

        // not enough players, reset countdown
        // timer complete! Send players off to the races here.
        // optionally notify players of the countdown
        BukkitRunnable countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                // not enough players, reset countdown
                if (playersInQueue.size() < 2) {
                    countdownSeconds = 60;
                    return;
                }

                if (countdownSeconds <= 0) {
                    // timer complete! Send players off to the races here.
                    StopChecks();
                    for (Player player : playersInQueue) {
                        race.AddPlayer(player);
                    }
                    cancel();
                    return;
                }

                // optionally notify players of the countdown
                for (Player p : playersInQueue) {
                    p.sendMessage("Race starting in " + countdownSeconds + " seconds...");
                }

                countdownSeconds--;
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 second
    }
    public void StopChecks() {
        active = false;
    }
}
