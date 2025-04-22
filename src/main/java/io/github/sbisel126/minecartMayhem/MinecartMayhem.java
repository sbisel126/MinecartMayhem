package io.github.sbisel126.minecartMayhem;

import io.github.sbisel126.minecartMayhem.Race.RaceHandler;
import io.github.sbisel126.minecartMayhem.Race.RaceMoveListener;
import io.github.sbisel126.minecartMayhem.Race.RaceQueue;
import io.github.sbisel126.minecartMayhem.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public class MinecartMayhem extends JavaPlugin implements Listener {
    public MiniMessage miniMessage;
    public DatabaseHandler db;
    public RaceHandler GrassRace;
    public RaceHandler SandRace;
    public RaceQueue GrassRaceQueue;
    public RaceQueue SandRaceQueue;
    public ComponentLogger PluginLogger;

    // hello world!
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.miniMessage = MiniMessage.miniMessage();
        this.PluginLogger = getComponentLogger();
        this.db = new DatabaseHandler(this);

        // register our commands
        Objects.requireNonNull(getCommand("teleport_menu")).setExecutor(new TeleportMenu(this));
        Objects.requireNonNull(getCommand("cart_menu")).setExecutor(new CartMenu(this));
        Objects.requireNonNull(getCommand("join_race")).setExecutor(new JoinRace(this));
        Objects.requireNonNull(getCommand("itemboxwand")).setExecutor(new ItemBoxWandCommand());
        ItemHandler itemBoxManager = new ItemHandler(this);
        Bukkit.getPluginManager().registerEvents(itemBoxManager, this);
        itemBoxManager.loadItemBoxLocations();

        //Crank up some instances of Race for our RaceQueues
        this.GrassRace = new RaceHandler(this, "grass");
        this.SandRace = new RaceHandler(this, "sand");

        getServer().getPluginManager().registerEvents(new RaceMoveListener(GrassRace), this);
        getServer().getPluginManager().registerEvents(new RaceMoveListener(SandRace), this);

        // and here we make the queues that handle assigning players to the races
        this.GrassRaceQueue = new RaceQueue(this, GrassRace, "grass");
        this.SandRaceQueue = new RaceQueue(this, SandRace, "sand");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // insert user into User Database
        db.InsertUser(player);

        // set them to adventure mode
        player.setGameMode(GameMode.ADVENTURE);

        // send player to hub area
        player.teleport(new Location(player.getWorld(), -24, -60, 574));

        // display Welcome Text
        player.showTitle(Title.title(Component.text("Welcome to"), Component.text("Minecart Mayhem", NamedTextColor.RED)));
    }
}
