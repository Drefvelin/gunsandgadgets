package net.tfminecraft.gunsandgadgets.guns;

public enum GunType {
    RIFLE("Rifle"),
    PISTOL("Pistol"),
    SHOTGUN("Shotgun");

    private final String displayName;

    GunType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
