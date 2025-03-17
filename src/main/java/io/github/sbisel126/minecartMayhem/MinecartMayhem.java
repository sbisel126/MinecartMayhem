package io.github.sbisel126.minecartMayhem;

import io.github.sbisel126.minecartMayhem.commands.CartMenu;
import io.github.sbisel126.minecartMayhem.commands.MapMenu;
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

        new RaceHandler(logger, this);

        logger.info(Component.text("Hello world!"));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // insert user into User Database
        db.InsertUser(player);
        // Display text to user
        Component text = this.miniMessage.deserialize("Plugin <green>MinecartMayhem</green> loaded successfully.<br>" +
                "   Not an official Minecraft product. <br>" +
                "   Not approved by or associated with Mojang. <br>");
        player.sendMessage(text);

        // send player to hub area
        player.teleport(new Location(player.getWorld(), -24, -60, 574));

        if(player.isOp()){
            Component opMessage = this.miniMessage.deserialize("<green>MinecartMayhem</green> detected you are a server <red>operator</red><br>" +
                            "   Available <red>operator</red> commands are:<br>" +
                            "   <yellow>ExampleCommand1</yellow>: Description of command1<br>" +
                            "   <yellow>ExampleCommand2</yellow>: Description of command2");
            player.sendMessage(opMessage);

        }else{
            Component message = this.miniMessage.deserialize("<green>MinecartMayhem</green> ");
            player.sendMessage(message);
        }
    }

}
