package net.tfminecraft.gunsandgadgets.shooter;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Util.LightEffect;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.attributes.AttributeReader;
import net.tfminecraft.gunsandgadgets.cache.Cache;
import net.tfminecraft.gunsandgadgets.guns.ammunition.Ammunition;
import net.tfminecraft.gunsandgadgets.guns.ammunition.Ammunition.AmmoOption;
import net.tfminecraft.gunsandgadgets.guns.stats.StatCalculator;
import net.tfminecraft.gunsandgadgets.util.SoundPlayer;

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
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;

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

        // Common values
        Location start = player.getEyeLocation().clone();
        Vector forward = start.getDirection().normalize();
        start.add(forward.multiply(1.0));

        // 💨 Muzzle smoke if not smokeless
        if (!ammo.hasOption(AmmoOption.SMOKELESS)) {
            spawnMuzzleSmoke(start, forward);
        }
        if (!ammo.hasOption(AmmoOption.NO_LIGHT)) {
            (new LightEffect()).createTemporaryLight(start, 10);
            new BukkitRunnable() {
                @Override
                public void run() {
                    (new LightEffect()).createTemporaryLight(start, 15);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            (new LightEffect()).createTemporaryLight(start, 8);
                        }
                    }.runTaskLater(GunsAndGadgets.getInstance(), 2L);
                }
            }.runTaskLater(GunsAndGadgets.getInstance(), 2L);
        }

        shootBullet(player, gun, ammo);
    }

    private static void spawnMuzzleSmoke(Location start, Vector forward) {
        Vector velocity = forward.clone();
        for(int i = 0; i<10; i++) {
            double mult = Math.max(0.2, i*0.08);
            velocity = forward.clone().multiply(mult);
            Vector spread = new Vector(
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2
                );
            start.getWorld().spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                start,
                0,
                spread.getX(), spread.getY(), spread.getZ(),
                mult
            );
        }
        for(int i = 0; i<10; i++) {
            double mult = Math.max(0.2, i*0.08);
            velocity = forward.clone().multiply(mult);
            start.getWorld().spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                start,
                0,
                velocity.getX(), velocity.getY(), velocity.getZ(),
                mult
            );
        }
    }



    public static void shootBullet(Player player, ItemStack gun, Ammunition ammo) {
        if (gun == null || !gun.hasItemMeta()) return;

        ItemMeta meta = gun.getItemMeta();
        if (meta == null) return;


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
        double maxDistance = (totalRange > 0 ? totalRange : ammo.hasOption(AmmoOption.ROCKET) ? 160 : Math.min(96, Math.abs(speed * 20)));

        // 🎯 Accuracy stat (gun + ammo)
        int gunAcc = meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_accuracy"),
                PersistentDataType.INTEGER, 0);
        int ammoAcc = ammo.getStats().getOrDefault("accuracy", 0);

        int totalAcc = gunAcc + ammoAcc;
        double spreadDegrees = Math.max(0.02, StatCalculator.calculateAccuracy(totalAcc)*AttributeReader.getAccuracyMultFromAttributes(player));

        int salt = meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(GunsAndGadgets.getInstance(), "accuracy_salt"),
                PersistentDataType.INTEGER, 0);

        java.util.Random rand = new java.util.Random(salt ^ System.nanoTime());

        int projectiles = Math.max(1, ammo.getAmount());

        for (int i = 0; i < projectiles; i++) {
            Vector shotDir = applySpread(forward.clone(), spreadDegrees, rand);
            Vector velocity = shotDir.multiply(speed * (ammo.hasOption(AmmoOption.ROCKET) ? 0.3 : 0.7));
            int gunDamage = meta.getPersistentDataContainer().getOrDefault(
                    new NamespacedKey(GunsAndGadgets.getInstance(), "stat_value_damage"),
                    PersistentDataType.INTEGER, 0);
            int ammoDamage = ammo.getStats().getOrDefault("damage", 0);
            double totalDamage = gunDamage + ammoDamage;

            int pierceStat = ammo.getStats().getOrDefault("pierce", 0);

            startProjectileTask(player, ammo, start.clone(), velocity, spreadDegrees, maxDistance, totalDamage, pierceStat);
        }
    }

    private static void explode(Player shooter, Location loc, double damage, int pierceStat) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 10, 0, 0, 0, 0, null, true);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 8f, 1f);
        (new LightEffect()).createTemporaryLight(loc, 10);
        new BukkitRunnable() {
            @Override
            public void run() {
                (new LightEffect()).createTemporaryLight(loc, 15);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        (new LightEffect()).createTemporaryLight(loc, 8);
                    }
                }.runTaskLater(GunsAndGadgets.getInstance(), 2L);
            }
        }.runTaskLater(GunsAndGadgets.getInstance(), 2L);

        // Damage entities in radius
        double radius = 8.0;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity le) {
                double dist = e.getLocation().distance(loc);
                double scale = Math.max(0, 1 - (dist / radius)); // closer = more damage
                applyDamage(shooter, le, damage * 2 * scale, pierceStat);
            }
        }

        // Block damage (if enabled)
        if (Cache.blockDamage) {
            loc.getWorld().createExplosion(loc, 2f, false, true, shooter);
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

    private static void startProjectileTask(Player player, Ammunition ammo, Location start, Vector velocity, double spreadDegrees, double maxDistance, double damage, int pierceStat) {
        new BukkitRunnable() {
            Location loc = start.clone();
            Vector vel = velocity.clone();

            // --- Rocket wobble state ---
            Vector wobble = new Vector(0, 0, 0);
            Vector wobbleTarget = randomOffset();
            int ticks = 0;

            // --- Spin mode state ---
            boolean spinning = false;
            int spinTicks = 0;
            int spinDuration = 10; // lasts 10 ticks
            Vector spinAxis = new Vector(0, 1, 0); // arbitrary axis (adjusted dynamically)

            @Override
            public void run() {
                ticks++;
                Location prev = loc.clone();
                loc.add(vel);

                // 🚀 Rocket wobble
                if (ammo.hasOption(AmmoOption.ROCKET) && ticks > 5) {
                    if(ticks > 100) {
                        explode(player, loc, damage, pierceStat);
                        cancel();
                        return;
                    }
                    if (!spinning && ticks % 5 == 0) {
                        Vector newTarget = randomOffset();
                        if (newTarget.clone().subtract(wobble).lengthSquared() > 1.5) {
                            // big change → trigger spin mode
                            spinning = true;
                            spinTicks = 0;
                            spinAxis = vel.clone().crossProduct(newTarget).normalize();
                        }
                        wobbleTarget = newTarget;
                    }

                    if (spinning) {
                        spinTicks++;
                        double angle = Math.sin(spinTicks / (double) spinDuration * Math.PI) * 0.4; // smooth wave
                        vel.rotateAroundAxis(spinAxis, angle);

                        if (spinTicks >= spinDuration) {
                            spinning = false;
                        }
                    } else {
                        // Smooth wobble
                        wobble.multiply(0.8).add(wobbleTarget.clone().multiply(0.2));
                        vel.add(wobble).normalize().multiply(velocity.length());
                    }

                    // Rocket sound
                    if(ticks % 3 == 0) loc.getWorld().playSound(loc, Cache.rocketSound, 4f, 1.3f);
                }

                // 🔍 Check for hits
                if (handleHits(player, ammo, prev, loc, damage, pierceStat)) {
                    cancel();
                    return;
                }

                // ✏️ Draw trace
                drawLine(ammo, prev, loc, 160);

                // Gravity (lighter for rockets)
                vel.add(new Vector(0, ammo.hasOption(AmmoOption.ROCKET) ? -0.02 : -0.05, 0));

                if (loc.distanceSquared(start) > maxDistance * maxDistance) cancel();
            }

            private Vector randomOffset() {
                return new Vector(
                    (Math.random() - 0.5) * spreadDegrees/3,
                    (Math.random() - 0.5) * spreadDegrees/3,
                    (Math.random() - 0.5) * spreadDegrees/3
                );
            }
        }.runTaskTimer(GunsAndGadgets.getInstance(), 0L, 1L);
    }




    private static boolean isHeadshot(LivingEntity entity, Location hitPoint) {
        if (!(entity instanceof Player)) return false;

        BoundingBox box = entity.getBoundingBox();
        double headThreshold = box.getMinY() + (box.getHeight() * 0.75); // top 25%

        return hitPoint.getY() >= headThreshold;
    }


    private static boolean handleHits(Player player, Ammunition ammo, Location from, Location to, double damage, int pierceStat) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        RayTraceResult result = from.getWorld().rayTraceEntities(
            from,                    // start point
            direction,               // direction vector (should be normalized)
            distance,                // how far to check
            Cache.creators.contains(player.getName()) ? 0.5 : 0.2,                     // radius (the "thickness" of the ray)
            entity -> entity instanceof LivingEntity && !((LivingEntity) entity).isDead()
        );


        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            if(target instanceof Player) {
                if(((Player) target).equals(player)) return false;
            }
            Location hitPoint = result.getHitPosition().toLocation(from.getWorld());
            if(ammo.hasOption(AmmoOption.ROCKET)) {
                explode(player, hitPoint, damage, pierceStat);
                return true;
            }

            double finalDamage = damage;
            if (isHeadshot(target, hitPoint)) {
                finalDamage *= 1.5; // 🔥 1.5x multiplier for headshots
            }

            applyDamage(player, target, finalDamage, pierceStat);
            return true; // ✅ stop projectile
        } else if (result != null) {
            Entity hit = result.getHitEntity();
            ActiveVehicle v = VehicleFramework.getVehicleManager().get(hit);
            if(v != null) {
                if(ammo.hasOption(AmmoOption.ROCKET)) v.damage("ROCKET", damage);
                else v.damage("PROJECTILE", damage);
            }
        }

        // Check block collision
        if (from.getBlock().getType().isSolid()) {
            if(ammo.hasOption(AmmoOption.ROCKET)) {
                explode(player, from, damage, pierceStat);
            }
            return true; // ✅ hit a block, stop
        }

        return false;
    }




    public static void applyDamage(Player attacker, LivingEntity target, double baseDamage, int pierceStat) {
        // --- Step 1: Calculate piercing portion ---
        double piercePercent = Math.min(1.0, pierceStat / 20.0); // 0 → 0%, 20 → 100%
        double pierceDamage = baseDamage * piercePercent;
        double normalDamage = baseDamage - pierceDamage;

        // --- Step 2: Apply pierce directly (ignores reduction) ---
        if (pierceDamage > 0) {
            if(pierceDamage > target.getHealth()) pierceDamage = target.getHealth()-0.1;
            target.damage(pierceDamage); // vanilla direct damage
        }

        // --- Step 3: Apply remaining damage via MythicLib/MMOCore ---
        if (normalDamage > 0) {
            DamageMetadata dmgMeta = new DamageMetadata(normalDamage, DamageType.PROJECTILE);
            AttackMetadata attackMeta = new AttackMetadata(dmgMeta, target, StatProvider.get(attacker, EquipmentSlot.MAIN_HAND, true));
            MythicLib.inst().getDamage().registerAttack(attackMeta, false, true);
        }
    }


    private static void drawLine(Ammunition ammo, Location from, Location to, double radius) {
        Vector diff = to.toVector().subtract(from.toVector());
        int steps = (int) (diff.length() * 4);
        Vector step = diff.clone().multiply(1.0 / steps);

        Location point = from.clone();
        for (int i = 0; i < steps; i++) {
            if(ammo.hasOption(AmmoOption.ROCKET)) {
                from.getWorld().spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    point,
                    10,
                    0, 0, 0,
                    0.05,
                    null,
                    true
                );
            } else {
                from.getWorld().spawnParticle(
                    Particle.SMOKE_NORMAL,
                    point,
                    1,
                    0, 0, 0,
                    0,
                    null,
                    true
                );
            }
            point.add(step);
        }
    }
}
