package io.github.sbisel126.minecartMayhem;

import io.github.sbisel126.minecartMayhem.commands.CartMenu;
import io.github.sbisel126.minecartMayhem.commands.MapMenu;
import io.github.sbisel126.minecartMayhem.commands.StartRace;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public class MinecartMayhem extends JavaPlugin implements Listener {
    private MiniMessage miniMessage;
    private DatabaseHandler db;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.miniMessage = MiniMessage.miniMessage();
        ComponentLogger logger = getComponentLogger();
        this.db = new DatabaseHandler(logger);

        Objects.requireNonNull(getCommand("map_menu")).setExecutor(new MapMenu(logger, this));
        Objects.requireNonNull(getCommand("cart_menu")).setExecutor(new CartMenu(logger, this));
        Objects.requireNonNull(getCommand("start_race")).setExecutor(new StartRace(logger, this));

        ItemHandler itemBoxManager = new ItemHandler(this);
        Bukkit.getPluginManager().registerEvents(itemBoxManager, this);

        // Example: Add some item boxes at racetrack locations
        itemBoxManager.registerItemBox(new Location(Bukkit.getWorld("world"), 100, 65, 200));
        itemBoxManager.registerItemBox(new Location(Bukkit.getWorld("world"), 150, 65, 250));

        //logger.info(Component.text("Hello world!"));
    }

    @Override
    public void onDisable(){

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // insert user into User Database
        db.InsertUser(player);

        // send player to hub area
        player.teleport(new Location(player.getWorld(), -24, -60, 574));


    }
}
