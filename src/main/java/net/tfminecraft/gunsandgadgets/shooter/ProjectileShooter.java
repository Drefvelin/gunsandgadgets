package net.tfminecraft.gunsandgadgets.shooter;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.guns.ammunition.Ammunition;
import net.tfminecraft.gunsandgadgets.guns.stats.StatCalculator;
import net.tfminecraft.gunsandgadgets.util.SoundPlayer;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.damage.ProjectileAttackMetadata;

public class ProjectileShooter {

    private static final NamespacedKey SPEED_KEY =
            new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_speed");

    public static void shoot(Player player, ItemStack gun, Ammunition ammo) {
        if (gun == null || !gun.hasItemMeta()) return;

        ItemMeta meta = gun.getItemMeta();
        if (meta == null) return;

        String shootSounds = meta.getPersistentDataContainer().get(
            new NamespacedKey(GunsAndGadgets.getInstance(), "shoot_sounds"),
            PersistentDataType.STRING
        );
        SoundPlayer.playSounds(player.getLocation(), shootSounds, true, 8f);


        Location start = player.getEyeLocation().clone();
        Vector forward = start.getDirection().normalize();
        start.add(forward.multiply(1.0));

        int speed = meta.getPersistentDataContainer()
                .getOrDefault(SPEED_KEY, PersistentDataType.INTEGER, 10);

        // 📏 Range stat (gun + ammo)
        int gunRange = meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_range"),
                PersistentDataType.INTEGER, 0);
        int ammoRange = ammo.getStats().getOrDefault("range", 0);

        int totalRange = gunRange + ammoRange;
        double maxDistance = (totalRange > 0 ? totalRange : speed * 20);

        // 🎯 Accuracy stat (gun + ammo)
        int gunAcc = meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_accuracy"),
                PersistentDataType.INTEGER, 0);
        int ammoAcc = ammo.getStats().getOrDefault("accuracy", 0);

        int totalAcc = gunAcc + ammoAcc;
        double spreadDegrees = StatCalculator.calculateAccuracy(totalAcc);

        int salt = meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(GunsAndGadgets.getInstance(), "accuracy_salt"),
                PersistentDataType.INTEGER, 0);

        java.util.Random rand = new java.util.Random(salt ^ System.nanoTime());

        int projectiles = Math.max(1, ammo.getAmount());

        for (int i = 0; i < projectiles; i++) {
            Vector shotDir = applySpread(forward.clone(), spreadDegrees, rand);
            Vector velocity = shotDir.multiply(speed * 0.7);
            int gunDamage = meta.getPersistentDataContainer().getOrDefault(
                    new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_damage"),
                    PersistentDataType.INTEGER, 0);
            int ammoDamage = ammo.getStats().getOrDefault("damage", 0);
            double totalDamage = gunDamage + ammoDamage;

            int pierceStat = ammo.getStats().getOrDefault("pierce", 0);

            startProjectileTask(player, start.clone(), velocity, maxDistance, totalDamage, pierceStat);
        }
    }


    private static Vector applySpread(Vector direction, double degrees, java.util.Random rand) {
        // Convert degrees to radians
        double radians = Math.toRadians(degrees);

        // Pick random yaw + pitch offsets within [-radians, +radians]
        double yaw = (rand.nextDouble() * 2 - 1) * radians;
        double pitch = (rand.nextDouble() * 2 - 1) * radians;

        // Apply rotation
        Vector dir = direction.clone();
        rotateAroundY(dir, yaw);
        rotateAroundX(dir, pitch);

        return dir.normalize();
    }

    private static void rotateAroundY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getZ() * cos - v.getX() * sin;
        v.setX(x);
        v.setZ(z);
    }

    private static void rotateAroundX(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        v.setY(y);
        v.setZ(z);
    }

    private static void startProjectileTask(Player player, Location start, Vector velocity, double maxDistance, double damage, int pierceStat) {
        new BukkitRunnable() {
            Location loc = start.clone();
            Vector vel = velocity.clone();

            @Override
            public void run() {
                Location prev = loc.clone();
                loc.add(vel);

                // 🔍 Check for hits between prev → loc
                handleHits(player, prev, loc, damage, pierceStat);

                // ✏️ Draw trace
                drawLine(prev, loc, 160);

                // Gravity
                vel.add(new Vector(0, -0.05, 0));

                if (loc.distanceSquared(start) > maxDistance * maxDistance) cancel();
            }
        }.runTaskTimer(GunsAndGadgets.getInstance(), 0L, 1L);
    }

    private static boolean isHeadshot(LivingEntity entity, Location hitPoint) {
        if (!(entity instanceof Player)) return false;

        BoundingBox box = entity.getBoundingBox();
        double headThreshold = box.getMinY() + (box.getHeight() * 0.75); // top 25%

        return hitPoint.getY() >= headThreshold;
    }


    private static void handleHits(Player player, Location from, Location to, double damage, int pierceStat) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        RayTraceResult result = from.getWorld().rayTraceEntities(
            from, direction, distance,
            entity -> entity instanceof LivingEntity && !((LivingEntity) entity).isDead()
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            Location hitPoint = result.getHitPosition().toLocation(from.getWorld());

            double finalDamage = damage;
            if (isHeadshot(target, hitPoint)) {
                finalDamage *= 1.5; // 🔥 1.5x multiplier for headshots
            }

            applyDamage(player, target, finalDamage, pierceStat);
        }
    }



    public static void applyDamage(Player attacker, LivingEntity target, double baseDamage, int pierceStat) {
        // --- Step 1: Calculate piercing portion ---
        double piercePercent = Math.min(1.0, pierceStat / 20.0); // 0 → 0%, 20 → 100%
        double pierceDamage = baseDamage * piercePercent;
        double normalDamage = baseDamage - pierceDamage;

        // --- Step 2: Apply pierce directly (ignores reduction) ---
        if (pierceDamage > 0) {
            target.damage(pierceDamage, attacker); // vanilla direct damage
        }

        // --- Step 3: Apply remaining damage via MythicLib/MMOCore ---
        if (normalDamage > 0) {
            DamageMetadata dmgMeta = new DamageMetadata(normalDamage, DamageType.PHYSICAL, DamageType.PROJECTILE);
            AttackMetadata attackMeta = new AttackMetadata(dmgMeta, target, StatProvider.get(attacker, EquipmentSlot.MAIN_HAND, true));
            MythicLib.inst().getDamage().registerAttack(attackMeta, false, true);
        }
    }


    private static void drawLine(Location from, Location to, double radius) {
        Vector diff = to.toVector().subtract(from.toVector());
        int steps = (int) (diff.length() * 4);
        Vector step = diff.clone().multiply(1.0 / steps);

        Location point = from.clone();
        for (int i = 0; i < steps; i++) {
            from.getWorld().spawnParticle(
                Particle.SMOKE_NORMAL,
                point,
                1,
                0, 0, 0,
                0,
                null,
                true
            );
            point.add(step);
        }
    }
}
