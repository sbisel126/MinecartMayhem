package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MapMenu implements Listener, CommandExecutor {
    private final String invName = "Map Selector";
    private ComponentLogger logger;
    private final Integer grass_map = 11;
    private final Integer sand_map = 15;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MapMenu(ComponentLogger logger, MinecartMayhem plugin){
        this.logger = logger;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("MapMenu initialized."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if (!event.getView().title().toString().contains(invName)){
            //This is not the inventory you're looking for...
            return;
        }

        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();

        if (slot == grass_map){
            player.sendMessage("Grass Map selected");
            player.teleport(new Location(player.getWorld(), 266.0, -59.0, -52.0, -175, 5));
            event.getInventory().close();
        } else if (slot == sand_map) {
            player.sendMessage("Sand Map selected");
            player.teleport(new Location(player.getWorld(), -270.0, -60.0, 52.0, 1, 0));
            event.getInventory().close();
        }

        event.setCancelled(true);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String  label, String @NotNull [] args){
        if(!(sender instanceof Player player)){
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, miniMessage.deserialize(invName));

        inv.setItem(grass_map, getItem(new ItemStack(Material.GRASS_BLOCK), "Grass Map", "Click to join", "Race on the Grass Map"));
        inv.setItem(sand_map, getItem(new ItemStack(Material.SAND), "Sand Map", "Click to join", "Race on the Sand Map"));

        player.openInventory(inv);

        return true;
    }

    private ItemStack getItem(ItemStack item, String name, String ... lore){
        ItemMeta meta = item.getItemMeta();
        meta.customName(miniMessage.deserialize(String.format("<green>%s</green>", name)));

        List<Component> lores = new ArrayList<>();

        for(String s : lore){
            lores.add(miniMessage.deserialize(String.format("<blue>%s</blue>", s)));
        }
        meta.lore(lores);

        item.setItemMeta(meta);
        return item;
    }
}
