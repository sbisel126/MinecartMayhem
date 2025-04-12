package io.github.sbisel126.minecartMayhem.Race;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RaceMoveListener implements Listener {
    private final RaceHandler raceHandler;

    public RaceMoveListener(RaceHandler raceHandler) {
        this.raceHandler = raceHandler;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        RacePlayer racePlayer = raceHandler.players.stream()
                .filter(rp -> rp.GetUsername().equals(player.getName()))
                .findFirst()
                .orElse(null);

        if (racePlayer != null && racePlayer.isRacing()) {
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();

            for (Checkpoint checkpoint : raceHandler.Checkpoints) {
                if (checkpoint.CheckPlayerInCheckpoint(x, y, z)) {
                    // if it's the finish line, we have some things to check
                    if (checkpoint.getCheckpointID() == 0) {
                        // if player lapcount = 3 we kick them out of the race and log their time
                        if (racePlayer.currentLap >= 3) {
                            player.sendMessage("You did it!!!");
                        }
                        racePlayer.currentLap++;
                        player.sendMessage("You have completed lap " + racePlayer.currentLap);
                        raceHandler.plugin.getLogger().info(player.getName() + " completed lap " + racePlayer.currentLap);
                    } else {
                        // otherwise, we just update the last checkpoint
                        racePlayer.lastCheckpoint = checkpoint.getCheckpointID();
                        player.sendMessage("You have crossed checkpoint " + racePlayer.lastCheckpoint);
                    }
                    //raceHandler.plugin.getLogger().info(player.getName() + " crossed a checkpoint at time !" + raceHandler.getCurrentRaceTime());
                    break;
                }
            }
        }
    }
}