package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import io.github.sbisel126.minecartMayhem.RaceHandler;
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


public class StartRace implements Listener, CommandExecutor {
    private Date startTime;
    private final ComponentLogger logger;
    private RaceHandler race;
    private JavaPlugin plugin;

    public StartRace(ComponentLogger logger, MinecartMayhem plugin){
        this.logger = logger;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("StartRace command initialized."));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        } else if (!player.isInsideVehicle()){
            sender.sendMessage("You must be in a vehicle to start a race.");
            return true;
        } else {
            race = new RaceHandler(plugin, player, 100, 2, 1500);
            race.startRace();
            return true;
        }
    }
}
