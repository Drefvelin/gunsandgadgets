package net.tfminecraft.gunsandgadgets.manager.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PartSelectionHolder implements InventoryHolder {
    private final String partId; // e.g. "barrel", "loader", "stock" (your PartData id)

    public PartSelectionHolder(String partId) {
        this.partId = partId;
    }

    public String getPartId() {
        return partId;
    }

    @Override public Inventory getInventory() { return null; }
}
