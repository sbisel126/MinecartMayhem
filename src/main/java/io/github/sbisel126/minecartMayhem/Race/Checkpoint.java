package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.MinecartMayhem;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Checkpoint {
    MinecartMayhem plugin;
    // this class defines the region of blocks for a checkpoint.
    // a checkpoint is a line of blocks that the player must cross to progress the race
    // the checkpoint is defined by two points in 3D space
    int x1, y1, z1;
    int x2, y2, z2;

    // we use this ID to identify if the checkpoint is the finish line or not mainly
    // we reserve id 0 for the start/finish line
    int checkpointID;

    // between these two coordinates, it is presumed there will be a line of blocks
    // the following list contains the blocks that are part of the checkpoint
    // this is a list of blocks that are part of the checkpoint

    List<Block> blocks = new ArrayList<>();

    public Checkpoint(MinecartMayhem plugin, int checkpointID, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.plugin = plugin;

        // define the checkpoint region
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.checkpointID = checkpointID;
        // generate the coordinate region between the two points
        GenerateBlocks();
    }

    // here we generate the blocks that are part of the checkpoint
    // yes this looks crazy, but it works. please don't question it.
    public void GenerateBlocks() {
        // this method generates the blocks that are part of the checkpoint
        // it is assumed that the blocks are in a straight line between the two points
        // this is a simple implementation and can be improved later
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    blocks.add(plugin.getServer().getWorlds().getFirst().getBlockAt(x, y, z));
                }
            }
        }
    }

    // we provide a method to check if a player is in the checkpoint
    public Boolean CheckPlayerInCheckpoint(int x, int y, int z) {
        // this method checks if a player is in the checkpoint
        for (Block block : blocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    public int getCheckpointID() {
        return checkpointID;
    }
}
