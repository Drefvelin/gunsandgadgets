package net.tfminecraft.gunsandgadgets.manager;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinData;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinState;
import net.tfminecraft.gunsandgadgets.guns.stats.StatCalculator;
import net.tfminecraft.gunsandgadgets.loader.SkinLoader;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GunManager implements Listener {

    private final NamespacedKey skinKey = new NamespacedKey(GunsAndGadgets.getInstance(), "skin_id");

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String skinId = meta.getPersistentDataContainer().get(skinKey, PersistentDataType.STRING);
        if (skinId == null) return;

        SkinData skin = SkinLoader.get().get(skinId);
        if (skin == null) return;

        // Look up reload stat from PDC (default 0 if missing)
        int reloadStat = meta.getPersistentDataContainer()
            .getOrDefault(new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_reload"),
                        PersistentDataType.INTEGER, 0);

        int reloadTicks = StatCalculator.calculateReloadTicks(reloadStat);

        Player player = event.getPlayer();
        player.sendMessage("§7Reload time: §e" + (reloadTicks / 20.0) + "s");

        // Visual: swap to reload model
        ItemStack reloading = skin.parseModel(SkinState.RELOAD);
        meta.setCustomModelData(reloading.getItemMeta().getCustomModelData());
        item.setItemMeta(meta);
        item.setType(reloading.getType());

        player.getInventory().setItemInMainHand(item);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1.2f);
    }
}

