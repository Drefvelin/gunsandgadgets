package net.tfminecraft.gunsandgadgets.guns.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.tfminecraft.gunsandgadgets.guns.parts.GunPart;
import net.tfminecraft.gunsandgadgets.loader.PartLoader;
import net.tfminecraft.gunsandgadgets.utils.GGCraftKeys;

public final class GunCraftProvenance {

    private static final Gson GSON = new Gson();

    private List<GGCraftPart> parts = new ArrayList<>();
    private int partsRevision;

    public GunCraftProvenance() {}

    public GunCraftProvenance(List<GGCraftPart> parts, int partsRevision) {
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
        this.partsRevision = partsRevision;
    }

    public List<GGCraftPart> getParts() {
        return parts;
    }

    public int getPartsRevision() {
        return partsRevision;
    }

    public static GunCraftProvenance from(Collection<GunPart> liveParts) {
        List<GGCraftPart> stamped = new ArrayList<>();
        int maxRevision = 0;
        for (GunPart part : liveParts) {
            int revision = part.getRevision();
            stamped.add(new GGCraftPart(part.getId(), revision));
            maxRevision = Math.max(maxRevision, revision);
        }
        return new GunCraftProvenance(stamped, maxRevision);
    }

    public void applyTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(GGCraftKeys.craftParts(), GGCraftKeys.STRING, GSON.toJson(parts));
        meta.getPersistentDataContainer().set(GGCraftKeys.partsRevision(), GGCraftKeys.INTEGER, partsRevision);
        item.setItemMeta(meta);
    }

    public static GunCraftProvenance readFrom(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String json = meta.getPersistentDataContainer().get(GGCraftKeys.craftParts(), GGCraftKeys.STRING);
        if (json == null || json.isBlank()) {
            return null;
        }
        List<GGCraftPart> parsed = GSON.fromJson(json, new TypeToken<List<GGCraftPart>>() {}.getType());
        if (parsed == null) {
            parsed = new ArrayList<>();
        }
        Integer stampedMax = meta.getPersistentDataContainer().get(GGCraftKeys.partsRevision(), GGCraftKeys.INTEGER);
        int maxRevision = stampedMax != null ? stampedMax : 0;
        return new GunCraftProvenance(parsed, maxRevision);
    }

    public boolean isOutdated() {
        for (GGCraftPart stamped : parts) {
            GunPart live = PartLoader.getByString(stamped.getId());
            if (live != null && live.getRevision() > stamped.getRevision()) {
                return true;
            }
        }
        return false;
    }

    public List<GGCraftPart> getOutdatedParts() {
        List<GGCraftPart> outdated = new ArrayList<>();
        for (GGCraftPart stamped : parts) {
            GunPart live = PartLoader.getByString(stamped.getId());
            if (live != null && live.getRevision() > stamped.getRevision()) {
                outdated.add(stamped);
            }
        }
        return outdated;
    }

    public void syncRevisions() {
        int max = 0;
        for (GGCraftPart stamped : parts) {
            GunPart live = PartLoader.getByString(stamped.getId());
            if (live != null) {
                stamped.setRevision(live.getRevision());
                max = Math.max(max, live.getRevision());
            }
        }
        partsRevision = max;
    }

    /**
     * Resolves stamped ids against live PartLoader. Missing yaml entries go to missingIds.
     */
    public ResolvedParts resolveStampedParts() {
        List<GunPart> live = new ArrayList<>();
        List<String> missingIds = new ArrayList<>();
        for (GGCraftPart stamped : parts) {
            GunPart part = PartLoader.getByString(stamped.getId());
            if (part == null) {
                missingIds.add(stamped.getId());
            } else {
                live.add(part);
            }
        }
        return new ResolvedParts(live, missingIds);
    }

    public record ResolvedParts(List<GunPart> live, List<String> missingIds) {}
}
