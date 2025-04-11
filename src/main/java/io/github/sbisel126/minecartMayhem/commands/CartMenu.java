package io.github.sbisel126.minecartMayhem.commands;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class CartMenu implements Listener, CommandExecutor {
    private final String invName = "Cart Selector";
    // these define the inventory slots for the Cart Selection menu
    private static final int Cart_one = 10;
    private static final int Cart_two = 12;
    private static final int Cart_three = 14;
    private static final int Cart_four = 16;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final DatabaseHandler db;

    public CartMenu(MinecartMayhem plugin) {
        this.db = plugin.db;
        ComponentLogger logger = plugin.PluginLogger;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info(Component.text("CartMenu initialized."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().toString().contains(invName)) {
            //This is not the inventory you're looking for...
            return;
        }
        if (!(event.getAction() == InventoryAction.PICKUP_ALL)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        switch (slot) {
            case Cart_one:
                player.sendMessage("Updated Preference: Cart 1");
                db.SetPlayerBoatColor(player, 1);
                break;
            case Cart_two:
                player.sendMessage("Updated Preference: Cart 2");
                db.SetPlayerBoatColor(player, 2);
                break;
            case Cart_three:
                player.sendMessage("Updated Preference: Cart 3");
                db.SetPlayerBoatColor(player, 3);
                break;
            case Cart_four:
                player.sendMessage("Updated Preference: Cart 4");
                db.SetPlayerBoatColor(player, 4);
                break;
            default:
                break;
        }
        event.getInventory().close();
        event.setCancelled(true);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, miniMessage.deserialize(invName));

        inv.setItem(Cart_one, getItem(new ItemStack(Material.REDSTONE_BLOCK), "Cart 1", "Click to select", "Race with the Red Cart"));
        inv.setItem(Cart_two, getItem(new ItemStack(Material.LAPIS_BLOCK), "Cart 2", "Click to select", "Race with the Blue Cart"));
        inv.setItem(Cart_three, getItem(new ItemStack(Material.IRON_BLOCK), "Cart 3", "Click to select", "Race with the Red Cart"));
        inv.setItem(Cart_four, getItem(new ItemStack(Material.GOLD_BLOCK), "Cart 4", "Click to select", "Race with the Red Cart"));

        player.openInventory(inv);

        return true;
    }

    private ItemStack getItem(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        meta.customName(miniMessage.deserialize(String.format("<green>%s</green>", name)));

        List<Component> lores = new ArrayList<>();

        for (String s : lore) {
            lores.add(miniMessage.deserialize(String.format("<blue>%s</blue>", s)));
        }
        meta.lore(lores);

        item.setItemMeta(meta);
        return item;
    }
}
