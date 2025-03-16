package io.github.sbisel126.minecartMayhem;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class RaceHandler implements Listener {

    private ArrayList<String> players = new ArrayList<String>();
    private ComponentLogger logger;

    public RaceHandler(ComponentLogger logger, MinecartMayhem plugin){
        this.logger = logger;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("RaceHandler initialized."));
    }

}
