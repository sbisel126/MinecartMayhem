package io.github.sbisel126.minecartMayhem;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import io.github.sbisel126.minecartMayhem.Race.RacePlayer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class MinecartHandler {
    private final MinecartMayhem plugin;
    private final ProtocolManager protocolManager;
    private final Map<Player, Integer> movementState = new HashMap<>();
    private boolean frozenBoat = false;

    private BukkitTask task;
    private Boat boat;
    private ArmorStand modelStand;

    public MinecartHandler(MinecartMayhem plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void PutPlayerInCart(RacePlayer RP, boolean useBirchBoat) {
        Player player = RP.GetPlayer();
        Location loc = player.getLocation();
        this.boat = (Boat) player.getWorld().spawnEntity(loc, useBirchBoat ? EntityType.BIRCH_BOAT : EntityType.ACACIA_BOAT);
        boat.setSilent(true);
        boat.setInvulnerable(true);
        boat.setGravity(false);
        boat.addPassenger(player);

        frozenBoat = true;
        spawnKartModel(RP);
    }

    private void spawnKartModel(RacePlayer RP) {
        Player player = RP.GetPlayer();
        int cartChoice = plugin.db.GetPlayerBoatColor(player);
        int modelId = switch (cartChoice) {
            case 1 -> 123456;
            case 2 -> 123457;
            case 3 -> 123458;
            case 4 -> 123459;
            default -> 123456;
        };

        Bukkit.getLogger().info("[MinecartHandler] " + player.getName() + "'s saved cart = " + cartChoice + " → modelId = " + modelId);

        this.modelStand = (ArmorStand) boat.getWorld().spawnEntity(boat.getLocation(), EntityType.ARMOR_STAND);
        modelStand.setInvisible(true);
        modelStand.setMarker(true);
        modelStand.setGravity(false);
        modelStand.setInvulnerable(true);
        modelStand.setSilent(true);

        ItemStack modelItem = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = modelItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelId);
            modelItem.setItemMeta(meta);
        }
        modelStand.setHelmet(modelItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!boat.isValid() || !modelStand.isValid() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                Vector lookDir = boat.getLocation().getDirection().clone().setY(0).normalize();
                Location predicted = boat.getLocation().clone().add(lookDir.multiply(3));
                predicted.setYaw(boat.getLocation().getYaw());
                modelStand.teleport(predicted);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        startBoatControl(RP);
    }

    private void startBoatControl(RacePlayer RP) {
        Player player = RP.GetPlayer();
        final Map<Player, Boolean> isClimbing = new HashMap<>();

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (frozenBoat) {
                    event.setCancelled(true);
                    return;
                }

                PacketContainer packet = event.getPacket();
                StructureModifier<Boolean> booleans = packet.getStructures().read(0).getBooleans();
                if (booleans == null) return;

                boolean forward = booleans.read(0);
                boolean backward = booleans.read(1);

                if (forward) {
                    movementState.put(player, 1);
                    Vector direction = player.getLocation().getDirection().normalize();
                    Location frontLoc = boat.getLocation().clone().add(direction.clone().multiply(1.2));
                    frontLoc.setY(boat.getLocation().getY());

                    boolean needsToClimb = false;
                    if (!frontLoc.getBlock().isPassable()) {
                        Location above = frontLoc.clone().add(0, 1, 0);
                        if (above.getBlock().isPassable()) needsToClimb = true;
                    }

                    Material type = frontLoc.getBlock().getType();
                    if (type.name().contains("STAIRS") || type.name().contains("SLAB") || type.name().contains("STEP")) {
                        needsToClimb = !type.name().contains("SPRUCE_SLAB");
                    }
                    if (type.name().contains("WOOL")) {
                        needsToClimb = false;
                    }

                    isClimbing.put(player, needsToClimb);

                } else if (backward) {
                    movementState.put(player, -1);
                    isClimbing.put(player, false);
                } else {
                    movementState.put(player, 0);
                    isClimbing.put(player, false);
                }
            }
        });

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (boat.isDead() || !player.isInsideVehicle() || !(player.getVehicle() instanceof Boat)) {
                    this.cancel();
                    movementState.remove(player);
                    isClimbing.remove(player);
                    boat.remove();
                    if (modelStand != null) modelStand.remove();
                    player.teleport(new Location(player.getWorld(), -24, -60, 574));
                    plugin.GrassRaceQueue.RemovePlayer(player);
                    plugin.SandRaceQueue.RemovePlayer(player);
                    plugin.GrassRace.RemovePlayer(player);
                    plugin.SandRace.RemovePlayer(player);
                    return;
                }

                if (frozenBoat) {
                    boat.setVelocity(new Vector(0, 0, 0));
                    return;
                }

                int state = movementState.getOrDefault(player, 0);
                boolean climbing = isClimbing.getOrDefault(player, false);

                if (!boat.isOnGround() && !climbing) {
                    Vector v = boat.getVelocity();
                    v.setY(v.getY() - 0.5);
                    boat.setVelocity(v);
                }

                if (state == 1) {
                    Vector dir = boat.getLocation().getDirection().normalize();
                    if (climbing && boat.isOnGround() && boat.getVelocity().getY() < 0.7) {
                        boat.setVelocity(dir.multiply(1.0).add(new Vector(0, 0.7, 0)));
                    } else {
                        boat.setVelocity(dir.multiply(1.5).add(new Vector(0, -0.5, 0)));
                    }
                    boat.getWorld().spawnParticle(Particle.CLOUD, boat.getLocation(), 4, 0.2, 0.2, 0.2, 0.01);
                } else if (state == -1) {
                    Vector dir = boat.getLocation().getDirection().normalize().multiply(-1);
                    boat.setVelocity(dir.multiply(1.1));
                } else {
                    boat.setVelocity(boat.getVelocity().multiply(0.8));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void setFrozenBoat(boolean frozenBoat) {
        this.frozenBoat = frozenBoat;
    }

    public void stopBoatControl() {
        if (task != null) task.cancel();
        if (boat != null) boat.remove();
        if (modelStand != null) modelStand.remove();
    }
}
