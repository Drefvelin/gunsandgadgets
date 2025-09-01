package net.tfminecraft.gunsandgadgets.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class CostFormatter {
    public static List<String> getCostsFormatted(Map<String, Integer> map) {
        List<String> result = new ArrayList<>();
        for (String path : new TreeSet<>(map.keySet())) {
            int amount = map.get(path);
            ItemStack i = TLibs.getItemAPI().getCreator().getItemFromPath(path);
            result.add("§7- §f"+StringFormatter.getName(i)+" §e"+amount);
        }
        return result;
    }
}
