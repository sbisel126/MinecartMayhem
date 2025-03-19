package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.MinecartHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CartMenu implements Listener, CommandExecutor {
    private final String invName = "Cart Selector";
    private ComponentLogger logger;
    private final Integer red_cart = 11;
    private final Integer blue_cart = 15;
    private MinecartHandler minecartHandler;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CartMenu(ComponentLogger logger, MinecartMayhem plugin){
        this.logger = logger;
        this.minecartHandler = new MinecartHandler(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("MapMenu initialized."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if (!event.getView().title().toString().contains(invName)){
            //This is not the inventory you're looking for...
            return;
        }
        if (!(event.getAction() == InventoryAction.PICKUP_ALL)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == red_cart){
            player.sendMessage("Red Cart selected");
            minecartHandler.PutPlayerInCart(player, true);

            event.getInventory().close();
        } else if (slot == blue_cart) {
            player.sendMessage("Blue Cart selected");
            minecartHandler.PutPlayerInCart(player, false);
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

        inv.setItem(red_cart, getItem(new ItemStack(Material.REDSTONE_BLOCK), "Red Cart", "Click to select", "Race with the Red Cart"));
        inv.setItem(blue_cart, getItem(new ItemStack(Material.LAPIS_BLOCK), "Blue Cart", "Click to select", "Race with the Blue Cart"));

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
