package com.megaman.game.world;

import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.megaman.game.ConstKeys;
import com.megaman.game.MegamanGame;
import com.megaman.game.assets.SoundAsset;
import com.megaman.game.behaviors.BehaviorType;
import com.megaman.game.entities.Entity;
import com.megaman.game.entities.utils.bounce.BounceAction;
import com.megaman.game.entities.utils.bounce.BounceDef;
import com.megaman.game.entities.utils.damage.Damageable;
import com.megaman.game.entities.utils.damage.Damager;
import com.megaman.game.entities.impl.decorations.impl.Splash;
import com.megaman.game.entities.impl.enemies.Enemy;
import com.megaman.game.entities.impl.items.Item;
import com.megaman.game.entities.impl.megaman.Megaman;
import com.megaman.game.entities.impl.megaman.vals.AButtonTask;
import com.megaman.game.entities.impl.projectiles.Projectile;
import com.megaman.game.entities.impl.sensors.impl.Gate;
import com.megaman.game.entities.impl.special.SpecialFactory;
import com.megaman.game.entities.utils.bounce.Bouncer;
import com.megaman.game.entities.utils.special.UpsideDownable;
import com.megaman.game.health.HealthComponent;
import com.megaman.game.shapes.ShapeUtils;
import com.megaman.game.utils.Logger;
import com.megaman.game.utils.interfaces.UpdateFunc;
import com.megaman.game.utils.objs.Wrapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class WorldContactListenerImpl implements WorldContactListener {

    private static final Logger logger = new Logger(WorldContactListener.class, MegamanGame.DEBUG && true);

    private final MegamanGame game;

    @Override
    public void beginContact(Contact contact, float delta) {
        Wrapper<FixtureType> w = Wrapper.empty();
        // consumer
        if (contact.acceptMask(FixtureType.CONSUMER, false)) {
            Consumer<Fixture> c = (Consumer<Fixture>) contact.mask1stData(ConstKeys.CONSUMER);
            c.accept(contact.mask.getSecond());
        }

        // death and damageable
        else if (contact.acceptMask(FixtureType.DEATH, FixtureType.DAMAGEABLE)) {
            contact.mask2ndEntity().getComponent(HealthComponent.class).setDead();
        }

        // damager and damageable
        else if (contact.acceptMask(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            Damager dmgr = (Damager) contact.mask1stEntity();
            Damageable dmgbl = (Damageable) contact.mask2ndEntity();
            if (dmgr.canDamage(dmgbl) && dmgbl.canBeDamagedBy(dmgr)) {
                dmgbl.takeDamageFrom(dmgr);
                dmgr.onDamageInflictedTo(dmgbl);
            }
        }

        // side and block
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.BLOCK)) {
            if (contact.mask2ndBody().labels.contains(BodyLabel.NO_SIDE_TOUCHIE)) {
                return;
            }
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true);
            } else {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true);
            }
        }

        // side and gate
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.GATE)) {
            if (contact.mask1stEntity() instanceof Megaman &&
                    contact.mask2ndEntity() instanceof Gate gate &&
                    gate.isState(Gate.GateState.OPENABLE)) {
                gate.trigger();
            }
        }

        // side and ice
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.ICE)) {
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_ICE_LEFT, true);
            } else {
                body.set(BodySense.SIDE_TOUCHING_ICE_RIGHT, true);
            }
        }

        // feet and block
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.BLOCK)) {
            Body feetBody = contact.mask1stBody();
            Vector2 posDelta = contact.mask2ndBody().getPosDelta();
            feetBody.bounds.x += posDelta.x;
            feetBody.bounds.y += posDelta.y;
            // TODO: test
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = AButtonTask.JUMP;
                if (contact.mask1stShape() instanceof Rectangle feet &&
                        contact.mask2ndShape().contains(ShapeUtils.getBottomCenterPoint(feet))) {
                    m.request(SoundAsset.MEGAMAN_LAND_SOUND, true);
                }
            }
            feetBody.set(BodySense.FEET_ON_GROUND, true);
            /*
            if (!feetBody.is(BodySense.FEET_ON_GROUND) &&
                    contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = AButtonTask.JUMP;
                m.request(SoundAsset.MEGAMAN_LAND_SOUND, true);
            }
             */
        }

        // feet and ice
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.ICE)) {
            contact.mask1stBody().set(BodySense.FEET_ON_ICE, true);
        }

        // bounce and feet, head, or side
        else if (contact.acceptMask(FixtureType.BOUNCER, w,
                FixtureType.FEET,
                FixtureType.HEAD,
                FixtureType.SIDE) &&
                contact.mask1stEntity() instanceof Bouncer b) {
            BounceDef bounceDef = b.bounce(contact.mask.getSecond());
            Vector2 bounce = bounceDef.bounce;
            Body bounceableBody = contact.mask2ndBody();
            if (bounceDef.xAction == BounceAction.SET) {
                bounceableBody.velocity.x = bounce.x;
            } else {
                bounceableBody.velocity.x += bounce.x;
            }
            if (bounceDef.yAction == BounceAction.SET) {
                bounceableBody.velocity.y = bounce.y;
            } else {
                bounceableBody.velocity.y += bounce.y;
            }
            Runnable onBounce = contact.mask2ndData(ConstKeys.RUN, Runnable.class);
            if (onBounce != null) {
                onBounce.run();
            }
        }

        // head and block
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.BLOCK)) {
            if (!contact.mask2ndBody().labels.contains(BodyLabel.COLLIDE_DOWN_ONLY)) {
                Body headBody = contact.mask1stBody();
                headBody.set(BodySense.HEAD_TOUCHING_BLOCK, true);
                headBody.velocity.y = 0f;
            }
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().set(BodySense.BODY_IN_WATER, true);
            Entity e = contact.mask1stEntity();
            if (e instanceof Megaman m && !m.is(BodySense.FEET_ON_GROUND) && !m.is(BehaviorType.WALL_SLIDING)) {
                m.aButtonTask = AButtonTask.SWIM;
            }
            Splash.generate(game, contact.mask1stBody(), contact.mask2ndBody());
            if (e instanceof Megaman || e instanceof Enemy) {
                game.getAudioMan().playMusic(SoundAsset.SPLASH_SOUND);
            }
        }

        // head and ladder
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.LADDER)) {
            Body headBody = contact.mask1stBody();
            headBody.set(BodySense.HEAD_TOUCHING_LADDER, true);
            headBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
        }

        // feet and ladder
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.LADDER)) {
            Body feetBody = contact.mask1stBody();
            feetBody.set(BodySense.FEET_TOUCHING_LADDER, true);
            feetBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
        }

        // body and force
        else if (contact.acceptMask(FixtureType.BODY, FixtureType.FORCE)) {

            // TODO: test

            /*
            Vector2 force = ((Function<Fixture, Vector2>) contact.mask2ndData(ConstKeys.FUNCTION))
                    .apply(contact.mask.getFirst());
            contact.mask1stBody().velocity.add(force);
             */

            /*
            Function<Float, Vector2> forceFunc = (Function<Float, Vector2>) contact.mask2ndData(ConstKeys.FUNCTION);
            Vector2 force = forceFunc.apply(delta);
            contact.mask1stBody().velocity.add(force);
             */

            UpdateFunc<Fixture, Vector2> forceFunc = (UpdateFunc<Fixture, Vector2>)
                    contact.mask2ndData(ConstKeys.FUNCTION);
            Vector2 force = forceFunc.apply(contact.mask.getFirst(), delta);
            contact.mask1stBody().velocity.add(force);

        }

        // body and upside-down
        else if (contact.acceptMask(FixtureType.BODY, FixtureType.UPSIDE_DOWN) &&
                contact.mask1stEntity() instanceof UpsideDownable u) {
            u.setUpsideDown(true);
        }

        // projectile and block. body, shield, or water
        else if (contact.acceptMask(FixtureType.PROJECTILE, w,
                FixtureType.BLOCK,
                FixtureType.BODY,
                FixtureType.SHIELD,
                FixtureType.WATER)) {
            if (contact.mask2ndBody().labels.contains(BodyLabel.NO_PROJECTILE_COLLISION)) {
                return;
            }
            Projectile p = (Projectile) contact.mask1stEntity();
            Fixture f = contact.mask.getSecond();
            switch (w.data) {
                case BLOCK -> p.hitBlock(f);
                case BODY -> p.hitBody(f);
                case SHIELD -> p.hitShield(f);
                case WATER -> p.hitWater(f);
            }
        }

        // player and item
        else if (contact.acceptMask(FixtureType.PLAYER, FixtureType.ITEM)) {
            if (contact.mask1stEntity() instanceof Megaman m && contact.mask2ndEntity() instanceof Item i) {
                i.contactWithPlayer(m);
            }
        }
    }

    @Override
    public void continueContact(Contact contact, float delta) {

        // consumer
        if (contact.acceptMask(FixtureType.CONSUMER, false)) {
            Consumer<Fixture> c = (Consumer<Fixture>) contact.mask1stData(ConstKeys.CONSUMER);
            c.accept(contact.mask.getSecond());
        }

        // damager and damageable
        else if (contact.acceptMask(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            Damager dmgr = (Damager) contact.mask1stEntity();
            Damageable dmgbl = (Damageable) contact.mask2ndEntity();
            if (dmgr.canDamage(dmgbl) && dmgbl.canBeDamagedBy(dmgr)) {
                dmgbl.takeDamageFrom(dmgr);
                dmgr.onDamageInflictedTo(dmgbl);
            }
        }

        // feet and block
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.BLOCK)) {

            // TODO: test
            /*
            Vector2 posDelta;
            if (contact.mask2ndEntity().hasComponent(TrajectoryComponent.class)) {
                posDelta = contact.mask2ndEntity().getComponent(TrajectoryComponent.class).trajectory.getPosDelta();
            } else {
                posDelta = contact.mask2ndBody().getPosDelta();
            }
             */

            Body feetBody = contact.mask1stBody();
            feetBody.set(BodySense.FEET_ON_GROUND, true);
            Vector2 posDelta = contact.mask2ndBody().getPosDelta();
            feetBody.bounds.x += posDelta.x;
            feetBody.bounds.y += posDelta.y;
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = AButtonTask.JUMP;
            }
        }

        // feet and ladder
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.LADDER)) {
            Vector2 feetBottomCenter = ShapeUtils.getBottomCenterPoint((Rectangle) contact.mask1stShape());
            if (contact.mask2ndShape().contains(feetBottomCenter)) {
                Body feetBody = contact.mask1stBody();
                feetBody.set(BodySense.FEET_TOUCHING_LADDER, true);
                feetBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
            }
        }

        // head and ladder
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.LADDER)) {
            Vector2 headTopCenter = ShapeUtils.getTopCenterPoint((Rectangle) contact.mask1stShape());
            if (contact.mask2ndShape().contains(headTopCenter)) {
                Body headBody = contact.mask1stBody();
                headBody.set(BodySense.HEAD_TOUCHING_LADDER, true);
                headBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
            }
        }

        // head and block
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.BLOCK)) {
            if (!contact.mask2ndBody().labels.contains(BodyLabel.COLLIDE_DOWN_ONLY)) {
                contact.mask1stBody().set(BodySense.HEAD_TOUCHING_BLOCK, true);
            }
        }

        // feet and ice
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.ICE)) {
            // TODO: test
            // contact.mask1stBody().resistance.x = .925f;

            Body feetBody = contact.mask1stBody();
            feetBody.set(BodySense.FEET_ON_ICE, true);
            feetBody.resistance.x = 1.0175f;
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().set(BodySense.BODY_IN_WATER, true);
            if (contact.mask1stEntity() instanceof Megaman m &&
                    !m.is(BodySense.FEET_ON_GROUND) &&
                    !m.is(BehaviorType.WALL_SLIDING)) {
                m.aButtonTask = AButtonTask.SWIM;
            }
        }

        // body and force
        else if (contact.acceptMask(FixtureType.BODY, FixtureType.FORCE)) {

            // TODO: test

            /*
            Function<Fixture, Vector2> forceFunc = (Function<Fixture, Vector2>) contact.mask2ndData(ConstKeys.FUNCTION);
            Vector2 force = forceFunc.apply(contact.mask.getFirst());
            contact.mask1stBody().velocity.add(force);
             */

            /*
            Function<Float, Vector2> forceFunc = (Function<Float, Vector2>) contact.mask2ndData(ConstKeys.FUNCTION);
            Vector2 force = forceFunc.apply(delta);
            contact.mask1stBody().velocity.add(force);
             */

            UpdateFunc<Fixture, Vector2> forceFunc = (UpdateFunc<Fixture, Vector2>)
                    contact.mask2ndData(ConstKeys.FUNCTION);
            Vector2 force = forceFunc.apply(contact.mask.getFirst(), delta);
            contact.mask1stBody().velocity.add(force);

        }

        // laser and block
        else if (contact.acceptMask(FixtureType.LASER, FixtureType.BLOCK) &&
                !contact.mask1stEntity().equals(contact.mask2ndEntity())) {
            Fixture first = contact.mask.getFirst();
            Fixture second = contact.mask.getSecond();
            Collection<Vector2> contactPoints = first.getUserData(ConstKeys.COLLECTION, Collection.class);
            Collection<Vector2> temp = new ArrayList<>();
            if (ShapeUtils.intersectLineRect((Polyline) first.shape, (Rectangle) second.shape, temp)) {
                contactPoints.addAll(temp);
            }
        }

        // side and ice
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.ICE)) {
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_ICE_LEFT, true);
            } else {
                body.set(BodySense.SIDE_TOUCHING_ICE_RIGHT, true);
            }
        }

        // side and block
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.BLOCK)) {
            if (contact.mask2ndBody().labels.contains(BodyLabel.NO_SIDE_TOUCHIE)) {
                return;
            }
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true);
            } else {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true);
            }
        }
    }

    @Override
    public void endContact(Contact contact, float delta) {
        // side and block
        if (contact.acceptMask(FixtureType.SIDE, FixtureType.BLOCK)) {
            if (contact.mask2ndBody().labels.contains(BodyLabel.NO_SIDE_TOUCHIE)) {
                return;
            }
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false);
            } else {
                body.set(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false);
            }
        }

        // side and ice
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.ICE)) {
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.set(BodySense.SIDE_TOUCHING_ICE_LEFT, false);
            } else {
                body.set(BodySense.SIDE_TOUCHING_ICE_RIGHT, false);
            }
        }

        // feet and block
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.BLOCK)) {
            contact.mask1stBody().set(BodySense.FEET_ON_GROUND, false);
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = m.is(BodySense.BODY_IN_WATER) ? AButtonTask.SWIM : AButtonTask.AIR_DASH;
            }
        }

        // feet and ice
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.ICE)) {
            contact.mask1stBody().set(BodySense.FEET_ON_ICE, false);
        }

        // head and block
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.BLOCK)) {
            contact.mask1stBody().set(BodySense.HEAD_TOUCHING_BLOCK, false);
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().set(BodySense.BODY_IN_WATER, false);
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = AButtonTask.AIR_DASH;
            }
            game.getAudioMan().playMusic(SoundAsset.SPLASH_SOUND);
            Splash.generate(game, contact.mask1stBody(), contact.mask2ndBody());
        }

        // head and ladder
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.LADDER)) {
            Body headBody = contact.mask1stBody();
            headBody.set(BodySense.HEAD_TOUCHING_LADDER, false);
            if (!headBody.is(BodySense.FEET_TOUCHING_LADDER)) {
                headBody.removeUserData(SpecialFactory.LADDER);
            }
        }

        // feet and ladder
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.LADDER)) {
            Body feetBody = contact.mask1stBody();
            feetBody.set(BodySense.FEET_TOUCHING_LADDER, false);
            if (!feetBody.is(BodySense.HEAD_TOUCHING_LADDER)) {
                feetBody.removeUserData(SpecialFactory.LADDER);
            }
        }

        // body and upside-down
        else if (contact.acceptMask(FixtureType.BODY, FixtureType.UPSIDE_DOWN) &&
                contact.mask1stEntity() instanceof UpsideDownable u) {
            u.setUpsideDown(false);
        }
    }

}


