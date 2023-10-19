package com.megaman.maverick.game.world

import com.engine.IGame2D
import com.engine.common.extensions.objectSetOf
import com.engine.world.Contact
import com.engine.world.Fixture
import com.engine.world.IContactListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.IEnemyEntity
import com.megaman.maverick.game.entities.contracts.IUpsideDownable
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.VelocityAlterator
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask

@Suppress("UNCHECKED_CAST")
class ContactListener(private val game: IGame2D) : IContactListener {

  override fun beginContact(contact: Contact, delta: Float) {
    // consumer
    if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
      val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER)!!

      (consumer.getProperty(ConstKeys.CONSUMER) as (Fixture) -> Unit)(consumable)
    }

    // damager, damageable
    else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
      val (damager, damageable) =
          contact.getFixturesInOrder(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)!!

      damageable.setDamagedBy(damager)
    }

    // block, side
    else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.SIDE)) {
      val (block, side) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.SIDE)!!

      if (block.bodyHasLabel(BodyLabel.NO_SIDE_TOUCHIE)) return

      val body = side.getBody()
      val sideType =
          side.getProperty(
              ConstKeys.SIDE,
          )

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
    }

    // side, gate
    // TODO: implement gate entity
    /*
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.GATE)) {
      val (side, gate) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.GATE)!!
      if (gate.isState(Gate.GateState.OPENABLE)) gate.trigger()
    }
     */

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType =
          side.getProperty(
              ConstKeys.SIDE,
          )

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
    }

    // feet, block
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
      val (feet, block) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

      val body = feet.getBody()
      val posDelta = block.getBody().positionDelta

      body.x += posDelta.x
      body.y += posDelta.y

      val entity = feet.getEntity()
      if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

      body.setBodySense(BodySense.FEET_ON_GROUND, true)
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, true)
    }

    // bouncer, feet or head or side
    else if (contact.fixturesMatch(
        objectSetOf(FixtureType.BOUNCER),
        objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE))) {
      val (bouncer, bounceable) =
          contact.getFixturesInOrder(
              objectSetOf(FixtureType.BOUNCER),
              objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE))!!

      val bounceableBody = bounceable.getBody()
      val bounce = bouncer.getVelocityAlteration()
      VelocityAlterator.alterate(bounceableBody, bounce)

      val onBounce = bouncer.getRunnable()
      onBounce?.invoke()
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      val (head, block) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      if (!block.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
        val body = head.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)

        body.physics.velocity.y = 0f
      }
    }

    // water listener, water
    else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER_LISTENER)) {
      val (listener, _) =
          contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!

      val body = listener.getBody()
      body.setBodySense(BodySense.IN_WATER, true)

      val entity = listener.getEntity()
      if (entity is Megaman &&
          !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
          !entity.isBehaviorActive(BehaviorType.WALL_SLIDING))
          entity.aButtonTask = AButtonTask.SWIM

      // TODO: Splash.generate

      if (entity is Megaman || entity is IEnemyEntity)
          game.audioMan.playSound(SoundAsset.SPLASH_SOUND.source, false)
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      val (head, ladder) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)

      body.properties.put(ConstKeys.LADDER, ladder)
    }

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      val (feet, ladder) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)

      body.properties.put(ConstKeys.LADDER, ladder)
    }

    // body, force
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
      val (body, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

      val alterableBody = body.getBody()
      val forceAlteration = force.getVelocityAlteration()
      VelocityAlterator.alterate(alterableBody, forceAlteration, delta)
    }

    // body, upside down
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.UPSIDE_DOWN)) {
      val (body, _) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.UPSIDE_DOWN)!!

      val entity = body.getEntity()
      if (entity is IUpsideDownable) entity.upsideDown = true
    }

    // projectile, block or body or shield or water
    else if (contact.fixturesMatch(
        objectSetOf(FixtureType.PROJECTILE),
        objectSetOf(
            FixtureType.BLOCK, FixtureType.BODY, FixtureType.SHIELD, FixtureType.WATER_LISTENER))) {
      val (projectile, other) =
          contact.getFixturesInOrder(
              objectSetOf(FixtureType.PROJECTILE),
              objectSetOf(
                  FixtureType.BLOCK,
                  FixtureType.BODY,
                  FixtureType.SHIELD,
                  FixtureType.WATER_LISTENER))!!

      val projectileEntity = projectile.getEntity() as IProjectileEntity

      when (other.fixtureLabel) {
        FixtureType.BLOCK -> projectileEntity.hitBlock(other)
        FixtureType.BODY -> projectileEntity.hitBody(other)
        FixtureType.SHIELD -> projectileEntity.hitShield(other)
        FixtureType.WATER_LISTENER -> projectileEntity.hitWater(other)
      }
    }

    // player, item
    else if (contact.fixturesMatch(FixtureType.PLAYER, FixtureType.ITEM)) {
      val (player, item) = contact.getFixturesInOrder(FixtureType.PLAYER, FixtureType.ITEM)!!

      val playerEntity = player.getEntity()
      val itemEntity = item.getEntity()

      if (playerEntity is Megaman && itemEntity is ItemEntity)
          itemEntity.contactWithPlayer(playerEntity)
    }
  }

  override fun continueContact(contact: Contact, delta: Float) {}

  override fun endContact(contact: Contact, delta: Float) {}
}

