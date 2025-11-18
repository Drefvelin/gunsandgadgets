package net.tfminecraft.gunsandgadgets.attributes;

import org.bukkit.configuration.ConfigurationSection;

public class AttributeData {
    private String attribute;
    private double accuracy;
    private double reload;

    public AttributeData(String key, ConfigurationSection config) {
        attribute = key;
        accuracy = config.getDouble("accuracy-per-level", 0);
        reload = config.getDouble("reload-reduction-per-level", 0);
    }

    public String getAttribute() { return attribute; }
    public double getAccuracy() { return accuracy; }
    public double getReload() { return reload; }
}
