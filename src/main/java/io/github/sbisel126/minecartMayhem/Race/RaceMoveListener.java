package io.github.sbisel126.minecartMayhem.Race;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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
                            player.sendMessage(Component.text("it took you " + raceHandler.getCurrentRaceTime() + " seconds").color(NamedTextColor.GREEN));
                            return;
                        }
                        // so they aren't done yet, lets see if we can increment their lap count or not
                        // if all the checkpoints aren't flagged as crossed by them, we don't increment the lap count
                        if (racePlayer.CheckpointsCrossed.size() != raceHandler.Checkpoints.size() - 1) {
                            return;
                        }
                        // if they make it here, they have crossed all the checkpoints
                        // we increment their lap count and reset their checkpoint list
                        racePlayer.CheckpointsCrossed.clear();
                        racePlayer.currentLap++;
                        // this is a little workaround so the player isn't presented with a first lap notification
                        if (racePlayer.currentLap == 1) {
                            return;
                        }
                        // display the lap count to the player
                        player.showTitle(Title.title(Component.text(racePlayer.currentLap + " / 3"), Component.text("")));
                        //raceHandler.plugin.getLogger().info(player.getName() + " completed lap " + racePlayer.currentLap);
                    } else {
                        // so this isn't the finish line.
                        // otherwise, we just update the last checkpoint
                        racePlayer.addCheckpointCrossed(checkpoint.getCheckpointID());
                        raceHandler.plugin.getLogger().info(player.getName() + " crossed a checkpoint at time " + raceHandler.getCurrentRaceTime());
                    }
                    break;
                }
            }
        }
    }
}