/*
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
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true);
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
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true);
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
            feetBody.setBodySense(BodySense.FEET_ON_GROUND, true);
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
            contact.mask1stBody().setBodySense(BodySense.FEET_ON_ICE, true);
        }

        // force and feet, head, or side
        else if (contact.acceptMask(FixtureType.BOUNCER, w,
                FixtureType.FEET,
                FixtureType.HEAD,
                FixtureType.SIDE) &&
                contact.mask1stEntity() instanceof Bouncer b) {
            BounceDef bounceDef = b.force(contact.mask.getSecond());
            Vector2 force = bounceDef.force;
            Body bounceableBody = contact.mask2ndBody();
            if (bounceDef.xAction == VelocityAlterationType.SET) {
                bounceableBody.velocity.x = force.x;
            } else {
                bounceableBody.velocity.x += force.x;
            }
            if (bounceDef.yAction == VelocityAlterationType.SET) {
                bounceableBody.velocity.y = force.y;
            } else {
                bounceableBody.velocity.y += force.y;
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
                headBody.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true);
                headBody.velocity.y = 0f;
            }
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().setBodySense(BodySense.BODY_IN_WATER, true);
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
            headBody.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true);
            headBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
        }

        // feet and ladder
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.LADDER)) {
            Body feetBody = contact.mask1stBody();
            feetBody.setBodySense(BodySense.FEET_TOUCHING_LADDER, true);
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
                contact.mask1stEntity() instanceof IUpsideDownable u) {
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
            feetBody.setBodySense(BodySense.FEET_ON_GROUND, true);
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
                feetBody.setBodySense(BodySense.FEET_TOUCHING_LADDER, true);
                feetBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
            }
        }

        // head and ladder
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.LADDER)) {
            Vector2 headTopCenter = ShapeUtils.getTopCenterPoint((Rectangle) contact.mask1stShape());
            if (contact.mask2ndShape().contains(headTopCenter)) {
                Body headBody = contact.mask1stBody();
                headBody.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true);
                headBody.putUserData(SpecialFactory.LADDER, contact.mask2ndEntity());
            }
        }

        // head and block
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.BLOCK)) {
            if (!contact.mask2ndBody().labels.contains(BodyLabel.COLLIDE_DOWN_ONLY)) {
                contact.mask1stBody().setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true);
            }
        }

        // feet and ice
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.ICE)) {
            // TODO: test
            // contact.mask1stBody().resistance.x = .925f;

            Body feetBody = contact.mask1stBody();
            feetBody.setBodySense(BodySense.FEET_ON_ICE, true);
            feetBody.resistance.x = 1.0175f;
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().setBodySense(BodySense.BODY_IN_WATER, true);
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
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true);
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
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true);
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
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false);
            }
        }

        // side and ice
        else if (contact.acceptMask(FixtureType.SIDE, FixtureType.ICE)) {
            Body body = contact.mask1stBody();
            String side = contact.mask1stData(ConstKeys.SIDE, String.class);
            if (side.equals(ConstKeys.LEFT)) {
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false);
            } else {
                body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false);
            }
        }

        // feet and block
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.BLOCK)) {
            contact.mask1stBody().setBodySense(BodySense.FEET_ON_GROUND, false);
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = m.is(BodySense.BODY_IN_WATER) ? AButtonTask.SWIM : AButtonTask.AIR_DASH;
            }
        }

        // feet and ice
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.ICE)) {
            contact.mask1stBody().setBodySense(BodySense.FEET_ON_ICE, false);
        }

        // head and block
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.BLOCK)) {
            contact.mask1stBody().setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false);
        }

        // water listener and water
        else if (contact.acceptMask(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            contact.mask1stBody().setBodySense(BodySense.BODY_IN_WATER, false);
            if (contact.mask1stEntity() instanceof Megaman m) {
                m.aButtonTask = AButtonTask.AIR_DASH;
            }
            game.getAudioMan().playMusic(SoundAsset.SPLASH_SOUND);
            Splash.generate(game, contact.mask1stBody(), contact.mask2ndBody());
        }

        // head and ladder
        else if (contact.acceptMask(FixtureType.HEAD, FixtureType.LADDER)) {
            Body headBody = contact.mask1stBody();
            headBody.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false);
            if (!headBody.is(BodySense.FEET_TOUCHING_LADDER)) {
                headBody.removeUserData(SpecialFactory.LADDER);
            }
        }

        // feet and ladder
        else if (contact.acceptMask(FixtureType.FEET, FixtureType.LADDER)) {
            Body feetBody = contact.mask1stBody();
            feetBody.setBodySense(BodySense.FEET_TOUCHING_LADDER, false);
            if (!feetBody.is(BodySense.HEAD_TOUCHING_LADDER)) {
                feetBody.removeUserData(SpecialFactory.LADDER);
            }
        }

        // body and upside-down
        else if (contact.acceptMask(FixtureType.BODY, FixtureType.UPSIDE_DOWN) &&
                contact.mask1stEntity() instanceof IUpsideDownable u) {
            u.setUpsideDown(false);
        }
    }

}



 */
