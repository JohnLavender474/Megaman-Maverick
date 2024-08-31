package com.megaman.maverick.game.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.ProcessState
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.isFacing
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.ShapeUtils
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.world.Contact
import com.engine.world.IContactListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.entities.sensors.Gate.GateState
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.entities.special.PolygonWater
import com.megaman.maverick.game.entities.special.Water
import com.megaman.maverick.game.utils.VelocityAlterator

class MegaContactListener(
    private val game: MegamanMaverickGame, private val contactDebugFilter: (Contact) -> Boolean
) : IContactListener {

    companion object {
        const val TAG = "MegaContactListener"
        const val FEET_ON_ICE_FRICTION = 1.0175f
        const val FEET_ON_SAND_FRICTION = 1.25f
    }

    private fun printDebugLog(contact: Contact, log: String) {
        if (contactDebugFilter.invoke(contact)) {
            GameLogger.debug(TAG, log)
        }
    }

    override fun beginContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            printDebugLog(contact, "beginContact(): Consumer, contact = $contact")
            val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER)!!
            consumer.getConsumer()?.invoke(ProcessState.BEGIN, consumable)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            printDebugLog(contact, "beginContact(): Damager-Damageable, contact = $contact")
            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE
            )!!

            val damager = damagerFixture.getEntity() as IDamager
            val damageable = damageableFixture.getEntity() as IDamageable

            val canBeDamaged = damageable.canBeDamagedBy(damager)
            printDebugLog(contact, "canBeDamaged=$canBeDamaged")
            val canDamage = damager.canDamage(damageable)
            printDebugLog(contact, "canDamage=$canDamage")
            if (canBeDamaged && canDamage) {
                val takeDamageFrom = damageable.takeDamageFrom(damager)
                printDebugLog(contact, "takeDamageFrom=$takeDamageFrom")
                damager.onDamageInflictedTo(damageable)
            }
        }

        // death, feet / side / head / body
        else if (contact.fixtureSetsMatch(
                objectSetOf(FixtureType.DEATH), objectSetOf(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )
        ) {
            val (deathFixture, otherFixture) = contact.getFixtureSetsInOrder(
                objectSetOf(FixtureType.DEATH), objectSetOf(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )!!

            val entity = otherFixture.getEntity()

            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTITY_CAN_DIE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.getProperty(ConstKeys.INSTANT, Boolean::class) ?: false
            if (entity is IDamageable && (instant || !entity.invincible)) otherFixture.depleteHealth()
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Block-Body, contact = $contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val block = blockFixture.getEntity() as Block
            block.hitByBody(bodyFixture)

            if (bodyFixture.hasHitByBlockReceiver()) bodyFixture.getHitByBlock(block)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // body, body
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Body-Body, contact = $contact")
            val (bodyFixture1, bodyFixture2) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.BODY)!!

            if (bodyFixture1.hasHitByBodyReceiver()) bodyFixture1.getHitByBody(bodyFixture2.getEntity())
            if (bodyFixture2.hasHitByBodyReceiver()) bodyFixture2.getHitByBody(bodyFixture1.getEntity())
        }

        // side, block
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Block-Side, contact = $contact")
            val (blockFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.SIDE)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val body = sideFixture.getBody()
            val sideType = sideFixture.getProperty(ConstKeys.SIDE)

            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)

            val block = blockFixture.getEntity() as Block
            block.hitBySide(sideFixture)

            if (sideFixture.hasHitByBlockReceiver()) sideFixture.getHitByBlock(block)
        }

        // side / feet / head, gate
        else if (contact.fixtureSetsMatch(
                objectSetOf(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD), objectSetOf(FixtureType.GATE)
            )
        ) {
            printDebugLog(contact, "beginContact(): Side/Feet/Head-Gate")
            val (other, gateFixture) = contact.getFixtureSetsInOrder(
                objectSetOf(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD), objectSetOf(FixtureType.GATE)
            )!!
            val entity = other.getEntity()
            if (entity is Megaman) {
                val gate = gateFixture.getEntity() as Gate
                if (gate.state == GateState.OPENABLE) gate.trigger()
            }
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Side-Ice, contact = $contact")
            val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

            val body = side.getBody()
            val sideType = side.getProperty(ConstKeys.SIDE)

            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Feet-Block, contact = $contact")
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!
            val body = feetFixture.getBody()

            if (!blockFixture.getBody().physics.collisionOn) {
                body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            val stickToBlock = feetFixture.getOrDefaultProperty(ConstKeys.STICK_TO_BLOCK, true, Boolean::class)
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.x += posDelta.x
                body.y += posDelta.y
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            val block = blockFixture.getEntity() as Block
            block.hitByFeet(feetFixture)

            if (feetFixture.hasHitByBlockReceiver()) feetFixture.getHitByBlock(block)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Feet-Ice, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            body.physics.frictionOnSelf.set(FEET_ON_ICE_FRICTION, FEET_ON_ICE_FRICTION)
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "beginContact(): Feet-Sand, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, true)

            body.physics.frictionOnSelf.set(FEET_ON_SAND_FRICTION, FEET_ON_SAND_FRICTION)
        }

        // bouncer, feet or head or side
        else if (contact.fixtureSetsMatch(
                objectSetOf(FixtureType.BOUNCER), objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE)
            )
        ) {
            printDebugLog(contact, "beginContact(): Bouncer-Feet/Head/Side, contact = $contact")
            val (bouncer, bounceable) = contact.getFixtureSetsInOrder(
                objectSetOf(FixtureType.BOUNCER), objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE)
            )!!

            val bounce = bouncer.getVelocityAlteration(bounceable, delta)
            VelocityAlterator.alterate(bounceable.getBody(), bounce)

            bouncer.getRunnable()?.invoke()
            bounceable.getRunnable()?.invoke()
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Head-Block, contact = $contact")
            val (head, block) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

            if (block.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

            val body = head.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            body.physics.velocity.y = 0f

            val blockEntity = block.getEntity() as Block
            blockEntity.hitByHead(head)

            if (head.hasConsumer()) {
                val consumer = head.getConsumer()
                consumer?.invoke(ProcessState.BEGIN, block)
            }
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            printDebugLog(contact, "beginContact(): WaterListener-Water, contact = $contact")
            val (listenerFixture, waterFixture) = contact.getFixturesInOrder(
                FixtureType.WATER_LISTENER,
                FixtureType.WATER
            )!!

            val body = listenerFixture.getBody()
            body.setBodySense(BodySense.IN_WATER, true)

            val entity = listenerFixture.getEntity()
            if (entity is Megaman && !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
            ) entity.aButtonTask = AButtonTask.SWIM

            Splash.generate(game, listenerFixture.getBody(), waterFixture.getBody())

            if (entity is Megaman) {
                entity.gravityScalar = MegamanValues.WATER_GRAVITY_SCALAR

                val water = waterFixture.getEntity()
                if ((water is Water && water.splashSound) ||
                    (water is PolygonWater && water.splashSound)
                ) game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
            }
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Head-Ladder, contact = $contact")
            val (head, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

            val body = head.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)

            body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Feet-Ladder, contact = $contact")
            val (feetFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)

            body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "beginContact(): Body-Force, contact = $contact")
            val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

            val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
            VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)
        }

        // body, gravity change
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
            val (bodyFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BODY, FixtureType.GRAVITY_CHANGE
            )!!

            val body = bodyFixture.getBody()
            val pd = body.getPositionDelta()
            val bodyPointToCheck = if (pd.x > 0f) {
                if (pd.y > 0f) body.getTopRightPoint()
                else if (pd.y < 0f) body.getBottomRightPoint() else body.getCenterRightPoint()
            } else if (pd.x < 0f) {
                if (body.getPositionDelta().y > 0f) body.getTopLeftPoint()
                else if (body.getPositionDelta().y < 0f) body.getBottomLeftPoint()
                else body.getCenterLeftPoint()
            } else {
                if (pd.y > 0f) body.getTopCenterPoint()
                else if (pd.y < 0f) body.getBottomCenterPoint() else body.getCenter()
            }
            if (!gravityChangeFixture.getShape().contains(bodyPointToCheck)) return

            val entity = bodyFixture.getEntity()
            if (gravityChangeFixture.hasProperty(ConstKeys.GRAVITY) && entity is IScalableGravityEntity) {
                val canChangeGravityValue =
                    bodyFixture.getOrDefaultProperty(ConstKeys.GRAVITY_CHANGEABLE, true, Boolean::class)
                if (canChangeGravityValue) {
                    val scalar = gravityChangeFixture.getProperty(ConstKeys.GRAVITY, Float::class)!!
                    entity.gravityScalar = scalar
                }
            }

            if (gravityChangeFixture.hasProperty(ConstKeys.DIRECTION)) {
                val canChangeGravityRotation =
                    bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class)
                if (canChangeGravityRotation) {
                    val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return
                    if (entity is IDirectionRotatable && entity.directionRotation != direction) entity.directionRotation =
                        direction
                }
            }
        }

        // block, gravity change
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE)) {
            val (blockFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE
            )!!
            val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return
            val canChangeGravity = blockFixture.properties.getOrDefault(
                ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class
            )
            if (!canChangeGravity) return
            if (direction.isHorizontal()) {
                val blockBody = blockFixture.getBody()
                val frictionX = blockBody.physics.frictionToApply.x
                blockBody.physics.frictionToApply.x = blockBody.physics.frictionToApply.y
                blockBody.physics.frictionToApply.y = frictionX
            }
        }

        // body, player
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.PLAYER)) {
            printDebugLog(contact, "beginContact(): Body-Player, contact = $contact")
            val (bodyFixture, playerFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.PLAYER)!!
            if (bodyFixture.hasHitByPlayerReceiver()) bodyFixture.getHitByPlayer(playerFixture.getEntity() as Megaman)
        }

        // projectile, block or body or shield or water or projectile
        else if (contact.fixtureSetsMatch(
                objectSetOf(FixtureType.PROJECTILE), objectSetOf(
                    FixtureType.BLOCK,
                    FixtureType.BODY,
                    FixtureType.SHIELD,
                    FixtureType.WATER,
                    FixtureType.SAND,
                    FixtureType.PROJECTILE
                )
            )
        ) {
            printDebugLog(
                contact, "beginContact(): Projectile-Block/Body/Shield/Water, contact = $contact"
            )
            val (projectileFixture, otherFixture) = contact.getFixtureSetsInOrder(
                objectSetOf(FixtureType.PROJECTILE), objectSetOf(
                    FixtureType.BLOCK,
                    FixtureType.BODY,
                    FixtureType.SHIELD,
                    FixtureType.WATER,
                    FixtureType.SAND,
                    FixtureType.PROJECTILE
                )
            )!!

            if (otherFixture.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)) return

            val projectile = projectileFixture.getEntity() as IProjectileEntity

            if (otherFixture.hasHitByProjectileReceiver()) otherFixture.getHitByProjectile(projectile)

            when (otherFixture.getFixtureType()) {
                FixtureType.BLOCK -> {
                    printDebugLog(contact, "beginContact(): Projectile-Block, contact = $contact")
                    projectile.hitBlock(otherFixture)
                    (otherFixture.getEntity() as Block).hitByProjectile(projectileFixture)
                }

                FixtureType.BODY -> {
                    printDebugLog(contact, "beginContact(): Projectile-Body, contact = $contact")
                    projectile.hitBody(otherFixture)
                }

                FixtureType.SHIELD -> {
                    printDebugLog(contact, "beginContact(): Projectile-Shield, contact = $contact")
                    projectile.hitShield(otherFixture)
                }

                FixtureType.WATER -> {
                    printDebugLog(contact, "beginContact(): Projectile-Water, contact = $contact")
                    projectile.hitWater(otherFixture)
                }

                FixtureType.SAND -> {
                    printDebugLog(contact, "beginContact(): Projectile-Sand, contact = $contact")
                    projectile.hitSand(otherFixture)
                }

                FixtureType.PROJECTILE -> {
                    printDebugLog(contact, "beginContact(): Projectile-Projectile, contact = $contact")
                    projectile.hitProjectile(otherFixture)
                }
            }
        }

        // player, item
        else if (contact.fixturesMatch(FixtureType.PLAYER, FixtureType.ITEM)) {
            printDebugLog(contact, "beginContact(): Player-Item, contact = $contact")
            val (player, item) = contact.getFixturesInOrder(FixtureType.PLAYER, FixtureType.ITEM)!!

            val playerEntity = player.getEntity()
            val itemEntity = item.getEntity()

            if (playerEntity is Megaman && itemEntity is ItemEntity) itemEntity.contactWithPlayer(playerEntity)
        }

        // player feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "beginContact(): Feet-Cart, contact = $contact")
            val (feet, cart) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART)!!

            val feetEntity = feet.getEntity()
            val cartEntity = cart.getEntity() as IOwnable

            if (feetEntity is Megaman && cartEntity is Cart) {
                cartEntity.owner = feetEntity
                feetEntity.body.setBodySense(BodySense.TOUCHING_CART, true)
                feetEntity.body.putProperty(ConstKeys.CART, cartEntity)
            }
        }

        // teleporter listener, teleporter
        else if (contact.fixturesMatch(FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER)) {
            printDebugLog(contact, "beginContact(): TeleporterListener-Teleporter, contact = $contact")
            val (teleporterListenerFixture, teleporterFixture) = contact.getFixturesInOrder(
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER
            )!!

            val teleporterListener = teleporterListenerFixture.getEntity()
            teleporterListener.body.setBodySense(BodySense.TELEPORTING, true)

            val teleporterEntity = teleporterFixture.getEntity() as ITeleporterEntity
            teleporterEntity.teleportEntity(teleporterListener)
        }
    }

    override fun continueContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER)!!
            consumer.getConsumer()?.invoke(ProcessState.CONTINUE, consumable)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE
            )!!

            val damager = damagerFixture.getEntity() as IDamager
            val damageable = damageableFixture.getEntity() as IDamageable

            if (damageable.canBeDamagedBy(damager) && damager.canDamage(damageable)) {
                damageable.takeDamageFrom(damager)
                damager.onDamageInflictedTo(damageable)
            }
        }

        // death, feet / side / head / body
        else if (contact.fixtureSetsMatch(
                objectSetOf(FixtureType.DEATH), objectSetOf(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )
        ) {
            val (deathFixture, otherFixture) = contact.getFixtureSetsInOrder(
                objectSetOf(FixtureType.DEATH), objectSetOf(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )!!

            val entity = otherFixture.getEntity()

            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTITY_CAN_DIE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.getProperty(ConstKeys.INSTANT, Boolean::class) ?: false
            if (entity is IDamageable && (instant || !entity.invincible)) otherFixture.depleteHealth()
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Block-Body, contact = $contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!
            val body = feetFixture.getBody()
            if (!blockFixture.getBody().physics.collisionOn) {
                body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            val stickToBlock = feetFixture.getOrDefaultProperty(ConstKeys.STICK_TO_BLOCK, true, Boolean::class)
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.x += posDelta.x
                body.y += posDelta.y
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

            body.setBodySense(BodySense.FEET_ON_GROUND, true)
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            val (feetFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!
            val feetEntity = feetFixture.getEntity()

            val feetDirection = if (feetEntity is IDirectionRotatable) feetEntity.directionRotation else Direction.UP
            val feetPoint = when (feetDirection!!) {
                Direction.UP -> feetFixture.getShape().getBoundingRectangle().getBottomCenterPoint()
                Direction.DOWN -> feetFixture.getShape().getBoundingRectangle().getTopCenterPoint()
                Direction.LEFT -> feetFixture.getShape().getBoundingRectangle().getCenterRightPoint()
                Direction.RIGHT -> feetFixture.getShape().getBoundingRectangle().getCenterLeftPoint()
            }
            if (ladderFixture.getShape().contains(feetPoint)) {
                val body = feetFixture.getBody()
                body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
                body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
            }
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            val (headFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!
            val headEntity = headFixture.getEntity()

            val headDirection = if (headEntity is IDirectionRotatable) headEntity.directionRotation else Direction.UP
            val headPoint = when (headDirection!!) {
                Direction.UP -> headFixture.getShape().getBoundingRectangle().getTopCenterPoint()
                Direction.DOWN -> headFixture.getShape().getBoundingRectangle().getBottomCenterPoint()
                Direction.LEFT -> headFixture.getShape().getBoundingRectangle().getCenterLeftPoint()
                Direction.RIGHT -> headFixture.getShape().getBoundingRectangle().getCenterRightPoint()
            }
            if (ladderFixture.getShape().contains(headPoint)) {
                val body = headFixture.getBody()
                body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
                body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
            }
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            val (headFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

            if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                if (entity.body.isSensing(BodySense.FEET_ON_GROUND) && (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && entity.isFacing(
                        Facing.LEFT
                    ) || (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && entity.isFacing(Facing.RIGHT)))
                ) return
            }

            body.physics.frictionOnSelf.set(FEET_ON_ICE_FRICTION, FEET_ON_ICE_FRICTION)
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "beginContact(): Feet-Sand, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, true)

            body.physics.frictionOnSelf.set(FEET_ON_SAND_FRICTION, FEET_ON_SAND_FRICTION)
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            val (listener, _) = contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!

            val body = listener.getBody()
            body.setBodySense(BodySense.IN_WATER, true)

            val entity = listener.getEntity()
            if (entity is Megaman && !entity.body.isSensing(BodySense.FEET_ON_GROUND) && !entity.isBehaviorActive(
                    BehaviorType.WALL_SLIDING
                )
            ) entity.aButtonTask = AButtonTask.SWIM
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

            val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
            VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)

            force.getRunnable()?.invoke()
        }

        // TODO: should this be in the continue contact phase?
        // body, gravity change
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
            val (bodyFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BODY, FixtureType.GRAVITY_CHANGE
            )!!

            val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

            val canChangeGravity = bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true) as Boolean
            if (!canChangeGravity) return

            val body = bodyFixture.getBody()
            val pd = body.getPositionDelta()
            val bodyPointToCheck = if (pd.x > 0f) {
                if (pd.y > 0f) body.getTopRightPoint()
                else if (pd.y < 0f) body.getBottomRightPoint() else body.getCenterRightPoint()
            } else if (pd.x < 0f) {
                if (body.getPositionDelta().y > 0f) body.getTopLeftPoint()
                else if (body.getPositionDelta().y < 0f) body.getBottomLeftPoint()
                else body.getCenterLeftPoint()
            } else {
                if (pd.y > 0f) body.getTopCenterPoint()
                else if (pd.y < 0f) body.getBottomCenterPoint() else body.getCenter()
            }

            if (!gravityChangeFixture.getShape().contains(bodyPointToCheck)) return

            val entity = bodyFixture.getEntity()
            if (entity is IDirectionRotatable && entity.directionRotation != direction) entity.directionRotation =
                direction
        }

        // block, gravity change
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE)) {
            val (blockFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE
            )!!
            val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return
            val canChangeGravity = blockFixture.properties.getOrDefault(
                ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class
            )
            if (!canChangeGravity) return
            if (direction.isHorizontal()) {
                val blockBody = blockFixture.getBody()
                val frictionX = blockBody.physics.frictionToApply.x
                blockBody.physics.frictionToApply.x = blockBody.physics.frictionToApply.y
                blockBody.physics.frictionToApply.y = frictionX
            }
        }

        // laser, block
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BLOCK)) {
            printDebugLog(contact, "continueContact(): Laser-Block, contact = $contact")
            val (laser, block) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK)!!

            val laserEntity = laser.getEntity()
            val blockEntity = block.getEntity()

            if (laserEntity != blockEntity) {
                val blockRectangle = block.getShape() as GameRectangle
                val laserLine = laser.getProperty(ConstKeys.LINE, GameLine::class)!!
                val intersections = laser.properties.get(ConstKeys.COLLECTION) as MutableCollection<Vector2>?
                intersections?.let {
                    val temp = Array<Vector2>()
                    if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, temp)) it.addAll(temp)
                }
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

            if (block.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val body = side.getBody()
            val sideType = side.getProperty(ConstKeys.SIDE)

            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
        }
    }

    override fun endContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // side, block
        if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Side-Block, contact = $contact")
            val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK)!!

            val body = side.getBody()
            val sideType = side.getProperty(ConstKeys.SIDE)

            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "End Contact: Side-Ice, contact = $contact")
            val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

            val body = side.getBody()
            val sideType = side.getProperty(ConstKeys.SIDE)

            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Feet-Block, contact = $contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_GROUND, false)

            val entity = feetFixture.getEntity()
            if (entity is Megaman) entity.aButtonTask = if (entity.body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM
            else AButtonTask.AIR_DASH
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "End Contact: Feet-Ice, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, false)
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "beginContact(): Feet-Sand, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, false)
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Head-Block, contact = $contact")
            val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

            val body = head.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Block-Body, contact = $contact")
            val (_, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY)!!

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, false)
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "End Contact: Feet-Ladder, contact = $contact")
            val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

            val body = feet.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)

            if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "End Contact: Head-Ladder, contact = $contact")
            val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

            val body = head.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)

            if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "End Contact: Body-Force, contact = $contact")
            val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

            val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
            VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)

            force.getRunnable()?.invoke()
        }

        // water-listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            val (listener, water) = contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!
            listener.getBody().setBodySense(BodySense.IN_WATER, false)

            val listenerEntity = listener.getEntity()
            if (listenerEntity is Megaman) listenerEntity.aButtonTask = AButtonTask.AIR_DASH

            game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
            Splash.generate(game, listener.getBody(), water.getBody())

            if (listenerEntity is Megaman) listenerEntity.gravityScalar = 1f
        }

        // player feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "beginContact(): Feet-Cart, contact = $contact")
            val (feet, cart) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART)!!

            val feetEntity = feet.getEntity()
            val cartEntity = cart.getEntity() as IOwnable
            cartEntity.owner = null

            if (feetEntity is Megaman && cartEntity is Cart) {
                feetEntity.body.setBodySense(BodySense.TOUCHING_CART, false)
                feetEntity.body.removeProperty(ConstKeys.CART)
            }
        }

        // teleporter listener, teleporter
        else if (contact.fixturesMatch(FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER)) {
            printDebugLog(contact, "beginContact(): TeleporterListener-Teleporter, contact = $contact")
            val (teleporterListenerFixture, _) = contact.getFixturesInOrder(
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER
            )!!
            teleporterListenerFixture.getBody().setBodySense(BodySense.TELEPORTING, false)
        }
    }
}
