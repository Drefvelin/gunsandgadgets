package net.tfminecraft.gunsandgadgets.guns.stats;

public enum Stats {
    ACCURACY("accuracy"),
    SPEED("speed"),
    RELOAD("reload"),
    FIRE_RATE("fire_rate"),
    CAPACITY("capacity"),
    DAMAGE("damage"),
    SPREAD("spread"),
    RANGE("range"),
    PIERCE("pierce");

    private final String key;

    Stats(String key) {
        this.key = key;
    }

    /**
     * The config key (used in parts.yml).
     */
    public String getKey() {
        return key;
    }

    /**
     * Capitalized display name (e.g. "Fire_rate" -> "Fire Rate").
     */
    public String getDisplayName() {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Find enum constant by config key (case-insensitive).
     */
    public static Stats fromKey(String key) {
        for (Stats s : values()) {
            if (s.key.equalsIgnoreCase(key)) {
                return s;
            }
        }
        return null;
    }
}

