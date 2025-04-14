package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.MinecartHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

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
        // set a hard maximum of 5 players in the queue
        long currentCount = playersInQueue.stream().filter(Objects::nonNull).count();
        if (currentCount >= 5) {
            player.sendMessage("The race is full. Try again later.");
            return;
        }
        // disallow the same player from joining several times
        // this removes them from the queue if they are already in it
        // then they get added back in, in the proper place
        playersInQueue.removeIf(rp -> rp != null && rp.GetUsername().equalsIgnoreCase(player.getName()));

        // check if the queue is full


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
            NewRP.SetStartPos(BaseX + (2*(1+PlayerID)), BaseY, BaseZ);
            NewRP.player.teleport(new Location(player.getWorld(), BaseX + (2*(1+PlayerID)), BaseY, BaseZ, -175, 5));
        } else if (Objects.equals(MapName, "sand")) {
            int PlayerID = playersInQueue.indexOf(NewRP);
            int BaseX = -267;
            int BaseY = -60;
            int BaseZ = 50;
            NewRP.SetStartPos(BaseX + (2*(1+PlayerID)), BaseY, BaseZ);
            NewRP.player.teleport(new Location(player.getWorld(), BaseX - (2*(1+PlayerID)), BaseY, BaseZ, 0, 5));
        }
        MinecartHandler MinecartHandler = new MinecartHandler(plugin);
        MinecartHandler.PutPlayerInCart(NewRP, true);
        NewRP.setMinecart(MinecartHandler);
    }

    public void StartChecks() {
        if (active) return; // already running
        active = true;
        // sets the time after 2+ players join a race until race starts
        countdownSeconds = 15;

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
                    countdownSeconds = 15;
                    return;
                }

                if (countdownSeconds <= 0) {
                    // timer complete! Send players off to the races here.
                    StopChecks();

                    // only pass the non-null Race Players to the game
                    List<RacePlayer> validPlayers = playersInQueue.stream()
                            .filter(Objects::nonNull)
                            .toList();

                    race.AddPlayers(validPlayers);

                    // now that the players have been handed off to the RaceHandler, lets reset the Queue object
                    Collections.fill(playersInQueue, null);
                    cancel();

                    // we're out of here!
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

    public void RemovePlayer(Player player) {
        for (int i = 0; i < playersInQueue.size(); i++) {
            RacePlayer rp = playersInQueue.get(i);
            if (rp != null && rp.GetUsername().equalsIgnoreCase(player.getName())) {
                playersInQueue.set(i, null);
                player.sendMessage("You have been removed from the race queue.");
                plugin.getLogger().info(player.getName() + " was removed from the race queue.");
                return;
            }
        }
    }
}
