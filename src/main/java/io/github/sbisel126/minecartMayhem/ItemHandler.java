package io.github.sbisel126.minecartMayhem;

import org.bukkit.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ItemHandler implements Listener {
    private final JavaPlugin plugin;
    private final Set<Location> itemBoxLocations = new HashSet<>();
    private final Map<Location, Boolean> activeItemBoxes = new HashMap<>();

    public ItemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerItemBox(Location location) {
        // Store the item box location and mark it as active
        itemBoxLocations.add(location);
        activeItemBoxes.put(location, true);
        spawnItemBox(location);
    }

    private void spawnItemBox(Location location) {
        // Place a Shulker Box as the item box
        location.getBlock().setType(Material.LIGHT_BLUE_SHULKER_BOX);
    }

    private void removeItemBox(Location location) {
        // Remove the item box visually and mark it as inactive
        location.getBlock().setType(Material.AIR);
        activeItemBoxes.put(location, false);

        // Respawn the item box after a cooldown
        new BukkitRunnable() {
            @Override
            public void run() {
                spawnItemBox(location);
                activeItemBoxes.put(location, true);
            }
        }.runTaskLater(plugin, 100L); // 5 seconds delay (100 ticks)
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player is in a boat
        if (!(player.getVehicle() instanceof Boat)) {
            return;
        }

        // Check if the player is near an item box
        Location playerLoc = player.getLocation();
        for (Location boxLoc : itemBoxLocations) {
            if (activeItemBoxes.getOrDefault(boxLoc, false) && playerLoc.distance(boxLoc) < 1.5) {
                giveRandomItem(player);
                removeItemBox(boxLoc);
                break;
            }
        }
    }

    private void giveRandomItem(Player player) {
        // Define a list of random power-ups
        List<ItemStack> powerUps = Arrays.asList(
                createItem(Material.FEATHER, "Speed Boost"),
                createItem(Material.SNOWBALL, "Ice Ball"),
               // banana model not in yet createItem(Material.BANANA, "Banana Peel"),
                createItem(Material.TURTLE_EGG, "Green Shell"),
                createItem(Material.FIRE_CHARGE, "Fireball"),
                createItem(Material.SHIELD, "Temporary Shield")
        );

        // Pick a random power-up
        ItemStack randomItem = powerUps.get(new Random().nextInt(powerUps.size()));

        // Give the item to the player
        player.getInventory().addItem(randomItem);
        player.sendMessage(ChatColor.GOLD + "You got a " + randomItem.getItemMeta().getDisplayName() + "!");
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
