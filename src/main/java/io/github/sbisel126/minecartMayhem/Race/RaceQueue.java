package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.MinecartHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

//RaceQueue is the waiting zone that players get assigned before they get placed in the actual race
//RaceQueue ensures that >1 user is in a race before starting it
public class RaceQueue {
    MinecartMayhem plugin;
    RaceHandler race;
    Boolean active = false;
    String MapName;
    private int countdownSeconds = 60;
    private final List<RacePlayer> playersInQueue = new ArrayList<>(Collections.nCopies(5 ,null)) {};

    public RaceQueue(MinecartMayhem plugin, RaceHandler race, String MapName) {
        this.race = race;
        this.plugin = plugin;
        this.MapName = MapName;
    }

    public void AddPlayer(Player player) {
        //Create RacePlayer object
        RacePlayer NewRP = new RacePlayer(player, plugin.db);
        if (!active) {
            StartChecks();
        }

        // iterate over arraylist, find index of first null
        // insert player into null
        this.playersInQueue.add(playersInQueue.indexOf(null), NewRP);
        //Teleport user to race area based on respective map
        if (Objects.equals(MapName, "grass")) {
            //get the slot the user should be in 1-5
            //determine correct coordinates, teleport.
            int PlayerID = playersInQueue.indexOf(NewRP);
            int BaseX = 262;
            int BaseY = -59;
            int BaseZ = -52;
            player.teleport(new Location(player.getWorld(), BaseX + (2*(1+PlayerID)), BaseY, BaseZ, -175, 5));
        }
        MinecartHandler MinecartHandler = new MinecartHandler(plugin);
        MinecartHandler.PutPlayerInCart(player, NewRP.CartColor);
    }

    public void StartChecks() {
        if (active) return; // already running
        active = true;
        // sets the time after 2+ players join a race until race starts
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
                // due to our... interesting implementation of player slots,
                // this block is used to calculate the real player count, as fetching the array length
                // will always return 5.
                int playercount = 0;
                for (RacePlayer racePlayer : playersInQueue) {
                    if(racePlayer != null) {
                        playercount++;
                    }
                }

                // not enough players, reset countdown
                if (playercount < 2) {
                    countdownSeconds = 60;
                    return;
                }

                if (countdownSeconds <= 0) {
                    // timer complete! Send players off to the races here.
                    StopChecks();
                    for (RacePlayer p : playersInQueue) {
                        if(p == null) {
                            continue;
                        }

                        p.player.sendMessage("Race start!");
                        race.AddPlayers(playersInQueue);
                    }
                    cancel();
                    return;
                }

                // notify players of the countdown
                if(countdownSeconds == 15 ||countdownSeconds == 30 || countdownSeconds == 60) {
                    for (RacePlayer p : playersInQueue) {
                        if(p == null) {
                            continue;
                        }
                        p.player.sendMessage("Race starting in " + countdownSeconds + " seconds...");
                    }
                } else if (countdownSeconds <= 5) {
                    for (RacePlayer p : playersInQueue) {
                        if(p == null) {
                            continue;
                        }
                        p.player.sendMessage("Race starting in " + countdownSeconds + " seconds...");
                    }
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
