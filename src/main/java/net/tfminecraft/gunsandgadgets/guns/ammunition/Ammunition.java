package net.tfminecraft.gunsandgadgets.guns.ammunition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Ammunition {

    private final String key;
    private final String input; // modeled.(...) string
    private final int amount;
    private final Map<String, Integer> stats;

    private final EnumSet<AmmoOption> options = EnumSet.noneOf(AmmoOption.class);

    public Ammunition(String key, ConfigurationSection config) {
        this.key = key;
        this.input = config.getString("input", "v.iron_nugget");
        this.amount = config.getInt("amount", 1);

        // Parse stats
        this.stats = new HashMap<>();
        for (String s : config.getStringList("stats")) {
            String[] split = s.split(" ");
            if (split.length == 2) {
                try {
                    stats.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid stat number for ammunition " + key + ": " + s);
                }
            }
        }

        // Parse options
        for (String opt : config.getStringList("options")) {
            try {
                options.add(AmmoOption.valueOf(opt.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid ammo option for " + key + ": " + opt);
            }
        }
    }

    public boolean hasOption(AmmoOption option) {
        return options.contains(option);
    }

    // Getters
    public String getKey() { return key; }
    public String getInput() { return input; }
    public int getAmount() { return amount; }
    public Map<String, Integer> getStats() { return stats; }

    public enum AmmoOption {
        ROCKET, SMOKELESS, NO_LIGHT
    }
}
