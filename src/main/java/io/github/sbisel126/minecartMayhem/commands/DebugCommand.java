package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.jar.Attributes;

public class DebugCommand implements Listener, CommandExecutor {
    MinecartMayhem plugin;

    public DebugCommand(MinecartMayhem plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        displayRaceStartGraphic(player);
        return true;
    }

    private void displayRaceStartGraphic(Player player) {
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
}
