package com.megaman.game.entities.impl.enemies;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.megaman.game.ConstKeys;
import com.megaman.game.MegamanGame;
import com.megaman.game.assets.SoundAsset;
import com.megaman.game.audio.SoundComponent;
import com.megaman.game.cull.CullOnEventComponent;
import com.megaman.game.cull.CullOutOfBoundsComponent;
import com.megaman.game.entities.*;
import com.megaman.game.entities.utils.damage.DamageNegotiation;
import com.megaman.game.entities.utils.damage.Damageable;
import com.megaman.game.entities.utils.damage.Damager;
import com.megaman.game.entities.utils.faceable.Facing;
import com.megaman.game.entities.impl.explosions.ExplosionFactory;
import com.megaman.game.entities.impl.items.ItemFactory;
import com.megaman.game.entities.impl.megaman.Megaman;
import com.megaman.game.events.EventType;
import com.megaman.game.health.HealthComponent;
import com.megaman.game.updatables.UpdatableComponent;
import com.megaman.game.utils.UtilMethods;
import com.megaman.game.utils.objs.Timer;
import com.megaman.game.world.Body;
import com.megaman.game.world.BodyComponent;
import com.megaman.game.world.BodySense;
import com.megaman.game.world.BodyType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public abstract class Enemy extends Entity implements Damager, Damageable {

    private static final float DEFAULT_CULL_DUR = .75f;
    private static final float DEFAULT_DMG_DUR = .15f;
    private static final float DEFAULT_DMG_BLINK_DUR = .025f;

    protected final Body body;
    protected final Timer dmgTimer;
    protected final Timer dmgBlinkTimer;
    protected final Map<Class<? extends Damager>, DamageNegotiation> dmgNegs;

    protected boolean dmgBlink;
    protected boolean doDropItem;

    public Enemy(MegamanGame game, BodyType bodyType) {
        this(game, DEFAULT_DMG_DUR, bodyType);
    }

    public Enemy(MegamanGame game, float dmgDur, BodyType bodyType) {
        this(game, dmgDur, DEFAULT_CULL_DUR, bodyType);
    }

    public Enemy(MegamanGame game, float dmgDur, float cullDur, BodyType bodyType) {
        this(game, dmgDur, DEFAULT_DMG_BLINK_DUR, cullDur, bodyType);
    }

    public Enemy(MegamanGame game, float dmgDur, float dmgBlinkDur, float cullDur, BodyType bodyType) {
        super(game, EntityType.ENEMY);
        doDropItem = true;
        dmgTimer = new Timer(dmgDur, true);
        dmgBlinkTimer = new Timer(dmgBlinkDur, true);
        dmgNegs = defineDmgNegs();
        body = new Body(bodyType);
        putComponent(new BodyComponent(body));
        UpdatableComponent u = new UpdatableComponent();
        defineUpdateComponent(u);
        putComponent(u);
        CullOnEventComponent c = new CullOnEventComponent();
        defineCullOnEventComponent(c);
        putComponent(c);
        putComponent(new SoundComponent());
        putComponent(new HealthComponent());
        if (cullDur >= 0f) {
            putComponent(new CullOutOfBoundsComponent(() -> body.bounds, cullDur));
        }
        runOnDeath.add(() -> {
            if (hasHealth(0)) {
                disintegrate();
                if (doDropItem) {
                    UtilMethods.doIfRandMatch(0, 10, new Array<>() {{
                        add(1);
                        add(3);
                        add(9);
                    }}, r -> {
                        Entity e = game.getEntityFactories().fetch(EntityType.ITEM, ItemFactory.HEALTH_BULB);
                        game.getGameEngine().spawn(e, body.bounds, new ObjectMap<>() {{
                            put(ConstKeys.LARGE, r == 1);
                        }});
                    });
                }
            }
        });
    }

    protected abstract Map<Class<? extends Damager>, DamageNegotiation> defineDmgNegs();

    protected void request(SoundAsset ass, boolean play) {
        SoundComponent c = getComponent(SoundComponent.class);
        if (play) {
            c.requestToPlay(ass);
        } else {
            c.requestToStop(ass);
        }
    }

    protected void defineUpdateComponent(UpdatableComponent c) {
        c.add(delta -> {
            dmgTimer.update(delta);
            if (!dmgTimer.isFinished()) {
                dmgBlinkTimer.update(delta);
                if (dmgBlinkTimer.isFinished()) {
                    dmgBlinkTimer.reset();
                    dmgBlink = !dmgBlink;
                }
            }
            if (dmgTimer.isJustFinished()) {
                dmgBlink = false;
            }
        });
    }

    protected void defineCullOnEventComponent(CullOnEventComponent c) {
        Set<EventType> s = EnumSet.of(
                EventType.GAME_OVER,
                EventType.PLAYER_SPAWN,
                EventType.BEGIN_ROOM_TRANS,
                EventType.GATE_INIT_OPENING);
        c.preds.add(e -> s.contains(e.type));
    }

    @Override
    public Set<Class<? extends Damager>> getDamagerMaskSet() {
        return dmgNegs.keySet();
    }

    public boolean hasHealth(int health) {
        return getHealth() == health;
    }

    public int getHealth() {
        return getComponent(HealthComponent.class).getHealth();
    }

    public boolean isDamaged() {
        return !dmgTimer.isFinished();
    }

    @Override
    public boolean isInvincible() {
        return !dmgTimer.isFinished();
    }

    @Override
    public void takeDamageFrom(Damager damager) {
        DamageNegotiation dmgNeg = dmgNegs.get(damager.getClass());
        if (dmgNeg == null) {
            return;
        }
        dmgTimer.reset();
        dmgNeg.runOnDamage();
        getComponent(HealthComponent.class).translateHealth(-dmgNeg.getDamage(damager));
        getComponent(SoundComponent.class).requestToPlay(SoundAsset.ENEMY_DAMAGE_SOUND);
    }

    public boolean is(BodySense sense) {
        return body.is(sense);
    }

    protected void disintegrate() {
        game.getAudioMan().playMusic(SoundAsset.ENEMY_DAMAGE_SOUND);
        game.getGameEngine().spawn(game.getEntityFactories()
                .fetch(EntityType.EXPLOSION, ExplosionFactory.DISINTEGRATION), body.getCenter());
    }

    protected void explode() {
        game.getAudioMan().playMusic(SoundAsset.EXPLOSION_SOUND);
        game.getGameEngine().spawn(game.getEntityFactories()
                .fetch(EntityType.EXPLOSION, ExplosionFactory.EXPLOSION), body.getCenter());
    }

    protected boolean isPlayerShootingAtMe() {
        Megaman m = game.getMegaman();
        if (!m.isShooting()) {
            return false;
        }
        BodyComponent bc = getComponent(BodyComponent.class);
        BodyComponent mbc = m.getComponent(BodyComponent.class);
        return (bc.body.bounds.x < mbc.body.bounds.x && m.is(Facing.LEFT)) ||
                (bc.body.bounds.x > mbc.body.bounds.x && m.is(Facing.RIGHT));
    }

}
