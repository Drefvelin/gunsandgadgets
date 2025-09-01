package net.tfminecraft.gunsandgadgets.loader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.gunsandgadgets.cache.Cache;


public class ConfigLoader {
    public void loadConfig(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Cache.outputSlot = config.getInt("output-slot", 15);
        Cache.outputItem = config.getString("output-item", "v.stick");
	}
}
