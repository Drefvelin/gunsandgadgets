package net.tfminecraft.gunsandgadgets.manager;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.guns.ammunition.Ammunition;
import net.tfminecraft.gunsandgadgets.guns.parts.GunPart;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinData;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinState;
import net.tfminecraft.gunsandgadgets.guns.stats.StatCalculator;
import net.tfminecraft.gunsandgadgets.loader.AmmunitionLoader;
import net.tfminecraft.gunsandgadgets.loader.SkinLoader;
import net.tfminecraft.gunsandgadgets.shooter.ProjectileShooter;
import net.tfminecraft.gunsandgadgets.util.Caliber;
import net.tfminecraft.gunsandgadgets.util.SoundPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class GunManager implements Listener {

    private final NamespacedKey skinKey = new NamespacedKey(GunsAndGadgets.getInstance(), "skin_id");
    private final NamespacedKey gunKey = new NamespacedKey(GunsAndGadgets.getInstance(), "gun_id");

    private final NamespacedKey bulletsKey = new NamespacedKey(GunsAndGadgets.getInstance(), "bullets_loaded");
    private final NamespacedKey capacityKey = new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_capacity");

    private final NamespacedKey lastFireKey = new NamespacedKey(GunsAndGadgets.getInstance(), "last_fire");
    private final NamespacedKey fireRateKey = new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_fire_rate");

    // New PDC keys
    private final NamespacedKey reloadAmmoKey = new NamespacedKey(GunsAndGadgets.getInstance(), "reload_ammo");
    private final NamespacedKey reloadAmountKey = new NamespacedKey(GunsAndGadgets.getInstance(), "reload_amount");
    private final NamespacedKey loadedAmmoKey = new NamespacedKey(GunsAndGadgets.getInstance(), "ammo_loaded");

    private final Map<UUID, Boolean> reloading = new HashMap<>();

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if(item == null) return;
        if (!item.hasItemMeta()) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        ItemMeta meta = item.getItemMeta();
        String skinId = meta.getPersistentDataContainer().get(skinKey, PersistentDataType.STRING);
        if (skinId == null) {
            reloading.remove(id);
            return;
        }
        event.setCancelled(true);
        // ✅ Block duplicate reloads
        if (reloading.getOrDefault(id, false)) {
            return;
        }
        reloading.put(id, true);

        SkinData skin = SkinLoader.get().get(skinId);
        if (skin == null) {
            reloading.remove(id);
            return;
        }

        int bullets = meta.getPersistentDataContainer()
            .getOrDefault(bulletsKey, PersistentDataType.INTEGER, 0);
        if (bullets > 0) {
            long now = System.currentTimeMillis();
            int fireRateStat = meta.getPersistentDataContainer()
                    .getOrDefault(fireRateKey, PersistentDataType.INTEGER, 0);

            // Convert fireRateStat to a cooldown in ms
            long cooldown = (long) (StatCalculator.calculateFireRate(fireRateStat) * 1000);

            long lastFired = meta.getPersistentDataContainer()
                    .getOrDefault(lastFireKey, PersistentDataType.LONG, 0L);

            if (now - lastFired < cooldown) {
                // Too soon to fire again
                reloading.remove(id);
                return;
            }

            // Update last fire time
            meta.getPersistentDataContainer().set(lastFireKey, PersistentDataType.LONG, now);
            item.setItemMeta(meta);

            // Consume bullet & shoot
            bullets--;
            meta.getPersistentDataContainer().set(bulletsKey, PersistentDataType.INTEGER, bullets);
            item.setItemMeta(meta);
            String ammoId = meta.getPersistentDataContainer().get(loadedAmmoKey, PersistentDataType.STRING);
            Ammunition ammo = AmmunitionLoader.getByString(ammoId);
            if (ammo != null) {
                ProjectileShooter.shoot(player, item, ammo); // pass ammo in
            }

            if (bullets <= 0) {
                // Show carry model when empty
                meta.getPersistentDataContainer().remove(loadedAmmoKey);
                applyModel(item, skin, SkinState.CARRY);

                // Clear arrow if crossbow
                if (item.getType() == Material.CROSSBOW) {
                    CrossbowMeta cbMeta = (CrossbowMeta) item.getItemMeta();
                    cbMeta.setChargedProjectiles(new ArrayList<>());
                    item.setItemMeta(cbMeta);
                }
            } else {
                if (item.getType() == Material.CROSSBOW) {
                    chargeCrossbow(item, ammoId, bullets);
                }
            }
            player.getInventory().setItemInMainHand(item);
            reloading.remove(id);
            return;
        }

        int capacity = meta.getPersistentDataContainer()
        .getOrDefault(capacityKey, PersistentDataType.INTEGER, 1);

        // 🔫 Try to consume ammo before reload
        Collection<Ammunition> calibers = Caliber.get(item); // your helper from before
        String taken = takeAmmo(player, capacity, calibers);

        if (taken.equals("none")) {
            reloading.remove(id);
            return;
        }

        // Parse ammo info
        String[] split = taken.split("\\.");
        String ammoId = split[0];
        int takenAmount = Integer.parseInt(split[1]);

        // Save ammo info in PDC (for refund or finalize)
        meta.getPersistentDataContainer().set(reloadAmmoKey, PersistentDataType.STRING, ammoId);
        meta.getPersistentDataContainer().set(reloadAmountKey, PersistentDataType.INTEGER, takenAmount);
        item.setItemMeta(meta);

        // Switch to reload model
        player.getInventory().setItemInMainHand(applyModel(item, skin, SkinState.RELOAD));


        int reloadStat = meta.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_reload"),
                        PersistentDataType.INTEGER, 0);
        int accuracyStat = meta.getPersistentDataContainer()
                .getOrDefault(new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_accuracy"),
                        PersistentDataType.INTEGER, 0);

        int reloadTicks = StatCalculator.calculateReloadTicks(reloadStat);

        // Switch to reload model
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(applyModel(item, skin, SkinState.RELOAD));
        String reloadSounds = meta.getPersistentDataContainer().get(
            new NamespacedKey(GunsAndGadgets.getInstance(), "reload_sounds"),
            PersistentDataType.STRING
        );
        SoundPlayer.playSounds(player.getLocation(), reloadSounds, false, 1f);


        new BukkitRunnable() {
            int tick = 0;
            Location lastLoc = player.getLocation().clone();

            @Override
            public void run() {
                ItemStack current = player.getInventory().getItemInMainHand();
                if (!reloading.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                double progress = (double) tick / reloadTicks;
                String bar = makeProgressBar(progress, 10, "§7", "§f");
                player.sendTitle("", bar, 0, 5, 0);
                // 📍 Check movement since last tick
                Location now = player.getLocation();
                double moved = now.distanceSquared(lastLoc);
                lastLoc = now.clone();

                // If player moved, chance to skip tick
                if (moved > 0.0025) { // ~0.05 blocks², tweak sensitivity
                    double distance = Math.sqrt(moved);
                    double chance = Math.min(1.0, distance * 2.0); 
                    // e.g. move 0.5 blocks → 100% chance to skip tick
                    if (Math.random() < chance) {
                        return;
                    }
                }
                tick++;

                if (tick >= reloadTicks) {
                    // ✅ Finish reload
                    applyModel(current, skin, SkinState.AIM);
                    ItemMeta reloadMeta = current.getItemMeta();


                    // Load exactly how many bullets were consumed
                    int loaded = reloadMeta.getPersistentDataContainer()
                            .getOrDefault(reloadAmountKey, PersistentDataType.INTEGER, 0);
                    String loadedAmmo = reloadMeta.getPersistentDataContainer()
                            .get(reloadAmmoKey, PersistentDataType.STRING);
                    // Save bullets + ammo type
                    reloadMeta.getPersistentDataContainer().set(bulletsKey, PersistentDataType.INTEGER, loaded);
                    if (loadedAmmo != null) {
                        reloadMeta.getPersistentDataContainer().set(loadedAmmoKey, PersistentDataType.STRING, loadedAmmo);
                    }

                    // Clear temporary reload info
                    reloadMeta.getPersistentDataContainer().remove(reloadAmmoKey);
                    reloadMeta.getPersistentDataContainer().remove(reloadAmountKey);


                    current.setItemMeta(reloadMeta);
                    player.getInventory().setItemInMainHand(current);

                    if (player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW) {
                        chargeCrossbow(player.getInventory().getItemInMainHand(), loadedAmmo, loaded);
                    }


                    double spread = StatCalculator.calculateAccuracy(accuracyStat);
                    player.sendMessage("§7Accuracy: §e" + String.format("%.2f° spread", spread));

                    int fireRateStat = meta.getPersistentDataContainer()
                        .getOrDefault(fireRateKey, PersistentDataType.INTEGER, 0);

                    // Convert fireRateStat to a cooldown in ms
                    long cooldown = (long) (StatCalculator.calculateFireRate(fireRateStat) * 1000);
                    player.sendMessage("§7Fire Rate: §e" + cooldown);

                    //player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
                    reloading.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(GunsAndGadgets.getInstance(), 0L, 1L);

    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // If this player is currently reloading, cancel it
        if (reloading.containsKey(id)) {
            reloading.remove(id);
            ItemStack item = player.getInventory().getItem(event.getPreviousSlot());
            if (item != null && item.hasItemMeta()) {
                String skinId = item.getItemMeta().getPersistentDataContainer()
                        .get(skinKey, PersistentDataType.STRING);
                if (skinId != null) {
                    SkinData skin = SkinLoader.get().get(skinId);
                    if (skin != null) {
                        applyModel(item, skin, SkinState.CARRY);
                        player.getInventory().setItem(event.getPreviousSlot(), item);
                        ItemMeta cancelMeta = item.getItemMeta();
                        PersistentDataContainer pdc = cancelMeta.getPersistentDataContainer();

                        String ammoId = pdc.get(reloadAmmoKey, PersistentDataType.STRING);
                        int amount = pdc.getOrDefault(reloadAmountKey, PersistentDataType.INTEGER, 0);

                        if (ammoId != null && amount > 0) {
                            // Refund ammo
                            Ammunition ammo = AmmunitionLoader.getByString(ammoId);
                            if(ammo != null) {
                                ItemStack refund = TLibs.getItemAPI().getCreator().getItemFromPath(ammo.getInput());
                                refund.setAmount(amount);
                                player.getInventory().addItem(refund);
                            }
                        }

                        // Clean up
                        pdc.remove(reloadAmmoKey);
                        pdc.remove(reloadAmountKey);
                        item.setItemMeta(cancelMeta);
                    }
                }
            }
        }
    }


    private ItemStack applyModel(ItemStack i, SkinData data, SkinState state) {
        ItemStack skin = data.parseModel(state);
        ItemMeta m = i.getItemMeta();
        m.setCustomModelData(skin.getItemMeta().getCustomModelData());
        i.setItemMeta(m);
        i.setType(skin.getType());
        return i;
    }


    private void chargeCrossbow(ItemStack crossbow, String ammoId, int amount) {
        if (crossbow == null || crossbow.getType() != Material.CROSSBOW) return;
        Ammunition ammo = AmmunitionLoader.getByString(ammoId);
        ItemMeta meta = crossbow.getItemMeta();
        if (!(meta instanceof CrossbowMeta cbMeta)) return;
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        if(cbMeta.hasChargedProjectiles()) cbMeta.setChargedProjectiles(new ArrayList<>());
        if(ammo != null) {
            ItemMeta arrowMeta = arrow.getItemMeta();
            arrowMeta.setDisplayName(StringFormatter.getName(TLibs.getItemAPI().getCreator().getItemFromPath(ammo.getInput()))+" §7x"+amount);
            arrow.setItemMeta(arrowMeta);
        }
        cbMeta.addChargedProjectile(arrow);
        crossbow.setItemMeta(cbMeta);
    }


    /** Progress bar helper */
    private String makeProgressBar(double progress, int bars, String filled, String empty) {
        int filledBars = (int) Math.round(progress * bars);
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filledBars ? filled + "|" : empty + "|");
        }
        sb.append("");
        return sb.toString();
    }

    /**
     * Try to take ammo from the player's inventory.
     *
     * @param p        the player
     * @param amount   how many units required
     * @param calibers list of valid calibers for this gun
     * @return the ammo ID + how many removed (e.g. "ironshot.1"), or "none" if not enough
     */
    private String takeAmmo(Player p, int amount, Collection<Ammunition> calibers) {
        for (Ammunition ammo : calibers) {
            int totalFound = 0;

            // Count how many of this ammo we have
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null) continue;
                if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, ammo.getInput())) {
                    totalFound += item.getAmount();
                }
            }

            if (totalFound > 0) {
                // Take min(capacity, found)
                int toTake = Math.min(amount, totalFound);
                removeItems(p, ammo.getInput(), toTake);
                return ammo.getKey() + "." + toTake;
            }
        }

        // ❌ No ammo found in any caliber
        return "none";
    }


    /**
     * Remove a certain number of items of a specific type from a player's inventory.
     */
    private void removeItems(Player p, String itemPath, int amount) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, itemPath)) {
                int remove = Math.min(item.getAmount(), amount);
                item.setAmount(item.getAmount() - remove);
                amount -= remove;
                if (amount <= 0) break;
            }
        }
        p.updateInventory();
    }

}

