package net.tfminecraft.gunsandgadgets.guns.stats;

public class StatCalculator {

    public static int calculateReloadTicks(int reloadStat) {
        int base = 60; // 3 seconds
        int modifier = Math.max(-20, Math.min(20, reloadStat));

        double factor = 1.0 - (modifier * 0.05);
        int ticks = (int) Math.round(base * factor);

        return Math.max(10, Math.min(200, ticks)); // clamp
    }
}

