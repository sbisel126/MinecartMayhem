package io.github.sbisel126.minecartMayhem;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;

public class Race {
    private Dictionary<String, Integer> results;
    private ArrayList<String> players;
    private Date startTime;
    private String trackID;
    private Integer difficulty;
    private Integer baseScore;

    public Race(String trackID, int difficultyMultiplier, int baseMapScore, ArrayList<String> players){
        this.players = players;
        this.trackID = trackID;
        this.difficulty = difficultyMultiplier;
        this.baseScore = baseMapScore;
    }

    public Dictionary<String, Integer> getResults(){
        return this.results;
    }

    public void startRace(){
        for (String player : this.players) {
            this.results.put(player, 0);
        }
        this.startTime = new Date();

    }

    public void calculatePlayerScore(String player){
        assert this.players.contains(player);
        // Calculates the final score based on time and difficulty modifiers.
        double totalScore = this.baseScore.doubleValue();
        double diff = 0.0;

            // If a run beats the par time for the map, add an extra score multiplier.

        totalScore *= diff;
        totalScore *= this.difficulty.doubleValue();

        this.results.put(player, ((int)(totalScore)));
    }
}
