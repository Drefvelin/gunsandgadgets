package net.tfminecraft.gunsandgadgets.guns.parts;

import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.gunsandgadgets.guns.GunType;
import net.tfminecraft.gunsandgadgets.guns.SoundType;
import net.tfminecraft.gunsandgadgets.guns.stats.Stats;
import net.tfminecraft.gunsandgadgets.loader.PartDataLoader;

public class GunPart {

    private final String id;
    private final String name;
    private PartData partType;
    private Set<GunType> gunTypes = new HashSet<>();
    private final String itemKey;
    private final Map<Stats, Integer> stats;
    private final List<String> calibers;
    private List<String> caliberOverrides;
    private List<String> lore = new ArrayList<>();

    private final Map<SoundType, List<PartSound>> sounds = new HashMap<>();
    private final Map<SoundType, List<PartSound>> soundOverrides = new HashMap<>();

    private final Map<String, Integer> cost = new HashMap<>();

    // NEW: name-impact entries
    private final List<NameImpact> nameImpacts = new ArrayList<>();
    private final List<SkinImpact> skinImpacts = new ArrayList<>();

    public GunPart(String key, ConfigurationSection config) {
        this.id = key;
        this.name = StringFormatter.formatHex(config.getString("name", key));

        PartData data = PartDataLoader.getByString(config.getString("part-type", "barrel"));
        if (data != null) partType = data;

        this.itemKey = config.getString("item", "v.stone");

        for (String s : config.getStringList("type")) {
            try {
                GunType g = GunType.valueOf(s.toUpperCase());
                gunTypes.add(g);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Stats
        this.stats = new HashMap<>();
        List<String> statList = config.getStringList("stats");
        for (String s : statList) {
            String[] split = s.split(" ");
            if (split.length == 2) {
                Stats stat = Stats.fromKey(split[0]);
                if (stat != null) {
                    int value = Integer.parseInt(split[1]);
                    stats.put(stat, value); // now stats map is <Stats, Integer>
                }
            }

        }

        if(config.contains("cost")) {
            List<String> costList = config.getStringList("cost");
            for (String c : costList) {
                try {
                    // e.g. "v.iron_ingot(4)"
                    int start = c.indexOf("(");
                    int end = c.indexOf(")");
                    if (start > 0 && end > start) {
                        String ckey = c.substring(0, start).trim(); // "v.iron_ingot"
                        int amount = Integer.parseInt(c.substring(start + 1, end));
                        cost.put(ckey, amount);
                    } else {
                        // fallback: no () found, assume amount = 1
                        cost.put(c.trim(), 1);
                    }
                } catch (Exception e) {
                    System.out.println("Invalid cost entry for part " + id + ": " + c);
                }
            }
        }

        this.calibers = config.getStringList("caliber");
        if (config.contains("caliber-override")) {
            this.caliberOverrides = config.getStringList("caliber-override");
        }

        for (String s : config.getStringList("lore")) {
            lore.add(StringFormatter.formatHex(s));
        }

        // Parse name-impacts
        for (String s : config.getStringList("name-impact")) {
            String[] split = s.split(" ");
            if (split.length >= 3) {
                try {
                    String fragment = StringFormatter.formatHex(split[0]);
                    int index = Integer.parseInt(split[1]);
                    int weight = Integer.parseInt(split[2]);
                    nameImpacts.add(new NameImpact(fragment, index, weight));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid name-impact entry for part " + key + ": " + s);
                }
            }
        }

        for (String s : config.getStringList("skin-impact")) {
            String[] split = s.split(" ");
            if (split.length >= 2) {
                try {
                    String skinId = split[0].toLowerCase();
                    int weight = Integer.parseInt(split[1]);
                    skinImpacts.add(new SkinImpact(skinId, weight));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid skin-impact entry for part " + key + ": " + s);
                }
            }
        }
        
        // Parse normal sounds
        parseSoundSection(config.getStringList("sounds"), sounds);

        // Parse overrides
        parseSoundSection(config.getStringList("sound-overrides"), soundOverrides);
    }

    private void parseSoundSection(List<String> entries, Map<SoundType, List<PartSound>> target) {
        for (String s : entries) {
            String[] split = s.split(" ", 2);
            if (split.length < 2) continue;

            SoundType type;
            try {
                type = SoundType.valueOf(split[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid sound type for part " + id + ": " + split[0]);
                continue;
            }

            String raw = split[1].trim();
            String soundKeys = raw;
            GunType requiredType = null;

            if (raw.contains("(") && raw.endsWith(")")) {
                int start = raw.indexOf("(");
                int end = raw.lastIndexOf(")");
                soundKeys = raw.substring(0, start);
                String typeStr = raw.substring(start + 1, end);

                try {
                    requiredType = GunType.valueOf(typeStr.toUpperCase());
                } catch (Exception e) {
                    System.out.println("Invalid gun type for sound in part " + id + ": " + typeStr);
                }
            }

            List<String> keys = Arrays.stream(soundKeys.split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();

            target.computeIfAbsent(type, t -> new ArrayList<>())
                .add(new PartSound(keys, requiredType));
        }
    }


    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public PartData getPartType() { return partType; }
    public String getItemKey() { return itemKey; }
    public Map<Stats, Integer> getStats() { return stats; }
    public List<String> getCalibers() { return calibers; }
    public boolean hasCaliberOverrides() { return caliberOverrides != null; }
    public List<String> getCaliberOverrides() { return caliberOverrides; }
    public List<String> getLore() { return lore; }
    public Set<GunType> getGunTypes() { return gunTypes; }
    public Map<String, Integer> getCost() { return cost; }
    public boolean hasCost() { return cost.size() > 0; }
    public List<NameImpact> getNameImpacts() { return nameImpacts; }
    public List<SkinImpact> getSkinImpacts() { return skinImpacts; }
    public Map<SoundType, List<PartSound>> getSounds() { return sounds; }
    public Map<SoundType, List<PartSound>> getSoundOverrides() { return soundOverrides; }

    // Inner class for name-impact
    public static class NameImpact {
        private final String fragment;
        private final int index;
        private final int weight;

        public NameImpact(String fragment, int index, int weight) {
            this.fragment = fragment;
            this.index = index;
            this.weight = weight;
        }

        public String getFragment() { return fragment; }
        public int getIndex() { return index; }
        public int getWeight() { return weight; }
    }

    public static class SkinImpact {
        private final String skinId;
        private final int weight;

        public SkinImpact(String skinId, int weight) {
            this.skinId = skinId;
            this.weight = weight;
        }

        public String getSkinId() { return skinId; }
        public int getWeight() { return weight; }
    }

    public static class PartSound {
        private final List<String> soundKeys; // multiple options
        private final GunType requiredType; // may be null

        public PartSound(List<String> soundKeys, GunType requiredType) {
            this.soundKeys = soundKeys;
            this.requiredType = requiredType;
        }

        public GunType getRequiredType() { return requiredType; }

        public boolean matches(GunType type) {
            return requiredType == null || requiredType == type;
        }

        public List<String> getSoundKeys() { return soundKeys; }

        /** Pick one random key */
        public String pickRandomKey(Random rand) {
            if (soundKeys.isEmpty()) return null;
            return soundKeys.get(rand.nextInt(soundKeys.size()));
        }
    }
}
