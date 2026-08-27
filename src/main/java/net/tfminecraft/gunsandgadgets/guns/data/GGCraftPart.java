package net.tfminecraft.gunsandgadgets.guns.data;

/**
 * One stamped part entry on a crafted gun (GSON short keys: id, r).
 */
public class GGCraftPart {

    private String id;
    private int r;

    public GGCraftPart() {}

    public GGCraftPart(String id, int revision) {
        this.id = id;
        this.r = revision;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRevision() {
        return r;
    }

    public void setRevision(int revision) {
        this.r = revision;
    }
}
