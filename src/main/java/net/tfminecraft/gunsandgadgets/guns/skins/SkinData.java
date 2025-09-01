package net.tfminecraft.gunsandgadgets.guns.skins;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkinData {

    private final String id;
    private final String carry;
    private final String reload;
    private final String aim;
    private final List<String> types;
    private final List<String> options;

    public SkinData(String key, ConfigurationSection config) {
        this.id = key;

        this.carry = config.getString("carry", "");
        this.reload = config.getString("reload", "");
        this.aim = config.getString("aim", "");

        this.types = new ArrayList<>(config.getStringList("types"));
        this.options = config.contains("options") 
                ? new ArrayList<>(config.getStringList("options")) 
                : new ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCarry() {
        return carry;
    }

    public String getReload() {
        return reload;
    }

    public String getAim() {
        return aim;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getOptions() {
        return options;
    }

    public boolean hasOption(String option) {
        return options.contains(option.toLowerCase());
    }

    public ItemStack parseModel(SkinState state) {
        String path = null;
        switch(state) {
            case AIM:
                path = aim;
                break;
            case CARRY:
                path = carry;
                break;
            case RELOAD:
                path = reload;
                break;
            default:
                break;
        }
        if (path == null || path.isEmpty()) return new ItemStack(Material.BARRIER);

        String[] split = path.split("\\.");
        Material mat = Material.matchMaterial(split[0].toUpperCase());
        if (mat == null) mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat);
        if (split.length > 1) {
            try {
                int data = Integer.parseInt(split[1]);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(data);
                    item.setItemMeta(meta);
                }
            } catch (NumberFormatException ignored) {}
        }
        return item;
    }
}
