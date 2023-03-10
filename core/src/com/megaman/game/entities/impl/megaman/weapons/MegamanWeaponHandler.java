package com.megaman.game.entities.impl.megaman.weapons;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.megaman.game.ConstKeys;
import com.megaman.game.GameEngine;
import com.megaman.game.assets.SoundAsset;
import com.megaman.game.behaviors.BehaviorType;
import com.megaman.game.entities.utils.factories.EntityFactories;
import com.megaman.game.entities.EntityType;
import com.megaman.game.entities.utils.faceable.Facing;
import com.megaman.game.entities.impl.megaman.Megaman;
import com.megaman.game.entities.impl.projectiles.ChargeStatus;
import com.megaman.game.entities.impl.projectiles.Projectile;
import com.megaman.game.entities.impl.projectiles.ProjectileFactory;
import com.megaman.game.entities.impl.megaman.vals.MegamanVals;
import com.megaman.game.entities.impl.projectiles.impl.Bullet;
import com.megaman.game.entities.impl.projectiles.impl.ChargedShot;
import com.megaman.game.entities.impl.projectiles.impl.Fireball;
import com.megaman.game.utils.interfaces.Resettable;
import com.megaman.game.utils.interfaces.Updatable;
import com.megaman.game.utils.objs.Timer;
import com.megaman.game.world.BodySense;
import com.megaman.game.world.WorldVals;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class MegamanWeaponHandler implements Updatable, Resettable {

    private static class MegaWeaponEntry implements Updatable {

        private final Timer cooldownTimer;

        private Supplier<Integer> normalCost;
        private Supplier<Integer> halfChargedCost;
        private Supplier<Integer> fullyChargedCost;
        private Supplier<Boolean> chargeable;
        private Supplier<Boolean> canFireWeapon;
        private Array<Projectile> spawned;
        private int ammo;

        private MegaWeaponEntry(float cooldownDur) {
            this(cooldownDur, () -> true);
        }

        private MegaWeaponEntry(float cooldownDur, Supplier<Boolean> chargeable) {
            this(cooldownDur, chargeable, () -> true);
        }

        private MegaWeaponEntry(float cooldownDur, Supplier<Boolean> chargeable, Supplier<Boolean> canFireWeapon) {
            this(cooldownDur, chargeable, canFireWeapon, () -> 0, () -> 0, () -> 0);
        }

        private MegaWeaponEntry(float cooldownDur, Supplier<Boolean> chargeable, Supplier<Boolean> canFireWeapon,
                                Supplier<Integer> normalCost, Supplier<Integer> halfChargedCost,
                                Supplier<Integer> fullyChargedCost) {
            this.canFireWeapon = canFireWeapon;
            this.chargeable = chargeable;
            this.normalCost = normalCost;
            this.halfChargedCost = halfChargedCost;
            this.fullyChargedCost = fullyChargedCost;
            cooldownTimer = new Timer(cooldownDur, true);
            spawned = new Array<>();
            ammo = MegamanVals.MAX_WEAPON_AMMO;
        }

        public int getNormalCost() {
            return normalCost.get();
        }

        public int getHalfChargedCost() {
            return halfChargedCost.get();
        }

        public int getFullyChargedCost() {
            return fullyChargedCost.get();
        }

        private boolean isChargeable() {
            return chargeable.get();
        }

        private boolean canFireWeapon() {
            return canFireWeapon.get();
        }

        @Override
        public void update(float delta) {
            cooldownTimer.update(delta);
            Iterator<Projectile> pIter = spawned.iterator();
            while (pIter.hasNext()) {
                Projectile p = pIter.next();
                if (p.dead) {
                    pIter.remove();
                }
            }
        }

    }

    private static final float MEGA_BUSTER_BULLET_VEL = 10f;
    private static final Vector2 FLAME_TOSS_TRAJ = new Vector2(35f, 10f);

    private final Megaman megaman;
    private final GameEngine engine;
    private final EntityFactories factories;
    private final Map<MegamanWeapon, MegaWeaponEntry> weapons;

    public MegamanWeaponHandler(Megaman megaman) {
        this.megaman = megaman;
        engine = megaman.game.getGameEngine();
        factories = megaman.game.getEntityFactories();
        weapons = new EnumMap<>(MegamanWeapon.class);
    }

    @Override
    public void reset() {
        for (MegaWeaponEntry e : weapons.values()) {
            e.cooldownTimer.setToEnd();
        }
    }

    @Override
    public void update(float delta) {
        MegaWeaponEntry e = weapons.get(megaman.currWeapon);
        if (e == null) {
            return;
        }
        e.update(delta);
    }

    private MegaWeaponEntry getWeaponEntry(MegamanWeapon weapon) {
        return switch (weapon) {
            case MEGA_BUSTER -> new MegaWeaponEntry(.01f);
            case FLAME_TOSS -> {
                MegaWeaponEntry e = new MegaWeaponEntry(.5f);
                e.normalCost = () -> 3;
                e.halfChargedCost = () -> 5;
                e.fullyChargedCost = () -> 7;
                e.chargeable = () -> !megaman.is(BodySense.BODY_IN_WATER);
                e.canFireWeapon = () -> !megaman.is(BodySense.BODY_IN_WATER) && e.spawned.size == 0;
                yield e;
            }
        };
    }

    public Array<Projectile> getSpawned(MegamanWeapon weapon) {
        if (!hasWeapon(weapon)) {
            throw new IllegalStateException("Megaman does not have the weapon: " + weapon);
        }
        return weapons.get(weapon).spawned;
    }

    public void putWeapon(MegamanWeapon weapon) {
        weapons.put(weapon, getWeaponEntry(weapon));
    }

    public boolean hasWeapon(MegamanWeapon weapon) {
        return weapons.containsKey(weapon);
    }

    public boolean canFireWeapon(MegamanWeapon weapon, ChargeStatus stat) {
        if (!hasWeapon(weapon)) {
            return false;
        }
        MegaWeaponEntry e = weapons.get(weapon);
        if (!e.cooldownTimer.isFinished() || !e.canFireWeapon()) {
            return false;
        }
        int cost = e.isChargeable() ?
                (weapon == MegamanWeapon.MEGA_BUSTER ? 0 : switch (stat) {
                    case FULLY_CHARGED -> e.getFullyChargedCost();
                    case HALF_CHARGED -> e.getHalfChargedCost();
                    case NOT_CHARGED -> e.getNormalCost();
                }) : e.getNormalCost();
        return cost <= e.ammo;
    }

    public boolean isChargeable(MegamanWeapon weapon) {
        return hasWeapon(weapon) && weapons.get(weapon).isChargeable();
    }

    public void translateAmmo(MegamanWeapon weapon, int delta) {
        if (!hasWeapon(weapon)) {
            throw new IllegalStateException("Megaman does not have the weapon: " + weapon);
        }
        MegaWeaponEntry e = weapons.get(weapon);
        e.ammo += delta;
        if (e.ammo >= MegamanVals.MAX_WEAPON_AMMO) {
            e.ammo = MegamanVals.MAX_WEAPON_AMMO;
        } else if (e.ammo < 0) {
            e.ammo = 0;
        }
    }

    public void setToMaxAmmo(MegamanWeapon weapon) {
        if (!hasWeapon(weapon)) {
            throw new IllegalStateException("Megaman does not have the weapon: " + weapon);
        }
        MegaWeaponEntry e = weapons.get(weapon);
        e.ammo = MegamanVals.MAX_WEAPON_AMMO;
    }

    public void depleteAmmo(MegamanWeapon weapon) {
        if (!hasWeapon(weapon)) {
            throw new IllegalStateException("Megaman does not have the weapon: " + weapon);
        }
        MegaWeaponEntry e = weapons.get(weapon);
        e.ammo = 0;
    }

    public int getAmmo(MegamanWeapon weapon) {
        if (weapon == MegamanWeapon.MEGA_BUSTER) {
            return Integer.MAX_VALUE;
        }
        return weapons.get(weapon).ammo;
    }

    public boolean fireWeapon(MegamanWeapon weapon, ChargeStatus stat) {
        if (!canFireWeapon(weapon, stat)) {
            return false;
        }
        if (!isChargeable(weapon)) {
            stat = ChargeStatus.NOT_CHARGED;
        }
        MegaWeaponEntry e = weapons.get(weapon);
        int cost = weapon == MegamanWeapon.MEGA_BUSTER ? 0 : switch (stat) {
            case FULLY_CHARGED -> e.getFullyChargedCost();
            case HALF_CHARGED -> e.getHalfChargedCost();
            case NOT_CHARGED -> e.getNormalCost();
        };
        if (cost > getAmmo(weapon)) {
            return false;
        }
        Projectile p = switch (weapon) {
            case MEGA_BUSTER -> fireMegaBuster(stat);
            case FLAME_TOSS -> fireFlameToss(stat);
        };
        if (p != null) {
            e.spawned.add(p);
        }
        e.cooldownTimer.reset();
        translateAmmo(weapon, -cost);
        return true;
    }

    private Vector2 getSpawnCenter() {
        Vector2 spawnCenter = new Vector2(megaman.body.getCenter());
        float xOffset = WorldVals.PPM * .85f;
        if (megaman.is(Facing.LEFT)) {
            xOffset *= -1f;
        }
        spawnCenter.x += xOffset;
        float yOffset = WorldVals.PPM / 16f;
        if (megaman.is(BehaviorType.CLIMBING) || megaman.is(BehaviorType.WALL_SLIDING)) {
            yOffset += .15f * WorldVals.PPM;
        } else if (megaman.is(BodySense.FEET_ON_GROUND)) {
            yOffset -= .05f * WorldVals.PPM;
        } else {
            yOffset += .25f * WorldVals.PPM;
        }
        spawnCenter.y += yOffset;
        return spawnCenter;
    }

    private Projectile fireMegaBuster(ChargeStatus stat) {
        float x = MEGA_BUSTER_BULLET_VEL;
        if (megaman.is(Facing.LEFT)) {
            x *= -1f;
        }
        Vector2 trajectory = new Vector2(x, 0f);
        ObjectMap<String, Object> data = new ObjectMap<>();
        data.put(ConstKeys.OWNER, megaman);
        data.put(ConstKeys.TRAJECTORY, trajectory);
        Projectile proj = switch (stat) {
            case NOT_CHARGED -> (Bullet) factories.fetch(EntityType.PROJECTILE, ProjectileFactory.BULLET);
            case HALF_CHARGED, FULLY_CHARGED -> {
                data.put(ConstKeys.BOOL, stat == ChargeStatus.FULLY_CHARGED);
                yield (ChargedShot) factories.fetch(EntityType.PROJECTILE, ProjectileFactory.CHARGED_SHOT);
            }
        };
        if (stat == ChargeStatus.NOT_CHARGED) {
            megaman.request(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, true);
        } else {
            megaman.request(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, true);
            megaman.request(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false);
        }
        Vector2 s = getSpawnCenter();
        if (megaman.isUpsideDown()) {
            if (megaman.is(BehaviorType.CLIMBING)) {
                s.y -= .45f * WorldVals.PPM;
            } else {
                s.y -= .05f * WorldVals.PPM;
            }
        }
        engine.spawn(proj, s, data);
        return proj;
    }

    private Projectile fireFlameToss(ChargeStatus stat) {
        ObjectMap<String, Object> data = new ObjectMap<>();
        data.put(ConstKeys.OWNER, megaman);
        Projectile proj = switch (stat) {
            case NOT_CHARGED, HALF_CHARGED, FULLY_CHARGED -> {
                data.put(ConstKeys.LEFT, megaman.is(Facing.LEFT));
                yield (Fireball) factories.fetch(EntityType.PROJECTILE, ProjectileFactory.FIREBALL);
            }

            // TODO: charged flame weapons
            /*
            case HALF_CHARGED -> {

            }
            case FULLY_CHARGED -> {

            }
             */

        };
        engine.spawn(proj, getSpawnCenter(), data);

        // TODO: playMusic different sounds
        megaman.request(SoundAsset.CRASH_BOMBER_SOUND, true);

        return proj;
    }

}
