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

    public void PutPlayerInCart(Player player, boolean cartType){
        Location loc = player.getLocation();
        Boat boat;
        // Create a boat at the player's location
        if (cartType){
            boat = (Boat) player.getWorld().spawnEntity(loc, EntityType.BIRCH_BOAT);
        } else {
            boat = (Boat) player.getWorld().spawnEntity(loc, EntityType.ACACIA_BOAT);
        }

        // Make the player ride the boat
        boat.addPassenger(player);

        startBoatControl(player, boat);
    }


    private void startBoatControl(Player player, Boat boat) {
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
                    movementState.put(player, 1); // Forward movement
                    System.out.println("Boosting forward");
                } else if (backward) {
                    movementState.put(player, -1); // Backward movement
                    System.out.println("Boosting backward");
                } else {
                    movementState.put(player, 0); // No movement
                }
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                if (boat.isDead() || !player.isInsideVehicle() || !(player.getVehicle() instanceof Boat)) {
                    this.cancel();
                    movementState.remove(player);
                    return;
                }

                int state = movementState.getOrDefault(player, 0);

                if (state == 1) { // Forward
                    Vector direction = boat.getLocation().getDirection().normalize();
                    boat.setVelocity(direction.multiply(1.5));
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
