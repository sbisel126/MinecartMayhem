package io.github.sbisel126.minecartMayhem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class MinecartMayhem extends JavaPlugin implements Listener {
    private MiniMessage miniMessage;
    private DatabaseHandler db;
    private CommandHandler commander;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.miniMessage = MiniMessage.miniMessage();
        ComponentLogger logger = getComponentLogger();
        this.db = new DatabaseHandler(logger);
        this.commander = new CommandHandler();

        logger.info(Component.text("Hello world!"));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Component text = this.miniMessage.deserialize("Plugin <green>MinecartMayhem</green> loaded successfully.<br>" +
                "   Not an official Minecraft product. <br>" +
                "   Not approved by or associated with Mojang. <br>");
        event.getPlayer().sendMessage(text);
        if(event.getPlayer().isOp()){
            Component opMessage = this.miniMessage.deserialize("<green>MinecartMayhem</green> detected you are a server <red>operator</red><br>" +
                            "   Available <red>operator</red> commands are:<br>" +
                            "   <yellow>ExampleCommand1</yellow>: Description of command1<br>" +
                            "   <yellow>ExampleCommand2</yellow>: Description of command2");
            event.getPlayer().sendMessage(opMessage);

        }else{
            Component message = this.miniMessage.deserialize("<green>MinecartMayhem</green> ");
            event.getPlayer().sendMessage(message);
        }
    }

}
