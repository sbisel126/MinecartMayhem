package io.github.sbisel126.minecartMayhem.Race;

import io.github.sbisel126.minecartMayhem.DatabaseHandler;
import io.github.sbisel126.minecartMayhem.MinecartHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

// represents Player information within a race
public class RacePlayer {
    int CartColor;

    Player player;
    String Username;
    public MinecartHandler minecart;
    Boolean isRacing = false;
    int currentLap = 0;
    Integer finishTime;

    // list of checkpoints crossed by player
    // this is used to check if the player has crossed all checkpoints
    List<Integer> CheckpointsCrossed = new ArrayList<>();

    // starting position of the player
    public Integer StartX;
    public Integer StartY;
    public Integer StartZ;

    public RacePlayer(Player player, DatabaseHandler db) {
        // get player's preferred cart color
        this.CartColor = db.GetPlayerBoatColor(player);
        this.Username = player.getName();
        this.player = player;
    }

    public Integer getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Integer finishTime) {
        this.finishTime = finishTime;
    }

    public void setMinecart(MinecartHandler minecart) {
        this.minecart = minecart;
    }

    public String GetUsername() {
        return Username;
    }

    public void SetStartPos(Integer x, Integer y, Integer z) {
        this.StartX = x;
        this.StartY = y;
        this.StartZ = z;
    }
    public Player GetPlayer() {
        return player;
    }

    // allows us to check if player has finished race yet
    // called at start and end of race
    public void setRacing(Boolean isRacing) {
        this.isRacing = isRacing;
    }

    public Boolean isRacing() {
        return isRacing;
    }

    public void addCheckpointCrossed(int checkpointID) {
        if (CheckpointsCrossed.contains(checkpointID)) {
            player.showTitle(Title.title(Component.text("Wrong way!"), Component.text("")));
            return; // already crossed this checkpoint
        }
        CheckpointsCrossed.add(checkpointID);
    }
}
