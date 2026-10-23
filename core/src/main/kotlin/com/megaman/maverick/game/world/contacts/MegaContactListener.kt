package com.megaman.maverick.game.world.contacts

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.shapes.ShapeUtils
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.Contact
import com.maverick.game.engine.world.contacts.IContactListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.items.HeartTank
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlterator
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

class MegaContactListener(
    private val game: MegamanMaverickGame,
    private val contactDebugFilter: (Contact) -> Boolean
) : IContactListener {

    companion object {
        const val TAG = "MegaContactListener"
        const val ICE_FRICTION = 0.025f
        const val SAND_FRICTION = 1.15f
        const val SNOW_FRICTION_X = 0.95f
        const val SNOW_FRICTION_Y = 0.5f
    }

    private val out = GamePair<IFixture, IFixture>(DummyFixture(), DummyFixture())
    private val set1 = ObjectSet<Any>()
    private val set2 = ObjectSet<Any>()
    private val tempVec2Set = ObjectSet<Vector2>()

    override fun beginContact(contact: Contact, delta: Float) {
        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            printDebugLog(contact, "beginContact(): Consumer, contact=$contact")
            val (consumerFixture, consumableFixture) =
                contact.getFixturesIfOneMatches(FixtureType.CONSUMER, out)!!
            consumerFixture.receiver(Receivers.CONSUMER)?.invoke(ProcessState.BEGIN, consumableFixture)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            printDebugLog(contact, "beginContact(): Damager-Damageable, contact=$contact")

            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE, out
            )!!

            val damager = damagerFixture.getEntity() as IDamager
            val damageable = damageableFixture.getEntity() as IDamageable

            damagerFixture.receiver(Receivers.HIT_BY_DAMAGEABLE)?.invoke(damageable, ProcessState.BEGIN)

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
                typesSet1(FixtureType.DEATH),
                typesSet2(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )
        ) {
            printDebugLog(contact, "beginContact(): Death-Feet/Side/Head/Block, contact=$contact")

            val (deathFixture, otherFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.DEATH),
                typesSet2(FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY),
                out
            )!!

            val entity = otherFixture.getEntity()
            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.isProperty(ConstKeys.INSTANT, true)
            when {
                entity is IDamageable && (instant || !entity.invincible) -> otherFixture.depleteHealth()
                entity is IProjectileEntity -> entity.explodeAndDie()
                deathShouldDestroy(entity) -> entity.destroy()
            }
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Block-Body, contact=$contact")

            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val block = blockFixture.getEntity() as Block
            block.hitByBody(bodyFixture)

            bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // body, body
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Body-Body, contact=$contact")
            val (bodyFixture1, bodyFixture2) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.BODY, out)!!

            bodyFixture1.receiver(Receivers.HIT_BY_BODY)?.invoke(
                bodyFixture2.getEntity() as IBodyEntity,
                ProcessState.BEGIN
            )

            bodyFixture2.receiver(Receivers.HIT_BY_BODY)?.invoke(
                bodyFixture1.getEntity() as IBodyEntity,
                ProcessState.BEGIN
            )
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "beginContact(): Body-Feet, contact=$contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            bodyFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.BEGIN)?.invoke(
                feetFixture,
                delta
            )
        }

        // body, explosion
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.EXPLOSION)) {
            printDebugLog(contact, "beginContact(): Body-Explosion, contact=$contact")
            val (bodyFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BODY,
                FixtureType.EXPLOSION,
                out
            )!!
            bodyFixture.receiver(Receivers.HIT_BY_EXPLOSION)?.invoke(explosionFixture.getEntity() as IBodyEntity)
        }

        // block, explosion
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.EXPLOSION)) {
            printDebugLog(contact, "beginContact(): Block-Explosion, contact=$contact")
            val (blockFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BLOCK,
                FixtureType.EXPLOSION,
                out
            )!!
            blockFixture.receiver(Receivers.HIT_BY_EXPLOSION)?.invoke(explosionFixture.getEntity() as IBodyEntity)
        }

        // body, side
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Body-Side, contact=$contact")

            val (bodyFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.SIDE, out)!!
            if (bodyFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val bodyEntity = bodyFixture.getEntity() as IBodyEntity
            sideFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, ProcessState.BEGIN)
        }

        // side, side
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Side-Side, contact=$contact")

            val (side1Fixture, side2Fixture) = contact

            side1Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side2Fixture, ProcessState.BEGIN)
            side2Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side1Fixture, ProcessState.BEGIN)
        }

        // shield, side
        else if (contact.fixturesMatch(FixtureType.SHIELD, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Shield-Side, contact=$contact")

            val (shieldFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.SHIELD, FixtureType.SIDE, out)!!
            sideFixture.receiver(Receivers.HIT_BY_SHIELD)?.invoke(ProcessState.BEGIN, shieldFixture)
        }

        // block, side
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Block-Side, contact=$contact")

            val (blockFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.SIDE, out)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)

            val block = blockFixture.getEntity() as Block
            block.hitBySide(sideFixture)

            sideFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
        }

        // side / feet / head, gate
        else if (contact.fixtureSetsMatch(
                typesSet1(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD),
                typesSet2(FixtureType.GATE)
            )
        ) {
            printDebugLog(contact, "beginContact(): Side/Feet/Head-Gate")
            val (otherFixture, gateFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD),
                typesSet2(FixtureType.GATE),
                out
            )!!

            val entity = otherFixture.getEntity()
            if (entity is Megaman) {
                val gate = gateFixture.getEntity() as Gate
                if (gate.isTriggerable()) gate.trigger()
            }
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Side-Ice, contact=$contact")
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE, out)!!
            val body = sideFixture.getBody()

            val applyIceFrictionY =
                body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", true, Boolean::class)
            val entity = body.getEntity()
            if (applyIceFrictionY || (entity is Megaman && entity.isBehaviorActive(BehaviorType.WALL_SLIDING)))
                body.physics.frictionOnSelf.y = ICE_FRICTION * ConstVals.PPM

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Feet-Block, contact=$contact")
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
            if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

            val block = blockFixture.getEntity() as Block
            val body = feetFixture.getBody()

            if (!blockFixture.getBody().physics.collisionOn) {
                body.removeFeetBlock(block)
                if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            body.addFeetBlock(block)

            val stickToBlock =
                feetFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.BEGIN, blockFixture) ?: true
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.translate(posDelta)
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                entity.aButtonTask = AButtonTask.JUMP

                val blockMakesSound = block.getOrDefaultProperty(
                    "${ConstKeys.FEET}_${ConstKeys.SOUND}",
                    !block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) || when (entity.direction) {
                        Direction.UP -> entity.body.physics.velocity.y <= 0f
                        Direction.DOWN -> entity.body.physics.velocity.y >= 0f
                        Direction.LEFT -> entity.body.physics.velocity.x >= 0f
                        Direction.RIGHT -> entity.body.physics.velocity.x <= 0f
                    },
                    Boolean::class
                )
                if (blockMakesSound && entity.canMakeLandSound) {
                    entity.requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                    entity.canMakeLandSound = false
                }
            }

            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            block.hitByFeet(ProcessState.BEGIN, feetFixture)
            feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Feet-Ice, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            val applyIceFrictionX =
                body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true, Boolean::class)
            if (applyIceFrictionX) body.physics.frictionOnSelf.x = ICE_FRICTION * ConstVals.PPM
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "beginContact(): Feet-Sand, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SAND_FRICTION, SAND_FRICTION).scl(ConstVals.PPM.toFloat())

            if (game.megaman.ready) {
                val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
                val position = feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
                splash.spawn(
                    props(
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.TYPE pairTo SplashType.SAND,
                        ConstKeys.SOUND pairTo true
                    )
                )
            }
        }

        // feet, snow
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SNOW)) {
            printDebugLog(contact, "beginContact(): Feet-Snow, contact=$contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SNOW}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SNOW_FRICTION_X, SNOW_FRICTION_Y).scl(ConstVals.PPM.toFloat())

            snowFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.BEGIN)?.invoke(feetFixture, delta)
        }

        // bouncer, body, feet or head or side
        else if (contact.fixtureSetsMatch(
                typesSet1(FixtureType.BOUNCER),
                typesSet2(FixtureType.BODY, FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE)
            )
        ) {
            printDebugLog(contact, "beginContact(): Bouncer-Body/Feet/Head/Side, contact=$contact")

            val (bouncerFixture, bounceableFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.BOUNCER),
                typesSet2(FixtureType.BODY, FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE),
                out
            )!!

            val bounce =
                bouncerFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bounceableFixture, delta, ProcessState.BEGIN)
            VelocityAlterator.alterate(bounceableFixture.getBody(), bounce)

            bouncerFixture.receiver(Receivers.RUNNABLE)?.invoke()
            bounceableFixture.receiver(Receivers.RUNNABLE)?.invoke()
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Head-Block, contact=$contact")
            val (headFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK, out)!!

            if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            body.physics.velocity.y = 0f

            val block = blockFixture.getEntity() as Block

            block.hitByHead(ProcessState.BEGIN, headFixture)
            headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
        }

        // head, feet
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.FEET)) {
            printDebugLog(contact, "beginContact(): Head-Feet, contact=$contact")
            val (headFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.FEET, out)!!

            headFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.BEGIN)?.invoke(feetFixture, delta)
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            val (listenerFixture, waterFixture) = contact.getFixturesInOrder(
                FixtureType.WATER_LISTENER, FixtureType.WATER, out
            )!!

            printDebugLog(
                contact,
                "beginContact(): WaterListener-Water. Contact = $contact. Water shape = ${waterFixture.getShape()}"
            )

            val entity = listenerFixture.getEntity()

            val body = listenerFixture.getBody()
            val wasInWater = body.hasAnyContactWater()

            val water = waterFixture.getEntity() as IWater
            body.setBodySense(BodySense.IN_WATER, true)
            body.addContactWater(water)

            listenerFixture.receiver(Receivers.HIT_BY_WATER)?.invoke(water)

            val shouldSplash = water.shouldSplash(listenerFixture) &&
                listenerFixture.getOrDefaultProperty(ConstKeys.SPLASH, true, Boolean::class)

            if (!wasInWater && shouldSplash) Splash.splashOnWaterSurface(
                listenerFixture.getBody().getBounds(),
                waterFixture.getBody().getBounds(),
                water.getSplashType(listenerFixture),
                water.doMakeSplashSound(listenerFixture)
            )

            if (entity is Megaman) {
                if (!entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                    !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
                ) entity.aButtonTask = AButtonTask.SWIM

                entity.gravityScalar = MegamanValues.WATER_GRAVITY_SCALAR

                if (!wasInWater) {
                    if (entity.direction.isVertical()) body.physics.velocity.y = 0f
                    else body.physics.velocity.x = 0f
                }
            }
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Head-Ladder, contact=$contact")
            val (headFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER, out)!!

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
            body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Feet-Ladder, contact=$contact")
            val (feetFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
            body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "beginContact(): Body-Force, contact=$contact")
            val (bodyFixture, forceFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE, out)!!

            val receiveForce =
                bodyFixture.getOrDefaultProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", true, Boolean::class)
            if (!receiveForce) return

            val forceAlteration =
                forceFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bodyFixture, delta, ProcessState.BEGIN)
            val forceListener = bodyFixture.receiver(Receivers.FORCE_ALTERATION, ProcessState.BEGIN)
            if (forceListener != null) forceListener.invoke(forceAlteration)
            else VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration)

            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, true)
            forceFixture.receiver(Receivers.RUNNABLE)?.invoke()
        }

        // body, gravity change
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
            val (bodyFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BODY, FixtureType.GRAVITY_CHANGE, out
            )!!

            val body = bodyFixture.getBody()
            val pd = body.getPositionDelta()

            val bodyPointToCheck = when {
                pd.x > 0f -> {
                    if (pd.y > 0f) body.getPositionPoint(Position.TOP_RIGHT)
                    else if (pd.y < 0f) body.getPositionPoint(Position.BOTTOM_RIGHT) else body.getPositionPoint(Position.CENTER_RIGHT)
                }

                pd.x < 0f -> {
                    if (body.getPositionDelta().y > 0f) body.getPositionPoint(Position.TOP_LEFT)
                    else if (body.getPositionDelta().y < 0f) body.getPositionPoint(Position.BOTTOM_LEFT)
                    else body.getPositionPoint(Position.CENTER_LEFT)
                }

                else -> {
                    if (pd.y > 0f) body.getPositionPoint(Position.TOP_CENTER)
                    else if (pd.y < 0f) body.getPositionPoint(Position.BOTTOM_CENTER) else body.getCenter()
                }
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
                    bodyFixture.getOrDefaultProperty(ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class)
                if (canChangeGravityRotation) {
                    val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return
                    if (entity is IDirectional && entity.direction != direction)
                        entity.direction = direction
                }
            }
        }

        // block, gravity change
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE)) {
            val (blockFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE, out
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
            printDebugLog(contact, "beginContact(): Body-Player, contact=$contact")
            val (bodyFixture, playerFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.PLAYER, out)!!
            bodyFixture.receiver(Receivers.HIT_BY_PLAYER)?.invoke(playerFixture.getEntity() as Megaman)
        }

        // projectile, block or body or shield or water or projectile
        else if (contact.fixtureSetsMatch(
                typesSet1(FixtureType.PROJECTILE),
                typesSet2(
                    FixtureType.BLOCK,
                    FixtureType.BODY,
                    FixtureType.SHIELD,
                    FixtureType.WATER,
                    FixtureType.SAND,
                    FixtureType.PROJECTILE,
                    FixtureType.EXPLOSION
                ),
            )
        ) {
            printDebugLog(
                contact, "beginContact(): Projectile-Block/Body/Shield/Water, contact=$contact"
            )
            val (projectileFixture, otherFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.PROJECTILE),
                typesSet2(
                    FixtureType.BLOCK,
                    FixtureType.BODY,
                    FixtureType.SHIELD,
                    FixtureType.WATER,
                    FixtureType.SAND,
                    FixtureType.PROJECTILE,
                    FixtureType.EXPLOSION
                ),
                out
            )!!

            val projectile1 = projectileFixture.getEntity() as IProjectileEntity

            if (otherFixture.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION) &&
                otherFixture.receiver(Receivers.NO_PROJECTILE_COLLISION_EXCEPTION)
                    ?.invoke(projectile1, projectileFixture) != true
            ) return

            otherFixture.receiver(Receivers.HIT_BY_PROJECTILE)?.invoke(projectile1)

            val thisShape = projectileFixture.getShape().copy()
            val otherShape = otherFixture.getShape().copy()
            when (otherFixture.getType()) {
                FixtureType.BLOCK -> {
                    printDebugLog(contact, "beginContact(): Projectile-Block, contact=$contact")

                    // If the body has a "shield" property set to true, then the projectile should ignore
                    // the block fixture and instead make contact only with the other body's shield fixture.
                    // IMPORTANT: If the body does NOT have a shield fixture, then nothing will trigger.
                    val shielded = otherFixture.getBody().isProperty(ConstKeys.SHIELD, true)
                    if (shielded) return

                    projectile1.hitBlock(otherFixture, thisShape, otherShape)
                    (otherFixture.getEntity() as Block).hitByProjectile(projectileFixture)
                }

                FixtureType.BODY -> {
                    printDebugLog(contact, "beginContact(): Projectile-Body, contact=$contact")
                    projectile1.hitBody(otherFixture, thisShape, otherShape)
                }

                FixtureType.SHIELD -> {
                    printDebugLog(contact, "beginContact(): Projectile-Shield, contact=$contact")
                    projectile1.hitShield(otherFixture, thisShape, otherShape)
                }

                FixtureType.WATER -> {
                    printDebugLog(contact, "beginContact(): Projectile-Water, contact=$contact")
                    projectile1.hitWater(otherFixture, thisShape, otherShape)
                }

                FixtureType.SAND -> {
                    printDebugLog(contact, "beginContact(): Projectile-Sand, contact=$contact")
                    projectile1.hitSand(otherFixture, thisShape, otherShape)
                    val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!

                    val overlap = GameObjectPools.fetch(Rectangle::class)
                    Intersector.intersectRectangles(
                        projectileFixture.getShape().toGdxRectangle(),
                        otherFixture.getShape().toGdxRectangle(),
                        overlap
                    )

                    val position = overlap.getCenter()
                    splash.spawn(
                        props(
                            ConstKeys.TYPE pairTo SplashType.SAND,
                            ConstKeys.POSITION pairTo position,
                            ConstKeys.SOUND pairTo true
                        )
                    )
                }

                FixtureType.PROJECTILE -> {
                    printDebugLog(contact, "beginContact(): Projectile-Projectile, contact=$contact")

                    projectile1.hitProjectile(otherFixture, thisShape, otherShape)

                    val projectile2 = otherFixture.getEntity() as IProjectileEntity
                    projectile2.hitProjectile(projectileFixture, otherShape, thisShape)
                }

                FixtureType.EXPLOSION -> {
                    printDebugLog(contact, "beginContact(): Projectile-Explosion, contact=$contact")
                    projectile1.hitExplosion(otherFixture, otherShape, thisShape)
                }
            }
        }

        // player, item
        else if (contact.fixturesMatch(FixtureType.PLAYER, FixtureType.ITEM)) {
            printDebugLog(contact, "beginContact(): Player-Item, contact=$contact")

            val (playerFixture, itemFixture) = contact.getFixturesInOrder(FixtureType.PLAYER, FixtureType.ITEM, out)!!

            val playerEntity = playerFixture.getEntity()
            val itemEntity = itemFixture.getEntity()

            if (playerEntity is Megaman && itemEntity is ItemEntity) itemEntity.contactWithPlayer(playerEntity)
        }

        // feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "beginContact(): Feet-Cart, contact=$contact")
            val (feetFixture, cartFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART, out)!!

            val feetEntity = feetFixture.getEntity()
            val cartEntity = cartFixture.getEntity() as IOwnable<*>

            if (feetEntity is Megaman && cartEntity is Cart) {
                cartEntity.owner = feetEntity
                feetEntity.body.setBodySense(BodySense.TOUCHING_CART, true)
                feetEntity.body.putProperty(ConstKeys.CART, cartEntity)
            }
        }

        // teleporter listener, teleporter
        else if (contact.fixturesMatch(FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER)) {
            printDebugLog(contact, "beginContact(): TeleporterListener-Teleporter, contact=$contact")

            val (teleporterListenerFixture, teleporterFixture) = contact.getFixturesInOrder(
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER, out
            )!!

            val teleporterListener = teleporterListenerFixture.getEntity() as IBodyEntity

            val teleporterEntity = teleporterFixture.getEntity() as ITeleporterEntity

            if (teleporterEntity.shouldTeleport(teleporterListener)) teleporterEntity.teleport(teleporterListener)
        }

        // laser, block
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Laser-Block, contact=$contact")

            val (laserFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK, out)!!

            val blockBody = blockFixture.getBody() as Body
            // If the block's body also has a shield fixture, then ignore the block. This assumes that the
            // block's shield fixture and block fixture are the same size and there is only one shield fixture.
            if (blockBody.fixtures.containsKey(FixtureType.SHIELD)) return

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val blockEntity = blockFixture.getEntity() as Block

            if (laserEntity != blockEntity && !laserEntity.isLaserIgnoring(blockEntity)) {
                val blockRectangle = blockFixture.getShape() as GameRectangle
                val laserLine = laserFixture.getShape() as GameLine

                val intersections =
                    laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

                if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Set))
                    tempVec2Set.forEach { intersections.add(it pairTo blockFixture) }

                tempVec2Set.clear()
            }
        }

        // laser, body
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Laser-Body, contact=$contact")

            val (laserFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BODY, out)!!

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val bodyEntity = bodyFixture.getEntity() as IBodyEntity

            if (laserEntity != bodyEntity && !laserEntity.isLaserIgnoring(bodyEntity)) {
                laserFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, ProcessState.BEGIN)
            }
        }

        // laser, shield
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.SHIELD)) {
            printDebugLog(contact, "beginContact(): Laser-Shield, contact=$contact")

            val (laserFixture, shieldFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.SHIELD, out)!!

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val shieldEntity = shieldFixture.getEntity()

            if (laserEntity != shieldEntity && !laserEntity.isLaserIgnoring(shieldEntity)) {
                val shieldRectangle = shieldFixture.getShape().getBoundingRectangle()
                val laserLine = laserFixture.getShape() as GameLine

                val intersections =
                    laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

                if (ShapeUtils.intersectRectangleAndLine(shieldRectangle, laserLine, tempVec2Set))
                    tempVec2Set.forEach { intersections.add(it pairTo shieldFixture) }

                tempVec2Set.clear()
            }
        }
    }

    override fun continueContact(contact: Contact, delta: Float) {
        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            val (consumerFixture, consumableFixture) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER, out)!!
            consumerFixture.receiver(Receivers.CONSUMER)?.invoke(ProcessState.CONTINUE, consumableFixture)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE, out
            )!!

            val damager = damagerFixture.getEntity() as IDamager
            val damageable = damageableFixture.getEntity() as IDamageable

            damagerFixture.receiver(Receivers.HIT_BY_DAMAGEABLE)?.invoke(damageable, ProcessState.CONTINUE)

            if (damageable.canBeDamagedBy(damager) && damager.canDamage(damageable)) {
                damageable.takeDamageFrom(damager)
                damager.onDamageInflictedTo(damageable)
            }
        }

        // death, feet / side / head / body
        else if (contact.fixtureSetsMatch(
                typesSet1(FixtureType.DEATH),
                typesSet2(FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY)
            )
        ) {
            val (deathFixture, otherFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.DEATH),
                typesSet2(FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY),
                out
            )!!

            val entity = otherFixture.getEntity()

            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.isProperty(ConstKeys.INSTANT, true)
            when {
                entity is IDamageable && (instant || !entity.invincible) -> otherFixture.depleteHealth()
                entity is IProjectileEntity -> entity.explodeAndDie()
                deathShouldDestroy(entity) -> entity.destroy()
            }
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "endContact(): Block-Body, contact=$contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val block = blockFixture.getEntity() as Block
            bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
            if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

            val block = blockFixture.getEntity() as Block

            val body = feetFixture.getBody()
            if (!blockFixture.getBody().physics.collisionOn) {
                body.removeFeetBlock(block)
                if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            body.addFeetBlock(block)

            val stickToBlock =
                feetFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.CONTINUE, blockFixture) ?: true
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.translate(posDelta)
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            block.hitByFeet(ProcessState.CONTINUE, feetFixture)
            feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            val (feetFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER, out)!!
            val feetEntity = feetFixture.getEntity()

            val feetDirection = if (feetEntity is IDirectional) feetEntity.direction else Direction.UP
            val feetPoint = when (feetDirection) {
                Direction.UP -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
                Direction.DOWN -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.TOP_CENTER)
                Direction.LEFT -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_RIGHT)
                Direction.RIGHT -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_LEFT)
            }

            if (ladderFixture.getShape().contains(feetPoint)) {
                val body = feetFixture.getBody()
                body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
                body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
            }
        }

        // body, side
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.SIDE)) {
            printDebugLog(contact, "continueContact(): Body-Side, contact=$contact")

            val (bodyFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.SIDE, out)!!
            if (bodyFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val bodyEntity = bodyFixture.getEntity() as IBodyEntity
            sideFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, ProcessState.CONTINUE)
        }

        // side, side
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.SIDE)) {
            printDebugLog(contact, "continueContact(): Side-Side, contact=$contact")

            val (side1Fixture, side2Fixture) = contact

            side1Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side2Fixture, ProcessState.CONTINUE)
            side2Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side1Fixture, ProcessState.CONTINUE)
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            val (headFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER, out)!!

            val headEntity = headFixture.getEntity()
            val headDirection = if (headEntity is IDirectional) headEntity.direction else Direction.UP

            val headPoint = when (headDirection) {
                Direction.UP -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.TOP_CENTER)
                Direction.DOWN -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
                Direction.LEFT -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_LEFT)
                Direction.RIGHT -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_RIGHT)
            }

            if (ladderFixture.getShape().contains(headPoint)) {
                val body = headFixture.getBody()
                body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
                body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
            }
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            val (headFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK, out)!!

            if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)

            val block = blockFixture.getEntity() as Block

            block.hitByHead(ProcessState.CONTINUE, headFixture)
            headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)
        }

        // head, feet
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.FEET)) {
            printDebugLog(contact, "continueContact(): Head-Feet, contact=$contact")
            val (headFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.FEET, out)!!

            headFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.CONTINUE)?.invoke(feetFixture, delta)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE, out)!!
            val body = feetFixture.getBody()

            body.setBodySense(BodySense.FEET_ON_ICE, true)

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                if (entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                    (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && entity.isFacing(Facing.LEFT) ||
                        (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && entity.isFacing(Facing.RIGHT)))
                ) return
            }

            val applyIceFrictionX =
                body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true, Boolean::class)
            if (applyIceFrictionX) body.physics.frictionOnSelf.x = ICE_FRICTION * ConstVals.PPM
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "continueContact(): Feet-Sand, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SAND}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SAND_FRICTION, SAND_FRICTION).scl(ConstVals.PPM.toFloat())
        }

        // feet, snow
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SNOW)) {
            printDebugLog(contact, "continueContact(): Feet-Snow, contact=$contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SNOW}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SNOW_FRICTION_X, SNOW_FRICTION_Y).scl(ConstVals.PPM.toFloat())


            snowFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.CONTINUE)?.invoke(feetFixture, delta)
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "continueContact(): Body-Feet, contact=$contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            bodyFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.CONTINUE)?.invoke(
                feetFixture,
                delta
            )
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            val (listenerFixture, waterFixture) = contact.getFixturesInOrder(
                FixtureType.WATER_LISTENER,
                FixtureType.WATER,
                out
            )!!

            val water = waterFixture.getEntity() as IWater

            val body = listenerFixture.getBody()
            body.setBodySense(BodySense.IN_WATER, true)
            body.addContactWater(water)

            val entity = listenerFixture.getEntity()
            if (entity is Megaman && !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
            ) entity.aButtonTask = AButtonTask.SWIM
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            val (bodyFixture, forceFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE, out)!!

            val receiveForce =
                bodyFixture.getOrDefaultProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", true, Boolean::class)
            if (!receiveForce) return

            val forceAlteration =
                forceFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bodyFixture, delta, ProcessState.CONTINUE)
            val forceListener = bodyFixture.receiver(Receivers.FORCE_ALTERATION, ProcessState.CONTINUE)
            if (forceListener != null) forceListener.invoke(forceAlteration)
            else VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration)

            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, true)
            forceFixture.receiver(Receivers.RUNNABLE)?.invoke()
        }

        // body, explosion
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.EXPLOSION)) {
            val (bodyFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BODY,
                FixtureType.EXPLOSION,
                out
            )!!
            bodyFixture.receiver(Receivers.HIT_BY_EXPLOSION)?.invoke(explosionFixture.getEntity() as IBodyEntity)
        }

        // block, explosion
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.EXPLOSION)) {
            val (blockFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BLOCK,
                FixtureType.EXPLOSION,
                out
            )!!
            blockFixture.receiver(Receivers.HIT_BY_EXPLOSION)?.invoke(explosionFixture.getEntity() as IBodyEntity)
        }

        // body, gravity change
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
            val (bodyFixture, gravityChangeFixture) = contact.getFixturesInOrder(
                FixtureType.BODY, FixtureType.GRAVITY_CHANGE, out
            )!!

            val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

            val canChangeGravity = bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true) as Boolean
            if (!canChangeGravity) return

            val body = bodyFixture.getBody()
            val pd = body.getPositionDelta()

            val bodyPointToCheck = when {
                pd.x > 0f -> when {
                    pd.y > 0f -> body.getPositionPoint(Position.TOP_RIGHT)
                    pd.y < 0f -> body.getPositionPoint(Position.BOTTOM_RIGHT)
                    else -> body.getPositionPoint(Position.CENTER_RIGHT)
                }

                pd.x < 0f -> when {
                    body.getPositionDelta().y > 0f -> body.getPositionPoint(Position.TOP_LEFT)
                    body.getPositionDelta().y < 0f -> body.getPositionPoint(Position.BOTTOM_LEFT)
                    else -> body.getPositionPoint(Position.CENTER_LEFT)
                }

                else -> when {
                    pd.y > 0f -> body.getPositionPoint(Position.TOP_CENTER)
                    pd.y < 0f -> body.getPositionPoint(Position.BOTTOM_CENTER)
                    else -> body.getCenter()
                }
            }

            if (!gravityChangeFixture.getShape().contains(bodyPointToCheck)) return

            val entity = bodyFixture.getEntity()
            if (entity is IDirectional && entity.direction != direction) entity.direction = direction
        }

        // block, gravity change
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE)) {
            val (blockFixture, gravityChangeFixture) =
                contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.GRAVITY_CHANGE, out)!!

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
            printDebugLog(contact, "beginContact(): Laser-Block, contact=$contact")

            val (laserFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK, out)!!

            val blockBody = blockFixture.getBody() as Body
            // If the block's body also has a shield fixture, then ignore the block. This assumes that the
            // block's shield fixture and block fixture are the same size and there is only one shield fixture.
            if (blockBody.fixtures.containsKey(FixtureType.SHIELD)) return

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val blockEntity = blockFixture.getEntity() as Block

            if (laserEntity != blockEntity && !laserEntity.isLaserIgnoring(blockEntity)) {
                val blockRectangle = blockFixture.getShape().getBoundingRectangle()
                val laserLine = laserFixture.getShape() as GameLine

                val intersections =
                    laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>

                if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Set))
                    tempVec2Set.forEach { intersections.add(it pairTo blockFixture) }

                tempVec2Set.clear()
            }
        }

        // laser, body
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Laser-Body, contact=$contact")

            val (laserFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BODY, out)!!

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val bodyEntity = bodyFixture.getEntity() as IBodyEntity

            if (laserEntity != bodyEntity && !laserEntity.isLaserIgnoring(bodyEntity)) {
                laserFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, ProcessState.CONTINUE)
            }
        }

        // laser, shield
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.SHIELD)) {
            printDebugLog(contact, "beginContact(): Laser-Shield, contact=$contact")

            val (laserFixture, shieldFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.SHIELD, out)!!

            val laserEntity = laserFixture.getEntity() as ILaserEntity
            val shieldEntity = shieldFixture.getEntity()

            if (laserEntity != shieldEntity && !laserEntity.isLaserIgnoring(shieldEntity)) {
                val shieldRectangle = shieldFixture.getShape().getBoundingRectangle()
                val laserLine = laserFixture.getShape() as GameLine

                val intersections =
                    laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<GamePair<Vector2, IFixture>>


                if (ShapeUtils.intersectRectangleAndLine(shieldRectangle, laserLine, tempVec2Set))
                    tempVec2Set.forEach { intersections.add(it pairTo shieldFixture) }

                tempVec2Set.clear()
            }
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE, out)!!
            val body = sideFixture.getBody()

            val applyIceFrictionY =
                body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", true, Boolean::class)
            val entity = body.getEntity()
            if (applyIceFrictionY || (entity is Megaman && entity.isBehaviorActive(BehaviorType.WALL_SLIDING)))
                body.physics.frictionOnSelf.y = ICE_FRICTION * ConstVals.PPM

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
        }

        // side, block
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
            val (sideFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK, out)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val body = sideFixture.getBody()

            val stickToBlock =
                sideFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.CONTINUE, blockFixture) ?: true
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.translate(posDelta)
            }

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            when (sideType) {
                ConstKeys.LEFT -> body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
                else -> body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            }
        }


        // teleporter listener, teleporter
        else if (contact.fixturesMatch(FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER)) {
            printDebugLog(contact, "continueContact(): TeleporterListener-Teleporter, contact=$contact")

            val (teleporterListenerFixture, teleporterFixture) = contact.getFixturesInOrder(
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER, out
            )!!

            val teleporterListener = teleporterListenerFixture.getEntity() as IBodyEntity

            val teleporterEntity = teleporterFixture.getEntity() as ITeleporterEntity

            if (teleporterEntity.shouldTeleport(teleporterListener)) teleporterEntity.teleport(teleporterListener)
        }
    }

    override fun endContact(contact: Contact, delta: Float) {
        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            val (consumerFixture, consumableFixture) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER, out)!!
            consumerFixture.receiver(Receivers.CONSUMER)?.invoke(ProcessState.END, consumableFixture)
        }

        // side, block
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
            printDebugLog(contact, "endContact(): Side-Block, contact=$contact")
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK, out)!!

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
        }

        // side, side
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.SIDE)) {
            printDebugLog(contact, "endContact(): Side-Side, contact=$contact")

            val (side1Fixture, side2Fixture) = contact

            side1Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side2Fixture, ProcessState.END)
            side2Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side1Fixture, ProcessState.END)
        }

        // body, side
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.SIDE)) {
            printDebugLog(contact, "endContact(): Body-Side, contact=$contact")

            val (bodyFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.SIDE, out)!!
            if (bodyFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val bodyEntity = bodyFixture.getEntity() as IBodyEntity
            sideFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, ProcessState.END)
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "endContact(): Side-Ice, contact=$contact")
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE, out)!!

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            printDebugLog(contact, "endContact(): Feet-Block, contact=$contact")
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
            if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

            val block = blockFixture.getEntity() as Block
            val body = feetFixture.getBody()

            body.removeFeetBlock(block)
            if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                if (!entity.bodyOverGround) {
                    entity.aButtonTask = when {
                        entity.body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                        else -> AButtonTask.AIR_DASH
                    }
                }
                if (!body.hasAnyFeetBlock()) entity.canMakeLandSound = true
            }

            block.hitByFeet(ProcessState.END, feetFixture)
            feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "endContact(): Feet-Ice, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, false)
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "endContact(): Feet-Sand, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, false)
        }

        // feet, snow
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SNOW)) {
            printDebugLog(contact, "endContact(): Feet-Snow, contact=$contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, false)

            snowFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.END)?.invoke(feetFixture, delta)
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "endContact(): Head-Block, contact=$contact")
            val (headFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK, out)!!

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)

            val block = blockFixture.getEntity() as Block

            block.hitByHead(ProcessState.END, headFixture)
            headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)
        }

        // head, feet
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.FEET)) {
            printDebugLog(contact, "endContact(): Head-Feet, contact=$contact")
            val (headFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.FEET, out)!!

            headFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.END)?.invoke(feetFixture, delta)
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "endContact(): Block-Body, contact=$contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!

            val block = blockFixture.getEntity() as Block
            bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, false)
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "endContact(): Body-Feet, contact=$contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            bodyFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.END)?.invoke(
                feetFixture,
                delta
            )
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "endContact(): Feet-Ladder, contact=$contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)
            if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "endContact(): Head-Ladder, contact=$contact")
            val (headFixture, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER, out)!!
            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)
            if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "endContact(): Body-Force, contact=$contact")
            val (bodyFixture, forceFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE, out)!!

            val receiveForce =
                bodyFixture.getOrDefaultProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", true, Boolean::class)
            if (!receiveForce) return

            val forceAlteration =
                forceFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bodyFixture, delta, ProcessState.END)
            val forceListener = bodyFixture.receiver(Receivers.FORCE_ALTERATION, ProcessState.END)
            if (forceListener != null) forceListener.invoke(forceAlteration)
            else VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration)

            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, false)
            forceFixture.receiver(Receivers.RUNNABLE)?.invoke()
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            printDebugLog(contact, "endContact(): Water-Listener, Water. Contact = $contact")
            val (listenerFixture, waterFixture) = contact.getFixturesInOrder(
                FixtureType.WATER_LISTENER, FixtureType.WATER, out
            )!!

            val water = waterFixture.getEntity() as IWater

            val body = listenerFixture.getBody()
            body.removeContactWater(water)
            if (!body.hasAnyContactWater()) body.setBodySense(BodySense.IN_WATER, false)

            if (!body.isSensing(BodySense.IN_WATER)) {
                val shouldSplash = water.shouldSplash(listenerFixture) &&
                    listenerFixture.getOrDefaultProperty(ConstKeys.SPLASH, true, Boolean::class)

                if (shouldSplash) Splash.splashOnWaterSurface(
                    listenerFixture.getBody().getBounds(),
                    waterFixture.getBody().getBounds(),
                    water.getSplashType(listenerFixture),
                    water.doMakeSplashSound(listenerFixture)
                )

                val listener = listenerFixture.getEntity()
                if (listener is Megaman) {
                    listener.aButtonTask = AButtonTask.AIR_DASH
                    listener.gravityScalar = 1f
                }
            }
        }

        // player feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "endContact(): Feet-Cart, contact=$contact")
            val (feetFixture, cartFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART, out)!!

            val feetEntity = feetFixture.getEntity()
            val cartEntity = cartFixture.getEntity() as IOwnable<*>
            cartEntity.owner = null

            if (feetEntity is Megaman && cartEntity is Cart) {
                feetEntity.body.setBodySense(BodySense.TOUCHING_CART, false)
                feetEntity.body.removeProperty(ConstKeys.CART)
            }
        }
    }

    private fun printDebugLog(contact: Contact, log: String) {
        if (contactDebugFilter.invoke(contact)) GameLogger.debug(TAG, log)
    }

    private fun deathShouldDestroy(entity: IGameEntity) = entity is HeartTank

    private fun typesSet1(vararg fixtureTypes: FixtureType): ObjectSet<Any> {
        set1.clear()
        fixtureTypes.forEach { set1.add(it) }
        return set1
    }

    private fun typesSet2(vararg fixtureTypes: FixtureType): ObjectSet<Any> {
        set2.clear()
        fixtureTypes.forEach { set2.add(it) }
        return set2
    }

    internal class DummyFixture : IFixture {
        override fun getShape() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override fun setShape(shape: IGameShape2D) =
            throw IllegalStateException("The `setShape` method should never be called on a DummyFixture instance")

        override fun setActive(active: Boolean) =
            throw IllegalStateException("The `setShape` method should never be called on a DummyFixture instance")

        override fun isActive() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override fun getType() =
            throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")

        override val properties: Properties
            get() = throw IllegalStateException("The `getType` method should never be called on a DummyFixture instance")
    }
}
