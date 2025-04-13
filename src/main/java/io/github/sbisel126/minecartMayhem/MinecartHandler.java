package io.github.sbisel126.minecartMayhem;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import io.github.sbisel126.minecartMayhem.Race.RacePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class MinecartHandler {
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<Player, Integer> movementState = new HashMap<>();
    private boolean frozenBoat = false;
    private BukkitTask task;
    private Boat boat;

    public MinecartHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void PutPlayerInCart(RacePlayer RP, int cartType) {
        Player player = RP.GetPlayer();
        Location loc = player.getLocation();

        this.boat = switch (cartType) {
            case 2 -> (Boat) player.getWorld().spawnEntity(loc, EntityType.BIRCH_BOAT);
            case 3 -> (Boat) player.getWorld().spawnEntity(loc, EntityType.ACACIA_BOAT);
            case 4 -> (Boat) player.getWorld().spawnEntity(loc, EntityType.OAK_BOAT);
            default -> (Boat) player.getWorld().spawnEntity(loc, EntityType.SPRUCE_BOAT);
        };

        // Make the player ride the boat
        boat.addPassenger(player);
        // start boat in frozen state
        frozenBoat = true;
        startBoatControl(RP, boat);
    }

    private void startBoatControl(RacePlayer RP, Boat boat) {
        Player player = RP.GetPlayer();
        // Add this map to track when the boat is climbing
        final Map<Player, Boolean> isClimbing = new HashMap<>();

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPlayer() != player) return;

                PacketContainer packet = event.getPacket();
                StructureModifier<Boolean> booleans = packet.getStructures().read(0).getBooleans();

                if (booleans == null) return;

                boolean forward = booleans.read(0);
                boolean backward = booleans.read(1);

                if (forward) {
                    movementState.put(player, 1); // Mark as moving forward

                    // More precise obstacle detection
                    Vector direction = player.getLocation().getDirection().normalize();
                    Location boatLoc = boat.getLocation();

                    // Check if there's a block at the front lower part of the boat
                    Location frontLoc = boatLoc.clone().add(direction.clone().multiply(1.2));
                    frontLoc.setY(boatLoc.getY()); // Check at boat level

                    // Check if there's a block in front that the boat needs to climb
                    boolean needsToClimb = false;

                    // Check if there's a solid block in front
                    if (!frontLoc.getBlock().isPassable()) {
                        // Check if there's space above for the boat to climb to
                        Location aboveFrontLoc = frontLoc.clone().add(0, 1, 0);
                        if (aboveFrontLoc.getBlock().isPassable()) {
                            needsToClimb = true;
                        }
                    }

                    // Also check specifically for stairs and slabs
                    Material frontBlockType = frontLoc.getBlock().getType();
                    if (frontBlockType.name().contains("STAIRS") ||
                            frontBlockType.name().contains("SLAB") ||
                            frontBlockType.name().contains("STEP")) {
                        // If it's a spruce slab, we don't want to climb
                        needsToClimb = !frontBlockType.name().contains("SPRUCE_SLAB");
                    }
                    if (frontBlockType.name().contains("WOOL")) {
                        needsToClimb = false;
                    }

                    isClimbing.put(player, needsToClimb);

                } else if (backward) {
                    movementState.put(player, -1); // Backward movement
                    isClimbing.put(player, false);
                } else {
                    movementState.put(player, 0); // No movement
                    isClimbing.put(player, false);
                }
            }
        });

       task = new BukkitRunnable() {
            @Override
            public void run() {
                // if something we don't like happens, just send the player back to the spawn area and remove them from the race.
                if (boat.isDead() || !player.isInsideVehicle() || !(player.getVehicle() instanceof Boat)) {
                    this.cancel();
                    movementState.remove(player);
                    isClimbing.remove(player);
                    boat.remove();
                    // send back to hub area
                    player.teleport(new Location(player.getWorld(), -24, -60, 574));
                    // remove from race complete
                    return;
                }

                if (frozenBoat) {
                    // If the boat is frozen, set boat velocity to zero
                    boat.setVelocity(new Vector(0, 0, 0));
                    //player.teleport(new Location(player.getWorld(), RP.StartX, RP.StartY, RP.StartZ));
                    return;
                }

                int state = movementState.getOrDefault(player, 0);
                boolean climbing = isClimbing.getOrDefault(player, false);

                // Apply gravity if boat is in the air and not actively climbing
                if (!boat.isOnGround() && !climbing) {
                    Vector currentVelocity = boat.getVelocity();
                    // Apply stronger downward acceleration (more negative Y value = stronger gravity)
                    currentVelocity.setY(currentVelocity.getY() - 0.5); // Increase this value for stronger gravity

                    boat.setVelocity(currentVelocity);
                }

                if (state == 1) { // Forward
                    Vector direction = boat.getLocation().getDirection().normalize();

                    if (climbing) {
                        // Apply climbing velocity only if boat isn't already moving too fast up
                        if (boat.isOnGround()) {
                            if (boat.getVelocity().getY() < 0.7) {
                                boat.setVelocity(direction.multiply(1.0).add(new Vector(0, 0.7, 0))); // Lower speed and lower jump for smoother climbing
                            }
                        }
                    } else {
                        // Normal forward movement
                        boat.setVelocity(direction.multiply(1.5).add(new Vector(0, -0.5, 0))); // Adjust forward speed if needed
                    }
                } else if (state == -1) { // Backward
                    Vector direction = boat.getLocation().getDirection().normalize();
                    direction.setZ(-direction.getZ());
                    direction.setX(-direction.getX());
                    boat.setVelocity(direction.multiply(1.1)); // Adjust backward speed if needed
                } else {
                    // Gradually slow down when not moving
                    boat.setVelocity(boat.getVelocity().multiply(0.8));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Runs every tick (20 ticks per second)
    }

    public void setFrozenBoat(boolean frozenBoat) {
        this.frozenBoat = frozenBoat;
    }

    public void stopBoatControl() {
        if (task != null) {
            task.cancel();
        }
        boat.remove();
    }

}