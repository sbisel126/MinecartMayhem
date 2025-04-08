package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import io.github.sbisel126.minecartMayhem.Race.RaceHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;


public class JoinRace implements Listener, CommandExecutor {
    private Date startTime;
    private RaceHandler race;

    public JoinRace(ComponentLogger logger, MinecartMayhem plugin){
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("JoinRace command initialized."));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
        } else {
            race.AddPlayer(player);
        }
        return true;
    }
}
