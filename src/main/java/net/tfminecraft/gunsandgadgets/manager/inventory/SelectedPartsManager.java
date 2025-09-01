package net.tfminecraft.gunsandgadgets.manager.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class SelectedPartsManager {
    // UUID -> (partCategoryId -> partKey)
    private static final Map<UUID, Map<String, String>> selections = new HashMap<>();

    public static void clear(Player p) {
        selections.remove(p.getUniqueId());
    }

    public static void set(Player p, String partCategoryId, String partKey) {
        selections.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(partCategoryId, partKey);
    }

    public static String get(Player p, String partCategoryId) {
        Map<String, String> m = selections.get(p.getUniqueId());
        return m == null ? null : m.get(partCategoryId);
    }
}
