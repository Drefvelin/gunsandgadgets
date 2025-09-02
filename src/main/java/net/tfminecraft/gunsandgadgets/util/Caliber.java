package net.tfminecraft.gunsandgadgets.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.guns.ammunition.Ammunition;
import net.tfminecraft.gunsandgadgets.loader.AmmunitionLoader;

public class Caliber {
    public static List<Ammunition> get(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.emptyList();

        ItemMeta meta = item.getItemMeta();
        String calibersStr = meta.getPersistentDataContainer().get(
            new NamespacedKey(GunsAndGadgets.getInstance(), "calibers"),
            PersistentDataType.STRING
        );

        if (calibersStr == null || calibersStr.isBlank()) return Collections.emptyList();

        return Arrays.stream(calibersStr.split(";"))
                .map(AmmunitionLoader::getByString) // convert key → Ammunition
                .filter(Objects::nonNull)           // skip invalid ones
                .toList();
    }
}
