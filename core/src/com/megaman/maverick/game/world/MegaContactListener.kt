package com.megaman.maverick.game.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.IGame2D
import com.engine.common.extensions.objectSetOf
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.ShapeUtils
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
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.utils.VelocityAlterator

/** A contact listener for the game. */
@Suppress("UNCHECKED_CAST")
class MegaContactListener(private val game: IGame2D) : IContactListener {

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
      val sideType = side.getProperty(ConstKeys.SIDE)

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

  override fun continueContact(contact: Contact, delta: Float) {
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

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      val (feet, ladder) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      if (ladder.shape.contains(feet.shape.getBoundingRectangle().getBottomCenterPoint())) {
        val body = feet.getBody()
        body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
        body.properties.put(ConstKeys.LADDER, ladder)
      }
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      val (head, ladder) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      if (ladder.shape.contains(head.shape.getBoundingRectangle().getTopCenterPoint())) {
        val body = head.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
        body.properties.put(ConstKeys.LADDER, ladder)
      }
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      val (head, block) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      if (block.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) {
        val body = head.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
      }
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, true)
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
    }

    // body, force
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
      val (body, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

      val alterableBody = body.getBody()
      val forceAlteration = force.getVelocityAlteration()
      VelocityAlterator.alterate(alterableBody, forceAlteration, delta)
    }

    // laser, block
    else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BLOCK)) {
      val (laser, block) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK)!!

      val laserEntity = laser.getEntity()
      val blockEntity = block.getEntity()

      if (laserEntity != blockEntity) {
        val blockRectangle = block.shape as GameRectangle
        val laserLine = laser.shape as GameLine

        val intersections = laser.properties.get(ConstKeys.ARRAY) as Array<Vector2>
        ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, intersections)
      }
    }

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
    }

    // side, block
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
      val (side, block) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK)!!

      if (block.bodyHasLabel(BodyLabel.NO_SIDE_TOUCHIE)) return

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
    }
  }

  override fun endContact(contact: Contact, delta: Float) {
    // side, block
    if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
    }

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
    }

    // feet, block
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_GROUND, false)

      val entity = feet.getEntity()
      if (entity is Megaman)
          entity.aButtonTask =
              if (entity.body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM
              else AButtonTask.AIR_DASH
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, false)
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
    }

    // water listener, water
    else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER_LISTENER)) {
      val (listener, _) =
          contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!

      val body = listener.getBody()
      body.setBodySense(BodySense.IN_WATER, false)

      val entity = listener.getEntity()
      if (entity is Megaman)
          entity.aButtonTask =
              if (entity.body.isSensing(BodySense.FEET_ON_GROUND)) AButtonTask.JUMP
              else AButtonTask.AIR_DASH

      game.audioMan.playSound(SoundAsset.SPLASH_SOUND.source, false)
      // TODO: generate Splash
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)
      body.properties.remove(ConstKeys.LADDER)
    }

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)
      body.properties.remove(ConstKeys.LADDER)
    }

    // body, upside-down
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.UPSIDE_DOWN)) {
      val (body, _) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.UPSIDE_DOWN)!!

      val entity = body.getEntity()
      if (entity is IUpsideDownable) entity.upsideDown = false
    }
  }
}