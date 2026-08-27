package net.tfminecraft.gunsandgadgets.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.utils.GunStatRefresher;

public class GunRefreshListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Bukkit.getScheduler().runTask(GunsAndGadgets.getInstance(), () -> {
            ItemStack dropped = event.getItemDrop().getItemStack();
            if (!GunStatRefresher.isManaged(dropped)) {
                return;
            }
            GunStatRefresher.RefreshResult result = GunStatRefresher.refreshIfOutdated(dropped);
            if (!result.isChanged()) {
                return;
            }
            event.getItemDrop().setItemStack(result.getItem());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        int slot = event.getSlot();
        Bukkit.getScheduler().runTask(GunsAndGadgets.getInstance(), () -> {
            if (clicked != null) {
                tryRefresh(clicked.getItem(slot), item -> clicked.setItem(slot, item));
            }
            tryRefresh(player.getItemOnCursor(), player::setItemOnCursor);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHotbarSelect(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int slot = event.getNewSlot();
        Bukkit.getScheduler().runTask(GunsAndGadgets.getInstance(), () -> {
            ItemStack held = player.getInventory().getItem(slot);
            tryRefresh(held, item -> player.getInventory().setItem(slot, item));
        });
    }

    private void tryRefresh(ItemStack item, ItemConsumer writer) {
        if (item == null || item.getType().isAir() || !GunStatRefresher.isManaged(item)) {
            return;
        }
        GunStatRefresher.RefreshResult result = GunStatRefresher.refreshIfOutdated(item);
        if (!result.isChanged()) {
            return;
        }
        writer.accept(result.getItem());
    }

    @FunctionalInterface
    private interface ItemConsumer {
        void accept(ItemStack item);
    }
}
