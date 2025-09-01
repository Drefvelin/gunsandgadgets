package net.tfminecraft.gunsandgadgets.manager.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.cache.Cache;
import net.tfminecraft.gunsandgadgets.guns.GunType;
import net.tfminecraft.gunsandgadgets.guns.parts.GunPart;
import net.tfminecraft.gunsandgadgets.guns.parts.PartData;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinData;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinResolver;
import net.tfminecraft.gunsandgadgets.guns.skins.SkinState;
import net.tfminecraft.gunsandgadgets.guns.stats.StatApplier;
import net.tfminecraft.gunsandgadgets.guns.stats.Stats;
import net.tfminecraft.gunsandgadgets.loader.PartDataLoader;
import net.tfminecraft.gunsandgadgets.loader.PartLoader;
import net.tfminecraft.gunsandgadgets.loader.SkinLoader;
import net.tfminecraft.gunsandgadgets.util.CostFormatter;
public class InventoryManager implements Listener {

    // ---------- OPEN ASSEMBLY (uses per-player selections if valid, else first) ----------
    public void openCraftingInventory(Player player) {
        Inventory inv = Bukkit.createInventory(new AssemblyHolder(), 27, "§6Gun Assembly");

        // background
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) { meta.setDisplayName("§o"); filler.setItemMeta(meta); }
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // type button
        GunType chosen = TypeSelectionManager.getSelectedType(player);
        inv.setItem(0, createWeaponTypeButton(chosen));

        Collection<GunPart> parts = new ArrayList<>();

        // parts per slot
        for (PartData partData : PartDataLoader.get().values()) {
            int slot = partData.getSlot();
            if (slot <= 0 || slot >= inv.getSize()) continue;

            GunPart part = getSelectedOrFirstPart(partData.getId(), chosen, player);
            if (part != null) {
                parts.add(part);
                inv.setItem(slot, createPartItem(part));
            }
        }

        inv.setItem(Cache.outputSlot, createOutputItem(chosen, parts, true));

