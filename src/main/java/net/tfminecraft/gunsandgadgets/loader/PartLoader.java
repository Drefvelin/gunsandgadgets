package net.tfminecraft.gunsandgadgets.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.gunsandgadgets.guns.parts.GunPart;

public class PartLoader implements LoaderInterface{
	static HashMap<String, GunPart> oList = new HashMap<>();
	private static List<GunPart> ordered = new ArrayList<>();
	public static void clear() {
		oList.clear();
	}
	public static List<GunPart> getOrdered() {
		return ordered;
	}
	public static HashMap<String, GunPart> get() {
		return oList;
	}
	public static GunPart getByString(String id) {
		if(oList.containsKey(id)) return oList.get(id);
		return null;
	}
	public void load(File configFile) {
		clear();
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			GunPart o = new GunPart(key, config.getConfigurationSection(key));
			oList.put(key, o);
			ordered.add(o);
		}
	}
}
