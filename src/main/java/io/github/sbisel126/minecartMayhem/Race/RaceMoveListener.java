package io.github.sbisel126.minecartMayhem.Race;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RaceMoveListener implements Listener {
    private final RaceHandler raceHandler;

    public RaceMoveListener(RaceHandler raceHandler) {
        this.raceHandler = raceHandler;
    }

    // used for string formatting
    private static String getOrdinal(int number) {
        if (number >= 11 && number <= 13) {
            return number + "th";
        }
        return switch (number % 10) {
            case 1 -> number + "st";
            case 2 -> number + "nd";
            case 3 -> number + "rd";
            default -> number + "th";
        };
    }

    private static String formatTime(int secs) {
        int minutes = secs / 60;
        int seconds = secs % 60;
        return String.format("%d:%02d", minutes, seconds);
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
            long now = System.currentTimeMillis();
            long checkpointCooldown = 1000;

            for (Checkpoint checkpoint : raceHandler.Checkpoints) {
                if (checkpoint.CheckPlayerInCheckpoint(x, y, z)) {
                    // if it's the finish line, we have some things to check
                    if (!racePlayer.canTriggerCheckpoint(checkpoint.getCheckpointID(), now, checkpointCooldown)) {
                        return;
                    }

                    if (checkpoint.getCheckpointID() == 0) {
                        // if player lapcount = 3 we kick them out of the race and log their time
                        if (racePlayer.currentLap == 3) {
                            // end of race logic triggers:
                            racePlayer.setFinishTime(raceHandler.getCurrentRaceTime());
                            racePlayer.setRacing(false);
                            raceHandler.CompletedRacerCount++;

                            // send them to the hub and disable their boat
                            racePlayer.minecart.stopBoatControl();
                            player.teleport(new Location(player.getWorld(), -24, -60, 574));

                            // player notification
                            player.sendMessage("You got " + getOrdinal(raceHandler.CompletedRacerCount) + " place!");
                            player.sendMessage("Final time: " + formatTime(racePlayer.getFinishTime()));
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
                        //raceHandler.plugin.getLogger().info(player.getName() + " crossed a checkpoint at time " + raceHandler.getCurrentRaceTime());
                    }
                    break;
                }
            }
        }
    }
}