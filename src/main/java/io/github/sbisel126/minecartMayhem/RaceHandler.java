package io.github.sbisel126.minecartMayhem;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaceHandler {
    private JavaPlugin plugin;
    private final ScheduledExecutorService scheduler;
    private Instant startTime;
    private Integer baseScore;
    private Integer difficulty;
    private long maxTime;
    private Player player;

    public RaceHandler(JavaPlugin plugin, Player player, Integer baseMapScore, Integer difficultyMultiplier, long maxTime) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.baseScore = baseMapScore;
        this.difficulty = difficultyMultiplier;
        this.maxTime = maxTime;
        this.plugin = plugin;
        this.player = player;
    }

    public void startRace() {
        //Location loc = player.getLocation();
        this.startTime = Instant.now();
        new BukkitRunnable () {
            @Override
            public void run () {
                Instant endTime = Instant.now();
                // Calculates the final score based on time and difficulty modifiers.
                double totalScore = baseScore.doubleValue();
                long runTime = Duration.between(startTime, endTime).toMillis();

                if (runTime < maxTime) {
                    totalScore *= (((double) maxTime / runTime) * 2.0);
                } else {
                    totalScore *= ((double) maxTime / runTime);
                }
                totalScore *= difficulty.doubleValue();

                player.sendMessage("Your score is " + totalScore);
            }
        }.runTaskLaterAsynchronously(plugin, maxTime);
    }

}
