package net.tfminecraft.gunsandgadgets.guns.ammunition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Ammunition {

    private final String key;
    private final String input; // modeled.(...) string
    private final int amount;
    private final Map<String, Integer> stats;

    public Ammunition(String key, ConfigurationSection config) {
        this.key = key;

        this.input = config.getString("input", "v.iron_nugget");

        this.amount = config.getInt("amount", 1);

        // Parse stats
        this.stats = new HashMap<>();
        List<String> statList = config.getStringList("stats");
        for (String s : statList) {
            String[] split = s.split(" ");
            if (split.length == 2) {
                try {
                    stats.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid stat number for ammunition " + key + ": " + s);
                }
            }
        }
    }

    // Getters
    public String getKey() { return key; }
    public String getInput() { return input; }
    public int getAmount() { return amount; }
    public Map<String, Integer> getStats() { return stats; }
}
