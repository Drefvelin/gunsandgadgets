package net.tfminecraft.gunsandgadgets.guns.parts;

import org.bukkit.configuration.ConfigurationSection;

public class PartData {
    private String id;
    private int slot;
    
    public PartData(String key, ConfigurationSection config) {
        id = key;
        slot = config.getInt("slot", 0);
    }

    public String getId() { return id; }
    public int getSlot() { return slot; }
}
