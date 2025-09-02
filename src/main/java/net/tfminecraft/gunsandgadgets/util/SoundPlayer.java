package net.tfminecraft.gunsandgadgets.util;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * Utility for playing semicolon-separated custom sounds
 * stored on guns (reload, shoot, etc.).
 */
public class SoundPlayer {

    /**
     * Play all sounds from a semicolon-separated list at a location.
     *
     * @param loc   Location to play sounds at
     * @param list  Semicolon-separated sound keys (e.g. "minecraft:gun.shoot1;minecraft:gun.shoot2")
     * @param fallback If true, play a fallback vanilla sound when no custom sounds are found
     */
    public static void playSounds(Location loc, String list, boolean fallback, float volume) {
        if (loc == null) return;

        boolean playedAny = false;
        if (list != null && !list.isBlank()) {
            String[] groups = list.split(";"); // each group
            Random rand = new Random();

            for (String group : groups) {
                String[] keys = group.split(",");
                if (keys.length == 0) continue;

                // pick one at random
                String chosen = keys[rand.nextInt(keys.length)].trim();
                if (chosen.isEmpty()) continue;

                playedAny = true;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(loc.getWorld())) {
                        p.playSound(loc, chosen, SoundCategory.PLAYERS, 1f, 1f);
                    }
                }
            }
        }

        if (!playedAny && fallback) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld())) {
                    p.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volume, 1.2f);
                }
            }
        }
    }
}


