package net.tfminecraft.gunsandgadgets.manager.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.tfminecraft.gunsandgadgets.guns.GunType;

public class TypeSelectionManager {
    private static final Map<UUID, GunType> selectedTypes = new HashMap<>();

    public static GunType getSelectedType(Player player) {
        return selectedTypes.getOrDefault(player.getUniqueId(), GunType.RIFLE);
    }

    public static void setSelectedType(Player player, GunType type) {
        selectedTypes.put(player.getUniqueId(), type);
    }
}

