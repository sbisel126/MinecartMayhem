package io.github.sbisel126.minecartMayhem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemHandler implements Listener {
    private final JavaPlugin plugin;

    private final Set<Location> itemBoxLocations = new HashSet<>();
    private final Map<Location, Boolean> activeItemBoxes = new HashMap<>();
    private final Map<Location, EnderCrystal> itemBoxCrystals = new HashMap<>();
    private final Map<Projectile, Integer> shellBounceCount = new HashMap<>();
    private final Set<UUID> shieldedPlayers = new HashSet<>();

    private final Map<UUID, Queue<ItemStack>> playerItems = new HashMap<>();
    private final int MAX_ITEMS = 3;

    public ItemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerItemBox(Location location) {
        itemBoxLocations.add(location);
        activeItemBoxes.put(location, true);
        spawnItemBox(location);
    }

    private void spawnItemBox(Location location) {
        location.getBlock().setType(Material.LIGHT_BLUE_SHULKER_BOX);
        Location crystalLoc = location.clone().add(0.5, 1, 0.5);
        EnderCrystal crystal = location.getWorld().spawn(crystalLoc, EnderCrystal.class);
        crystal.setShowingBottom(false);
        crystal.setInvulnerable(true);
        itemBoxCrystals.put(location, crystal);
    }

    private void removeItemBox(Location location) {
        location.getBlock().setType(Material.AIR);
        activeItemBoxes.put(location, false);
        EnderCrystal crystal = itemBoxCrystals.remove(location);
        if (crystal != null && !crystal.isDead()) crystal.remove();

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnItemBox(location);
                activeItemBoxes.put(location, true);
            }
        }.runTaskLater(plugin, 300L);
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    @EventHandler
    public void onPlaceOrRemoveItemBox(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.STICK) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (!displayName.equalsIgnoreCase("Item Box Wand")) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Location boxLocation = clicked.getLocation().add(0, 1, 0);

        if (player.isSneaking()) {
            if (activeItemBoxes.containsKey(boxLocation)) {
                boxLocation.getBlock().setType(Material.AIR);
                itemBoxLocations.remove(boxLocation);
                activeItemBoxes.remove(boxLocation);

                EnderCrystal crystal = itemBoxCrystals.remove(boxLocation);
                if (crystal != null && !crystal.isDead()) crystal.remove();

                player.sendMessage(ChatColor.RED + "Item Box removed at " + formatLoc(boxLocation));
                player.playSound(boxLocation, Sound.ENTITY_ITEM_BREAK, 1f, 1.5f);
            } else {
                player.sendMessage(ChatColor.GRAY + "No item box found to remove.");
            }
        } else {
            event.setCancelled(true);
            registerItemBox(boxLocation);
            player.sendMessage(ChatColor.GREEN + "Item Box placed at " + formatLoc(boxLocation));
            player.playSound(boxLocation, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat)) return;

        for (Map.Entry<Location, EnderCrystal> entry : itemBoxCrystals.entrySet()) {
            EnderCrystal crystal = entry.getValue();
            if (crystal == null || crystal.isDead()) continue;

            if (player.getLocation().distanceSquared(crystal.getLocation()) < 1.2) {
                giveRandomItem(player);
                removeItemBox(entry.getKey());
                break;
            }
        }
    }

    private void giveRandomItem(Player player) {
        UUID uuid = player.getUniqueId();
        Queue<ItemStack> items = playerItems.computeIfAbsent(uuid, k -> new LinkedList<>());
        if (items.size() >= MAX_ITEMS) {
            player.sendMessage(ChatColor.GRAY + "You already have 3 items. Use one first!");
            return;
        }

        List<ItemStack> powerUps = Arrays.asList(
                createItem(Material.FEATHER, "Speed Boost"),
                createItem(Material.SNOWBALL, "Ice Ball"),
                createItem(Material.TURTLE_EGG, "Green Shell"),
                createItem(Material.FIRE_CHARGE, "Fireball"),
                createItem(Material.SHIELD, "Temporary Shield"),
                createItem(Material.EGG, "Smoke Bomb")
        );

        ItemStack randomItem = powerUps.get(new Random().nextInt(powerUps.size()));
        items.add(randomItem);

        player.sendMessage(ChatColor.GOLD + "You got a " + ChatColor.stripColor(randomItem.getItemMeta().getDisplayName()) + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean useQueuedItem(Player player, String expectedName) {
        Queue<ItemStack> queue = playerItems.get(player.getUniqueId());
        if (queue == null || queue.isEmpty()) return false;

        ItemStack item = queue.peek();
        if (item == null || !item.hasItemMeta()) return false;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (!name.equalsIgnoreCase(expectedName)) return false;

        queue.poll();
        return true;
    }

    @EventHandler
    public void onShellHitBlock(ProjectileHitEvent event) {
        Projectile shell = event.getEntity();
        if (!"GreenShell".equals(shell.getCustomName())) return;
        if (!shellBounceCount.containsKey(shell)) return;
        if (event.getHitEntity() != null) return;

        int bounces = shellBounceCount.get(shell);
        if (bounces >= 5) {
            shell.remove();
            shellBounceCount.remove(shell);
            shell.getWorld().playSound(shell.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return;
        }

        Vector velocity = shell.getVelocity();
        BlockFace hitFace = event.getHitBlockFace();
        if (hitFace == null) return;

        switch (hitFace) {
            case EAST, WEST -> velocity.setX(-velocity.getX());
            case NORTH, SOUTH -> velocity.setZ(-velocity.getZ());
            default -> {
                return; // no bounce on UP/DOWN
            }
        }

        velocity.setY(0);
        shell.setVelocity(velocity);
        shellBounceCount.put(shell, bounces + 1);
        shell.getWorld().playSound(shell.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, 0.8f, 1.2f);
    }

    @EventHandler
    public void onUseFireball(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Fireball")) return;
        event.setCancelled(true);

        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        for (int i = -1; i <= 1; i++) {
            Fireball fireball = player.getWorld().spawn(origin, Fireball.class);
            fireball.setShooter(player);
            fireball.setCustomName("PowerFireball");
            fireball.setIsIncendiary(false);
            fireball.setYield(0);
            Vector spread = direction.clone().add(new Vector(i * 0.15, 0, 0)).normalize().multiply(0.6);
            fireball.setVelocity(spread);
        }
    }

    @EventHandler
    public void onUseIceBall(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Ice Ball")) return;
        event.setCancelled(true);

        Snowball iceBall = player.launchProjectile(Snowball.class);
        iceBall.setCustomName("Ice Ball");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (iceBall.isDead() || !iceBall.isValid()) {
                    cancel();
                    return;
                }
                Location loc = iceBall.getLocation();
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 4, 0.05, 0.02, 0.05, 0);
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 0, 0, 0, 1);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onUseSmokeBomb(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Smoke Bomb")) return;
        event.setCancelled(true);

        Egg egg = player.launchProjectile(Egg.class);
        egg.setCustomName("SmokeBomb");
    }

    @EventHandler
    public void onUseShield(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Temporary Shield")) return;
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        if (shieldedPlayers.contains(uuid)) return;

        shieldedPlayers.add(uuid);
        player.sendMessage(ChatColor.AQUA + "Shield activated for 5 seconds!");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !shieldedPlayers.contains(uuid)) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(
                        Particle.END_ROD,
                        player.getLocation().add(0, 1, 0),
                        8, 0.5, 0.7, 0.5, 0.1
                );
                if (++ticks >= 100) {
                    shieldedPlayers.remove(uuid);
                    player.sendMessage(ChatColor.RED + "Your shield has worn off.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onShieldedPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (shieldedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 10);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        }
    }

    @EventHandler
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        // Check if it's one of our item box crystals
        if (itemBoxCrystals.containsValue(crystal)) {
            event.setCancelled(true); // Cancel the explosion damage
        }
    }

    @EventHandler
    public void onProjectileHitCrystal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (!(event.getDamager() instanceof Projectile)) return;

        if (itemBoxCrystals.containsValue(crystal)) {
            event.setCancelled(true);
        }
    }
}