        player.openInventory(inv);
    }

    // ---------- OPEN TYPE SELECTION (unchanged) ----------
    public void openTypeSelection(Player player) {
        Inventory typeInv = Bukkit.createInventory(new TypeSelectionHolder(), 9, "§6Select Gun Type");
        int slot = 0;
        for (GunType type : GunType.values()) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + type.getDisplayName());
                meta.setLore(List.of("§7Click to choose this type"));
                item.setItemMeta(meta);
            }
            typeInv.setItem(slot++, item);
        }
        player.openInventory(typeInv);
    }

    // ---------- NEW: OPEN PART SELECTION FOR A CATEGORY ----------
    public void openPartSelection(Player player, String partCategoryId) {
        GunType chosen = TypeSelectionManager.getSelectedType(player);

        // collect options matching this category + gun type
        List<GunPart> options = new ArrayList<>();
        for (GunPart part : PartLoader.getOrdered()) {
            if (!part.getPartType().getId().equalsIgnoreCase(partCategoryId)) continue;
            if (!part.getGunTypes().contains(chosen)) continue;
            options.add(part);
        }

        int size = Math.max(9, Math.min(54, ((options.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(new PartSelectionHolder(partCategoryId), size, "§6Select " + partCategoryId);

        for (GunPart part : options) {
            ItemStack option = createPartItem(part); // pretty preview
            // store the part key for retrieval on click (use localizedName for simplicity)
            ItemMeta meta = option.getItemMeta();
            if (meta != null) {
                meta.setLocalizedName(part.getId()); // safe hidden id
                option.setItemMeta(meta);
            }
            inv.addItem(option);
        }

        // optional: back button (last slot)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) { bm.setDisplayName("§eBack"); back.setItemMeta(bm); }
        inv.setItem(size - 1, back);

        player.openInventory(inv);
    }

    // ---------- HELPERS ----------
    private GunPart getSelectedOrFirstPart(String partCategoryId, GunType chosen, Player player) {
        String selectedKey = SelectedPartsManager.get(player, partCategoryId);
        if (selectedKey != null) {
            GunPart gp = getPartByKey(selectedKey);
            if (gp != null && gp.getGunTypes().contains(chosen)) {
                // just return the selection if it exists & is valid for this gun type
                return gp;
            }
        }
        return getFirstPartOfType(partCategoryId, chosen);
    }


    private GunPart getPartByKey(String key) {
        // if you have PartLoader.get() -> Map<String,GunPart>
        GunPart byMap = PartLoader.get().get(key);
        if (byMap != null) return byMap;

        for (GunPart gp : PartLoader.getOrdered()) {
            if (gp.getId().equalsIgnoreCase(key)) return gp;
        }
        return null;
    }

    private GunPart getFirstPartOfType(String id, GunType chosen) {
        for (GunPart part : PartLoader.getOrdered()) {
            if (!part.getGunTypes().contains(chosen)) continue;
            if (part.getPartType().getId().equalsIgnoreCase(id)) return part;
        }
        return null;
    }

    private String getPartIdForSlot(int slot) {
        for (PartData data : PartDataLoader.get().values()) {
            if (data.getSlot() == slot) return data.getId();
        }
        return null;
    }

    // ---------- ITEM BUILDERS (unchanged except already in your class) ----------
    private ItemStack createWeaponTypeButton(GunType type) {
        ItemStack item = new ItemStack(Material.NETHER_STAR); // placeholder icon
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Select Type: §e" + type.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to change weapon type");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String buildName(Collection<GunPart> parts) {
        // index → best fragment
        Map<Integer, GunPart.NameImpact> chosen = new HashMap<>();

        for (GunPart part : parts) {
            for (GunPart.NameImpact ni : part.getNameImpacts()) {
                int idx = ni.getIndex();

                if (idx == -1) {
                    // override everything
                    return "§7" + ni.getFragment();
                }

                GunPart.NameImpact existing = chosen.get(idx);
                if (existing == null || ni.getWeight() > existing.getWeight()) {
                    chosen.put(idx, ni);
                }
            }
        }

        // order by index
        List<Integer> order = new ArrayList<>(chosen.keySet());
        Collections.sort(order);

        StringBuilder sb = new StringBuilder("§7");
        for (int idx : order) {
            sb.append(chosen.get(idx).getFragment()).append(" ");
        }
        return sb.toString().trim();
    }

    public ItemStack createOutputItem(GunType type, Collection<GunPart> parts, boolean gui) {

        // Resolve skin
        SkinResolver resolver = new SkinResolver(SkinLoader.get());
        SkinData skin = resolver.resolve(type, parts);

        // Base item
        ItemStack base = TLibs.getItemAPI().getCreator().getItemFromPath(Cache.outputItem);
        ItemStack skinItem = skin.parseModel(SkinState.CARRY);
        base.setType(skinItem.getType());

        // Meta edits
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(skinItem.getItemMeta().getCustomModelData());
            meta.setDisplayName(buildName(parts));

            // ✅ Store skin ID in PDC
            NamespacedKey skinKey = new NamespacedKey(GunsAndGadgets.getInstance(), "skin_id");
            meta.getPersistentDataContainer().set(skinKey, PersistentDataType.STRING, skin.getId());

            base.setItemMeta(meta);

            // Apply stats afterwards (so lore & stat PDCs get written)
            base = StatApplier.apply(base, parts, gui);
        }

        return base;
    }


    public Collection<GunPart> collectPartsForPlayer(Player player, GunType chosen) {
        Collection<GunPart> parts = new ArrayList<>();
        for (PartData partData : PartDataLoader.get().values()) {
            GunPart part = getSelectedOrFirstPart(partData.getId(), chosen, player);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts;
    }



    private ItemStack createPartItem(GunPart part) {
        ItemStack i = TLibs.getItemAPI().getCreator().getItemFromPath(part.getItemKey());

        ItemMeta m = i.getItemMeta();

        // Name
        m.setDisplayName(part.getName());

        NamespacedKey key = new NamespacedKey(GunsAndGadgets.getInstance(), "part_id");
        m.getPersistentDataContainer().set(key, PersistentDataType.STRING, part.getId());

        // Lore
        List<String> lore = new ArrayList<>();

        // Existing lore from config (if any)
        if (part.getLore() != null && !part.getLore().isEmpty()) {
            for (String line : part.getLore()) {
                lore.add(StringFormatter.formatHex(line));
            }
        }

        // Add stats
        if (part.getStats() != null && !part.getStats().isEmpty()) {
            lore.add(""); // spacer line
            lore.add(StringFormatter.formatHex("#b38e88§lStats:"));

            for (Map.Entry<Stats, Integer> entry : part.getStats().entrySet()) {
                Stats stat = entry.getKey();
                int value = entry.getValue();

                // Fixed color for stat names
                String statColor = "#c2b48a"; // cyan-ish for readability

                // Dynamic color for values
                String valueColor;
                if (value < 0) {
                    valueColor = "#FF5555"; // red
                } else if (value == 0) {
                    valueColor = "#AAAAAA"; // gray
                } else if (value < 4) {
                    valueColor = "#FFD700"; // gold
                } else if (value < 7) {
                    valueColor = "#FFFF55"; // yellow
                } else {
                    valueColor = "#55FF55"; // green
                }

                // Format line
                String line = "§7■ "+statColor + stat.getDisplayName()
                            + "§e: " + valueColor + value;
                lore.add(StringFormatter.formatHex(line));
            }
        }

        if(part.hasCost()) {
            lore.add(""); // spacer line
            lore.add(StringFormatter.formatHex("#76de91§lInput:"));
            lore.addAll(CostFormatter.getCostsFormatted(part.getCost()));
        }

        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    // ---------- CLICK HANDLER ----------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // ASSEMBLY GUI
        if (event.getInventory().getHolder() instanceof AssemblyHolder) {
            event.setCancelled(true); // fully managed GUI

            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
            if(event.getSlot() == Cache.outputSlot) return;
            // slot 0 -> choose gun type
            if (event.getSlot() == 0) {
                openTypeSelection(player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                return;
            }

            // clicking a part slot -> open part selection for that category
            String partCategoryId = getPartIdForSlot(event.getSlot());
            if (partCategoryId != null) {
                openPartSelection(player, partCategoryId);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            }
            return;
        }

        // TYPE SELECTION GUI
        if (event.getInventory().getHolder() instanceof TypeSelectionHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name != null && name.equals("§eBack")) {
                openCraftingInventory(player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                return;
            }

            String clean = name == null ? "" : name.replace("§e", "").trim();
            for (GunType type : GunType.values()) {
                if (type.getDisplayName().equalsIgnoreCase(clean)) {
                    TypeSelectionManager.setSelectedType(player, type);
                    // reset selected parts to avoid invalid leftovers
                    SelectedPartsManager.clear(player);
                    openCraftingInventory(player);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                    return;
                }
            }
            return;
        }

        // PART SELECTION GUI
        if (event.getInventory().getHolder() instanceof PartSelectionHolder holder) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            // back button
            String dn = clicked.getItemMeta().getDisplayName();
            if ("§eBack".equals(dn)) {
                openCraftingInventory(player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                return;
            }
            NamespacedKey key = new NamespacedKey(GunsAndGadgets.getInstance(), "part_id");
            String partKey = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (partKey == null) return;

            // save selection and reopen preview
            SelectedPartsManager.set(player, holder.getPartId(), partKey);
            openCraftingInventory(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
        }
    }
}
