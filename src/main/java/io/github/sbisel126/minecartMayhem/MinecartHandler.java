package io.github.sbisel126.minecartMayhem;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class MinecartHandler {
    private JavaPlugin plugin;
    private ProtocolManager protocolManager;
    private final Map<Player, Integer> movementState = new HashMap<>();

    public MinecartHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void PutPlayerInCart(Player player, boolean cartType) {
        Location loc = player.getLocation();
        Boat boat;
        // Create a boat at the player's location
        if (cartType) {
            boat = (Boat) player.getWorld().spawnEntity(loc, EntityType.BIRCH_BOAT);
        } else {
            boat = (Boat) player.getWorld().spawnEntity(loc, EntityType.ACACIA_BOAT);
        }

        // Make the player ride the boat
        boat.addPassenger(player);

        startBoatControl(player, boat);
    }


    private void startBoatControl(Player player, Boat boat) {
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
                        needsToClimb = true;
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

        new BukkitRunnable() {
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

                int state = movementState.getOrDefault(player, 0);
                boolean climbing = isClimbing.getOrDefault(player, false);

                // Apply gravity if boat is in the air and not actively climbing
                if (!boat.isOnGround() && !climbing) {
                    Vector currentVelocity = boat.getVelocity();
                    // Apply stronger downward acceleration (more negative Y value = stronger gravity)
                    currentVelocity.setY(currentVelocity.getY() - 0.15); // Increase this value for stronger gravity

                    // Set a terminal velocity to prevent falling too fast
                    if (currentVelocity.getY() < -1.0) {
                        currentVelocity.setY(-1.0);
                    }

                    boat.setVelocity(currentVelocity);
                }

                if (state == 1) { // Forward
                    Vector direction = boat.getLocation().getDirection().normalize();

                    if (climbing) {
                        // Apply climbing velocity only if boat isn't already moving too fast up
                        if (boat.getVelocity().getY() < 0.5) {
                            boat.setVelocity(direction.multiply(1.0).setY(0.4)); // Lower speed and lower jump for smoother climbing
                        }
                    } else {
                        // Normal forward movement
                        boat.setVelocity(direction.multiply(1.5));
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
    }