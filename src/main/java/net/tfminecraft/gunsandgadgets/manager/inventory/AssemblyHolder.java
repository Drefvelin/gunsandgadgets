package net.tfminecraft.gunsandgadgets.manager.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AssemblyHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null; // we don’t need this, since Bukkit manages it
    }
}
