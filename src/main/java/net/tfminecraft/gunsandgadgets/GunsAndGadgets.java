package net.tfminecraft.gunsandgadgets;

import java.io.File;
import java.util.HashMap;

import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.gunsandgadgets.guns.skins.SkinData;
import net.tfminecraft.gunsandgadgets.loader.AmmunitionLoader;
import net.tfminecraft.gunsandgadgets.loader.ConfigLoader;
import net.tfminecraft.gunsandgadgets.loader.PartDataLoader;
import net.tfminecraft.gunsandgadgets.loader.PartLoader;
import net.tfminecraft.gunsandgadgets.loader.SkinLoader;
import net.tfminecraft.gunsandgadgets.manager.CraftingManager;
import net.tfminecraft.gunsandgadgets.manager.GunManager;
import net.tfminecraft.gunsandgadgets.manager.inventory.InventoryManager;

public class GunsAndGadgets extends JavaPlugin{
    private static GunsAndGadgets instance;
    private final ConfigLoader configLoader = new ConfigLoader();
    private final PartLoader partLoader = new PartLoader();
    private final PartDataLoader partDataLoader = new PartDataLoader();
    private final SkinLoader skinLoader = new SkinLoader();
    private final AmmunitionLoader ammunitionLoader = new AmmunitionLoader();

    private final CraftingManager craftingManager = new CraftingManager();
    private GunManager gunManager;

    @Override
    public void onEnable() {
        instance = this;
        gunManager = new GunManager();
        createConfigs();
        loadConfigs();
        registerListeners();

    }

    @Override
    public void onDisable() {
        
    }

    public void registerListeners() {
		getServer().getPluginManager().registerEvents(craftingManager, this);
        getServer().getPluginManager().registerEvents(new InventoryManager(), this);
        getServer().getPluginManager().registerEvents(gunManager, this);
	}

    public static GunsAndGadgets getInstance() {
        return instance;
    }

    public void createConfigs() {
		String[] files = {
				"config.yml",
                "part-types.yml",
                "parts.yml",
                "skins.yml",
                "ammunition.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}

    public void loadConfigs() {
		configLoader.loadConfig(new File(getDataFolder(), "config.yml"));
        ammunitionLoader.load(new File(getDataFolder(), "ammunition.yml"));
        partDataLoader.load(new File(getDataFolder(), "part-types.yml"));
        partLoader.load(new File(getDataFolder(), "parts.yml"));
        skinLoader.load(new File(getDataFolder(), "skins.yml"));
	}

    public GunManager getGunManager() { return gunManager; }

    public SkinData getSkin(String id) { return SkinLoader.getByString(id); }
}
