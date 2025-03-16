package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CartMenu implements Listener, CommandExecutor {
    private String invName = "Cart Selector";
    private ComponentLogger logger;
    private final Integer red_cart = 11;
    private final Integer blue_cart = 15;

    public CartMenu(ComponentLogger logger, MinecartMayhem plugin){
        this.logger = logger;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("MapMenu initialized."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if (!event.getView().getTitle().equals(invName)){
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == red_cart){
            player.sendMessage("Red Cart selected");
            //player.teleport();
        } else if (slot == blue_cart) {
            player.sendMessage("Blue Cart selected");
        } else {
            player.sendMessage("Empty box selected");
        }

        event.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String  label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        Inventory inv = Bukkit.createInventory(player, 9*4, invName);


        inv.setItem(red_cart, getItem(new ItemStack(Material.REDSTONE_BLOCK), "&9Red Cart", "&aClick to select", "&aRace with the Red Cart"));
        inv.setItem(blue_cart, getItem(new ItemStack(Material.LAPIS_BLOCK), "&9Blue Cart", "&aClick to select", "&aRace with the Blue Cart"));

        player.openInventory(inv);

        return true;
    }

    private ItemStack getItem(ItemStack item, String name, String ... lore){
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> lores = new ArrayList<>();

        for(String s : lore){
            lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        meta.setLore(lores);

        item.setItemMeta(meta);
        return item;
    }
}
