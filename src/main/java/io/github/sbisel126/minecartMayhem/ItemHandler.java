package io.github.sbisel126.minecartMayhem;

import io.github.sbisel126.minecartMayhem.Race.RacePlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;

public class ItemHandler implements Listener {
    private final MinecartMayhem plugin;
    private final Set<Location> itemBoxLocations = new HashSet<>();
    private final Map<Location, Boolean> activeItemBoxes = new HashMap<>();
    private final Map<Location, EnderCrystal> itemBoxCrystals = new HashMap<>();
    private final Map<Projectile, Integer> shellBounceCount = new HashMap<>();
    private final Set<UUID> shieldedPlayers = new HashSet<>();
    private final Map<UUID, Queue<ItemStack>> playerItems = new HashMap<>();
    private final int MAX_ITEMS = 3;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    public ItemHandler(MinecartMayhem plugin) {
        this.plugin = plugin;
    }


    public void registerItemBox(Location location) {
        if (!itemBoxLocations.contains(location)) {
            itemBoxLocations.add(location);
        }

        activeItemBoxes.put(location, true);
        location.getBlock().setType(Material.LIGHT_BLUE_SHULKER_BOX);

        EnderCrystal old = itemBoxCrystals.remove(location);
        if (old != null && !old.isDead()) old.remove();

        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnCrystal(location), 10L);
    }

    private void spawnCrystal(Location location) {
        Location crystalLoc = location.clone().add(0.5, 1, 0.5);
        EnderCrystal crystal = location.getWorld().spawn(crystalLoc, EnderCrystal.class);
        crystal.setShowingBottom(false);
        crystal.setInvulnerable(true);
        itemBoxCrystals.put(location, crystal);
        Bukkit.getLogger().info("[ItemBox] Spawned crystal at: " + location);
    }

    public void removeItemBox(Location location) {
        Bukkit.getLogger().info("[ItemBox] Removing box at: " + location);

        location.getBlock().setType(Material.AIR);
        activeItemBoxes.put(location, false);

        EnderCrystal crystal = itemBoxCrystals.remove(location);
        if (crystal != null && !crystal.isDead()) crystal.remove();

        new BukkitRunnable() {
            @Override
            public void run() {
                location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
                Bukkit.getLogger().info("[ItemBox] Respawning box at: " + location);
                registerItemBox(location);
            }
        }.runTaskLater(plugin, 100L);
    }

    public void saveItemBoxLocations() {
        File file = new File(plugin.getDataFolder(), "itemboxes.yml");
        try (FileWriter writer = new FileWriter(file)) {
            for (Location loc : itemBoxLocations) {
                writer.write(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void loadItemBoxLocations() {
        File file = new File(plugin.getDataFolder(), "itemboxes.yml");
        if (!file.exists()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split(",");
                    World world = Bukkit.getWorld(split[0]);
                    if (world == null) continue;

                    int x = Integer.parseInt(split[1]);
                    int y = Integer.parseInt(split[2]);
                    int z = Integer.parseInt(split[3]);
                    Location loc = new Location(world, x, y, z);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> registerItemBox(loc), 20L);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 20L);
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
                saveItemBoxLocations();
            } else {
                player.sendMessage(ChatColor.GRAY + "No item box found to remove.");
            }
        } else {
            event.setCancelled(true);
            registerItemBox(boxLocation);
            player.sendMessage(ChatColor.GREEN + "Item Box placed at " + formatLoc(boxLocation));
            player.playSound(boxLocation, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            saveItemBoxLocations();
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
        player.getInventory().addItem(randomItem);
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
        // Also remove one matching item from their inventory
        player.getInventory().removeItem(item);
        return true;
    }

    // --- Power-up usage ---
    @EventHandler
    public void onUseGreenShell(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Green Shell")) return;
        event.setCancelled(true);

        // Launch a snowball as the shell
        Snowball shell = player.launchProjectile(Snowball.class);
        shell.setCustomName("GreenShell");

        // Boost velocity forward
        Vector velocity = player.getLocation().getDirection().normalize().multiply(1.3);
        shell.setVelocity(velocity);

        // Track bounces
        shellBounceCount.put(shell, 0);

        // Optional trail
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shell.isDead() || !shell.isValid()) {
                    cancel();
                    return;
                }
                shell.getWorld().spawnParticle(Particle.HAPPY_VILLAGER , shell.getLocation(), 2, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    @EventHandler
    public void onUseSpeedBoost(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!useQueuedItem(player, "Speed Boost")) return;
        event.setCancelled(true);

        player.sendMessage(ChatColor.YELLOW + "💨 Speed Boost activated!");

        if (!(player.getVehicle() instanceof Boat boat)) return;

        Vector direction = player.getLocation().getDirection().normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 30 || boat.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Apply forward boost with slight downward gravity
                Vector boost = direction.clone().multiply(1.5).add(new Vector(0, -0.2, 0));
                boat.setVelocity(boost);

                boat.getWorld().spawnParticle(Particle.CLOUD, boat.getLocation(), 3, 0.2, 0.2, 0.2, 0.01);
                boat.getWorld().playSound(boat.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1.2f);
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (egg.isDead() || !egg.isValid()) {
                    cancel();
                    return;
                }
                egg.getWorld().spawnParticle(Particle.SMOKE, egg.getLocation(), 2, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    @EventHandler
    public void onSmokeBombHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Egg egg)) return;
        if (!"SmokeBomb".equals(egg.getCustomName())) return;

        Location hitLoc = egg.getLocation();

        // Particle cloud
        hitLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, hitLoc, 60, 1.5, 1, 1.5, 0.01);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_CREEPER_PRIMED, 1f, 1.2f);

        // Blind nearby players (optional)
        for (Entity entity : hitLoc.getWorld().getNearbyEntities(hitLoc, 4, 2, 4)) {
            if (entity instanceof Player nearby) {
                nearby.sendMessage(ChatColor.DARK_GRAY + "☁ You got smoked!");
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1)); // 3 seconds
            }
        }
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
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 8, 0.5, 0.7, 0.5, 0.1);
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
            default -> { return; }
        }

        velocity.setY(0);
        shell.setVelocity(velocity);
        shellBounceCount.put(shell, bounces + 1);
        shell.getWorld().playSound(shell.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, 0.8f, 1.2f);
    }

    @EventHandler
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (itemBoxCrystals.containsValue(crystal)) {
            event.setCancelled(true);
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
    private void syncQueueWithInventory(Player player) {
        Queue<ItemStack> queue = playerItems.get(player.getUniqueId());
        if (queue == null || queue.isEmpty()) return;

        // Remove items from the queue if they are no longer in the inventory
        queue.removeIf(item -> !player.getInventory().contains(item));
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            syncQueueWithInventory(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            syncQueueWithInventory(player);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        syncQueueWithInventory(player);
    }
    @EventHandler
    public void onGreenShellHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball shell)) return;
        if (!(event.getEntity() instanceof Player hitPlayer)) return;
        if (!"GreenShell".equals(shell.getCustomName())) return;

        Player shooter = (Player) shell.getShooter();
        if (shooter == null || shooter.equals(hitPlayer)) return; // avoid hitting self

        event.setCancelled(true); // prevent damage

        // Apply spin-out effect
        hitPlayer.setVelocity(hitPlayer.getLocation().getDirection().multiply(-0.5).setY(0.3)); // knock back a bit
        hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        hitPlayer.getWorld().spawnParticle(Particle.CRIT, hitPlayer.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);

        // Optional: freeze movement briefly
        hitPlayer.setWalkSpeed(0f);
        new BukkitRunnable() {
            @Override
            public void run() {
                hitPlayer.setWalkSpeed(0.2f); // restore default speed
            }
        }.runTaskLater(plugin, 40L); // 2 seconds

        // Clean up the shell
        shell.remove();
        shellBounceCount.remove(shell);
    }
    @EventHandler
    public void onIceBallHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player hitPlayer)) return;
        if (!(event.getDamager() instanceof Snowball ball)) return;
        if (!"Ice Ball".equals(ball.getCustomName())) return;

        UUID uuid = hitPlayer.getUniqueId();

        // Notify and add to frozen list
        hitPlayer.sendMessage(ChatColor.AQUA + "❄ You've been frozen!");
        hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        hitPlayer.getWorld().spawnParticle(Particle.SNOWFLAKE, hitPlayer.getLocation().add(0, 1, 0), 20, 0.4, 0.8, 0.4, 0.01);
        frozenPlayers.add(uuid);

        // Freeze their kart (if in one)
        if (hitPlayer.getVehicle() instanceof Boat) {
            // Check if they're in the Grass Race
            for (RacePlayer rp : plugin.GrassRace.getPlayers()) {
                if (rp.GetPlayer().getUniqueId().equals(uuid)) {
                    rp.minecart.setFrozenBoat(true);
                    break;
                }
            }
            // Check if they're in the Sand Race
            for (RacePlayer rp : plugin.SandRace.getPlayers()) {
                if (rp.GetPlayer().getUniqueId().equals(uuid)) {
                    rp.minecart.setFrozenBoat(true);
                    break;
                }
            }
        }

        // Unfreeze after delay (3 seconds)
        new BukkitRunnable() {
            @Override
            public void run() {
                frozenPlayers.remove(uuid);
                // Unfreeze kart
                for (RacePlayer rp : plugin.GrassRace.getPlayers()) {
                    if (rp.GetPlayer().getUniqueId().equals(uuid)) {
                        rp.minecart.setFrozenBoat(false);
                        break;
                    }
                }
                for (RacePlayer rp : plugin.SandRace.getPlayers()) {
                    if (rp.GetPlayer().getUniqueId().equals(uuid)) {
                        rp.minecart.setFrozenBoat(false);
                        break;
                    }
                }
                hitPlayer.sendMessage(ChatColor.GREEN + "☀ You're thawed out!");
            }
        }.runTaskLater(plugin, 60L); // 3 seconds (20 ticks/sec)
    }
    @EventHandler
    public void onRightClickWhileRiding(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || !held.hasItemMeta()) return;
        String display = ChatColor.stripColor(held.getItemMeta().getDisplayName());

        // Manually trigger the matching power-up use
        switch (display) {
            case "Green Shell" -> onUseGreenShell(event);
            case "Fireball" -> onUseFireball(event);
            case "Ice Ball" -> onUseIceBall(event);
            case "Speed Boost" -> onUseSpeedBoost(event);
            case "Smoke Bomb" -> onUseSmokeBomb(event);
            case "Temporary Shield" -> onUseShield(event);
        }
    }
}
