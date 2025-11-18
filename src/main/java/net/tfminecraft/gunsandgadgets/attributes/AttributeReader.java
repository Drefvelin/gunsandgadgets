package net.tfminecraft.gunsandgadgets.attributes;

import org.bukkit.entity.Player;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes.AttributeInstance;
import net.tfminecraft.gunsandgadgets.cache.Cache;

public class AttributeReader {
    
    public static double getAccuracyMultFromAttributes(Player p) {
        PlayerData pd = PlayerData.get(p.getUniqueId());
        double total = 0;
        for(AttributeData data : Cache.attributes) {
            AttributeInstance a = pd.getAttributes().getInstance(data.getAttribute());
            if(a == null) continue;
            total += a.getTotal()*data.getAccuracy();
        }
        return 1-total/100.0;
    }

    public static double getReloadReductionMultFromAttributes(Player p) {
        PlayerData pd = PlayerData.get(p.getUniqueId());
        double total = 0;
        for(AttributeData data : Cache.attributes) {
            AttributeInstance a = pd.getAttributes().getInstance(data.getAttribute());
            if(a == null) continue;
            total += a.getTotal()*data.getReload();
        }
        return 1-total/100.0;
    }
}
