package com.megaman.maverick.game.world.contacts

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.shapes.ShapeUtils
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.IFixture
import com.mega.game.engine.world.contacts.Contact
import com.mega.game.engine.world.contacts.IContactListener
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
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.entities.sensors.Gate.GateState
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlterator
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

class MegaContactListener(
    private val game: MegamanMaverickGame, private val contactDebugFilter: (Contact) -> Boolean
) : IContactListener {

    companion object {
        const val TAG = "MegaContactListener"
        const val ICE_FRICTION = 0.035f
        const val SAND_FRICTION = 1.15f
        const val SNOW_FRICTION_X = 0.95f
        const val SNOW_FRICTION_Y = 0.5f
    }

    private val out = GamePair<IFixture, IFixture>(DummyFixture(), DummyFixture())
    private val set1 = ObjectSet<Any>()
    private val set2 = ObjectSet<Any>()
    private val tempVec2Arr = Array<Vector2>()

    override fun beginContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            printDebugLog(contact, "beginContact(): Consumer, contact = $contact")
            val (consumerFixture, consumableFixture) =
                contact.getFixturesIfOneMatches(FixtureType.CONSUMER, out)!!
            consumerFixture.getConsumer()?.invoke(ProcessState.BEGIN, consumableFixture)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            printDebugLog(contact, "beginContact(): Damager-Damageable, contact = $contact")
            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE, out
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
                typesSet1(FixtureType.DEATH),
                typesSet2(
                    FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
                )
            )
        ) {
            val (deathFixture, otherFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.DEATH),
                typesSet2(FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY),
                out
            )!!

            val entity = otherFixture.getEntity()
            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTTIY_KILLED_BY_DEATH_FIXTURE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.isProperty(ConstKeys.INSTANT, true)
            if (entity is IDamageable && (instant || !entity.invincible)) otherFixture.depleteHealth()
            else if (deathShouldDestroy(entity)) entity.destroy()
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Block-Body, contact = $contact")

            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val block = blockFixture.getEntity() as Block
            block.hitByBody(bodyFixture)

            if (bodyFixture.hasHitByBlockReceiver(ProcessState.BEGIN))
                bodyFixture.getHitByBlock(ProcessState.BEGIN, block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // body, body
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.BODY)) {
            printDebugLog(contact, "beginContact(): Body-Body, contact = $contact")
            val (bodyFixture1, bodyFixture2) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.BODY, out)!!
            if (bodyFixture1.hasHitByBodyReceiver()) bodyFixture1.getHitByBody(bodyFixture2.getEntity() as IBodyEntity)
            if (bodyFixture2.hasHitByBodyReceiver()) bodyFixture2.getHitByBody(bodyFixture1.getEntity() as IBodyEntity)
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "beginContact(): Body-Feet, contact = $contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            if (bodyFixture.hasHitByFeetReceiver(ProcessState.BEGIN)) bodyFixture.getHitByFeet(
                ProcessState.BEGIN,
                feetFixture,
                delta
            )
        }

        // body, explosion
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.EXPLOSION)) {
            printDebugLog(contact, "beginContact(): Body-Feet, contact = $contact")
            val (bodyFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BODY,
                FixtureType.EXPLOSION,
                out
            )!!
            if (bodyFixture.hasHitByExplosionReceiver()) {
                val explosion = explosionFixture.getEntity() as IBodyEntity
                bodyFixture.getHitByExplosion(explosion)
            }
        }

        // side, block
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.SIDE)) {
            printDebugLog(contact, "beginContact(): Block-Side, contact = $contact")

            val (blockFixture, sideFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.SIDE, out)!!
            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)

            val block = blockFixture.getEntity() as Block
            block.hitBySide(sideFixture)

            if (sideFixture.hasHitByBlockReceiver(ProcessState.BEGIN))
                sideFixture.getHitByBlock(ProcessState.BEGIN, block, delta)
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
                if (gate.state == GateState.OPENABLE) gate.trigger()
            }
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Side-Ice, contact = $contact")
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
            printDebugLog(contact, "beginContact(): Feet-Block, contact = $contact")
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            val body = feetFixture.getBody()
            if (!blockFixture.getBody().physics.collisionOn) {
                body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            val block = blockFixture.getEntity() as Block
            body.addFeetBlock(block)

            val stickToBlock = feetFixture.getOrDefaultProperty(ConstKeys.STICK_TO_BLOCK, true, Boolean::class)
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.translate(posDelta)
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                entity.aButtonTask = AButtonTask.JUMP

                val blockMakesSound =
                    block.getOrDefaultProperty("${ConstKeys.FEET}_${ConstKeys.SOUND}", true, Boolean::class)
                if (blockMakesSound && entity.canMakeLandSound) {
                    entity.requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                    entity.canMakeLandSound = false
                }
            }

            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            block.hitByFeet(feetFixture)
            if (feetFixture.hasHitByBlockReceiver(ProcessState.BEGIN))
                feetFixture.getHitByBlock(ProcessState.BEGIN, block, delta)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "beginContact(): Feet-Ice, contact = $contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            val applyIceFrictionX =
                body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true, Boolean::class)
            if (applyIceFrictionX) body.physics.frictionOnSelf.x = ICE_FRICTION * ConstVals.PPM
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "beginContact(): Feet-Sand, contact = $contact")
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
            printDebugLog(contact, "beginContact(): Feet-Snow, contact = $contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SNOW}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SNOW_FRICTION_X, SNOW_FRICTION_Y).scl(ConstVals.PPM.toFloat())

            if (snowFixture.hasHitByFeetReceiver(ProcessState.BEGIN))
                snowFixture.getHitByFeet(ProcessState.BEGIN, feetFixture, delta)
        }

        // bouncer, feet or head or side
        else if (contact.fixtureSetsMatch(
                typesSet1(FixtureType.BOUNCER),
                typesSet2(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE)
            )
        ) {
            printDebugLog(contact, "beginContact(): Bouncer-Feet/Head/Side, contact = $contact")

            val (bouncerFixture, bounceableFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.BOUNCER),
                typesSet2(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE),
                out
            )!!

            val bounce = bouncerFixture.getVelocityAlteration(bounceableFixture, delta)
            VelocityAlterator.alterate(bounceableFixture.getBody(), bounce)

            bouncerFixture.getRunnable()?.invoke()
            bounceableFixture.getRunnable()?.invoke()
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "beginContact(): Head-Block, contact = $contact")
            val (headFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK, out)!!

            if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            body.physics.velocity.y = 0f

            val blockEntity = blockFixture.getEntity() as Block
            blockEntity.hitByHead(headFixture)

            if (headFixture.hasConsumer()) {
                val consumer = headFixture.getConsumer()
                consumer?.invoke(ProcessState.BEGIN, blockFixture)
            }
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
            val isMegaman = entity is Megaman

            val body = listenerFixture.getBody()
            val wasInWater = body.hasAnyContactWater()

            val water = waterFixture.getEntity() as IWater
            body.setBodySense(BodySense.IN_WATER, true)
            body.addContactWater(water)

            if (listenerFixture.hasHitByWaterByReceiver()) listenerFixture.getHitByWater(water)

            if (!wasInWater && (isMegaman || entity is AbstractEnemy)) {
                val splash = listenerFixture.getOrDefaultProperty(ConstKeys.SPLASH, true, Boolean::class)
                if (splash) {
                    Splash.splashOnWaterSurface(
                        listenerFixture.getBody().getBounds(),
                        waterFixture.getBody().getBounds()
                    )

                    if (water.doMakeSplashSound()) game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
                }
            }

            if (isMegaman) {
                if (!entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
                    !entity.isBehaviorActive(BehaviorType.WALL_SLIDING)
                ) entity.aButtonTask = AButtonTask.SWIM

                entity.gravityScalar = MegamanValues.WATER_GRAVITY_SCALAR
            }
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Head-Ladder, contact = $contact")
            val (headFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER, out)!!

            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
            body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "beginContact(): Feet-Ladder, contact = $contact")
            val (feetFixture, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
            body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "beginContact(): Body-Force, contact = $contact")
            val (bodyFixture, forceFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE, out)!!

            val forceAlteration = forceFixture.getVelocityAlteration(bodyFixture, delta)
            bodyFixture.applyForceAlteration(ProcessState.BEGIN, forceAlteration)

            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, true)
            forceFixture.getRunnable()?.invoke()
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
            printDebugLog(contact, "beginContact(): Body-Player, contact = $contact")
            val (bodyFixture, playerFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.PLAYER, out)!!
            if (bodyFixture.hasHitByPlayerReceiver()) bodyFixture.getHitByPlayer(playerFixture.getEntity() as Megaman)
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
                    FixtureType.PROJECTILE
                ),
            )
        ) {
            printDebugLog(
                contact, "beginContact(): Projectile-Block/Body/Shield/Water, contact = $contact"
            )
            val (projectileFixture, otherFixture) = contact.getFixtureSetsInOrder(
                typesSet1(FixtureType.PROJECTILE),
                typesSet2(
                    FixtureType.BLOCK,
                    FixtureType.BODY,
                    FixtureType.SHIELD,
                    FixtureType.WATER,
                    FixtureType.SAND,
                    FixtureType.PROJECTILE
                ),
                out
            )!!

            if (otherFixture.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)) return

            val projectile1 = projectileFixture.getEntity() as IProjectileEntity
            if (otherFixture.hasHitByProjectileReceiver()) otherFixture.getHitByProjectile(projectile1)

            val thisShape = projectileFixture.getShape().copy()
            val otherShape = otherFixture.getShape().copy()
            when (otherFixture.getType()) {
                FixtureType.BLOCK -> {
                    printDebugLog(contact, "beginContact(): Projectile-Block, contact = $contact")
                    projectile1.hitBlock(otherFixture, thisShape, otherShape)
                    (otherFixture.getEntity() as Block).hitByProjectile(projectileFixture)
                }

                FixtureType.BODY -> {
                    printDebugLog(contact, "beginContact(): Projectile-Body, contact = $contact")
                    projectile1.hitBody(otherFixture, thisShape, otherShape)
                }

                FixtureType.SHIELD -> {
                    printDebugLog(contact, "beginContact(): Projectile-Shield, contact = $contact")
                    projectile1.hitShield(otherFixture, thisShape, otherShape)
                }

                FixtureType.WATER -> {
                    printDebugLog(contact, "beginContact(): Projectile-Water, contact = $contact")
                    projectile1.hitWater(otherFixture, thisShape, otherShape)
                }

                FixtureType.SAND -> {
                    printDebugLog(contact, "beginContact(): Projectile-Sand, contact = $contact")
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
                    printDebugLog(contact, "beginContact(): Projectile-Projectile, contact = $contact")
                    projectile1.hitProjectile(otherFixture, thisShape, otherShape)
                    val projectile2 = otherFixture.getEntity() as IProjectileEntity
                    projectile2.hitProjectile(projectileFixture, otherShape, thisShape)
                }
            }
        }

        // player, item
        else if (contact.fixturesMatch(FixtureType.PLAYER, FixtureType.ITEM)) {
            printDebugLog(contact, "beginContact(): Player-Item, contact = $contact")

            val (playerFixture, itemFixture) = contact.getFixturesInOrder(FixtureType.PLAYER, FixtureType.ITEM, out)!!

            val playerEntity = playerFixture.getEntity()
            val itemEntity = itemFixture.getEntity()

            if (playerEntity is Megaman && itemEntity is ItemEntity) itemEntity.contactWithPlayer(playerEntity)
        }

        // player feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "beginContact(): Feet-Cart, contact = $contact")
            val (feetFixture, cartFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART, out)!!

            val feetEntity = feetFixture.getEntity()
            val cartEntity = cartFixture.getEntity() as IOwnable

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
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER, out
            )!!

            val teleporterListener = teleporterListenerFixture.getEntity() as IBodyEntity
            teleporterListener.body.setBodySense(BodySense.TELEPORTING, true)

            val teleporterEntity = teleporterFixture.getEntity() as ITeleporterEntity
            teleporterEntity.teleportEntity(teleporterListener)
        }

        // laser, block
        else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BLOCK)) {
            printDebugLog(contact, "continueContact(): Laser-Block, contact = $contact")
            val (laserFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK, out)!!

            val laserEntity = laserFixture.getEntity()
            val blockEntity = blockFixture.getEntity()

            if (laserEntity != blockEntity) {
                val blockRectangle = blockFixture.getShape() as GameRectangle
                val laserLine = laserFixture.getProperty(ConstKeys.LINE, GameLine::class)!!

                val intersections = laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<Vector2>

                if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Arr))
                    intersections.addAll(tempVec2Arr)

                tempVec2Arr.clear()
            }
        }
    }

    override fun continueContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // consumer
        if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
            val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER, out)!!
            consumer.getConsumer()?.invoke(ProcessState.CONTINUE, consumable)
        }

        // damager, damageable
        else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
            val (damagerFixture, damageableFixture) = contact.getFixturesInOrder(
                FixtureType.DAMAGER, FixtureType.DAMAGEABLE, out
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

            val canDie = entity.getOrDefaultProperty(ConstKeys.ENTTIY_KILLED_BY_DEATH_FIXTURE, true, Boolean::class)
            if (!canDie) return

            val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
            if (!deathListener) return

            val instant = deathFixture.getProperty(ConstKeys.INSTANT, Boolean::class) == true
            if (entity is IDamageable && (instant || !entity.invincible)) otherFixture.depleteHealth()
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "endContact(): Block-Body, contact = $contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!

            if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

            val block = blockFixture.getEntity() as Block
            if (bodyFixture.hasHitByBlockReceiver(ProcessState.CONTINUE))
                bodyFixture.getHitByBlock(ProcessState.CONTINUE, block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            val body = feetFixture.getBody()
            if (!blockFixture.getBody().physics.collisionOn) {
                body.setBodySense(BodySense.FEET_ON_GROUND, false)
                return
            }

            val block = blockFixture.getEntity() as Block
            body.addFeetBlock(block)

            val stickToBlock = feetFixture.getOrDefaultProperty(ConstKeys.STICK_TO_BLOCK, true, Boolean::class)
            if (stickToBlock) {
                val posDelta = blockFixture.getBody().getPositionDelta()
                body.translate(posDelta)
            }

            val entity = feetFixture.getEntity()
            if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

            body.setBodySense(BodySense.FEET_ON_GROUND, true)
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
            printDebugLog(contact, "continueContact(): Feet-Sand, contact = $contact")
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
            printDebugLog(contact, "continueContact(): Feet-Snow, contact = $contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, true)

            val takeFriction =
                body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SNOW}", true, Boolean::class)
            if (takeFriction)
                body.physics.frictionOnSelf.set(SNOW_FRICTION_X, SNOW_FRICTION_Y).scl(ConstVals.PPM.toFloat())


            if (snowFixture.hasHitByFeetReceiver(ProcessState.CONTINUE))
                snowFixture.getHitByFeet(ProcessState.CONTINUE, feetFixture, delta)
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "continueContact(): Body-Feet, contact = $contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            if (bodyFixture.hasHitByFeetReceiver(ProcessState.CONTINUE)) bodyFixture.getHitByFeet(
                ProcessState.CONTINUE,
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

            val forceAlteration = forceFixture.getVelocityAlteration(bodyFixture, delta)
            bodyFixture.applyForceAlteration(ProcessState.CONTINUE, forceAlteration)

            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, true)
            forceFixture.getRunnable()?.invoke()
        }

        // body, explosion
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.EXPLOSION)) {
            val (bodyFixture, explosionFixture) = contact.getFixturesInOrder(
                FixtureType.BODY,
                FixtureType.EXPLOSION,
                out
            )!!
            if (bodyFixture.hasHitByExplosionReceiver()) {
                val explosion = explosionFixture.getEntity() as IBodyEntity
                bodyFixture.getHitByExplosion(explosion)
            }
        }

        // TODO: should this be in the continue contact phase?
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
            printDebugLog(contact, "continueContact(): Laser-Block, contact = $contact")
            val (laserFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK, out)!!

            val laserEntity = laserFixture.getEntity()
            val blockEntity = blockFixture.getEntity()

            if (laserEntity != blockEntity) {
                val blockRectangle = blockFixture.getShape() as GameRectangle
                val laserLine = laserFixture.getProperty(ConstKeys.LINE, GameLine::class)!!

                val intersections = laserFixture.getProperty(ConstKeys.COLLECTION) as MutableCollection<Vector2>

                if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, tempVec2Arr))
                    intersections.addAll(tempVec2Arr)

                tempVec2Arr.clear()
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

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
        }
    }

    override fun endContact(contact: Contact, delta: Float) {
        if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

        // side, block
        if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Side-Block, contact = $contact")
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK, out)!!

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
        }

        // side, ice
        else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
            printDebugLog(contact, "End Contact: Side-Ice, contact = $contact")
            val (sideFixture, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE, out)!!

            val body = sideFixture.getBody()

            val sideType = sideFixture.getProperty(ConstKeys.SIDE)
            if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
            else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
        }

        // feet, block
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Feet-Block, contact = $contact")
            val (feetFixture, blockFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK, out)!!

            val block = blockFixture.getEntity() as Block
            val body = feetFixture.getBody()
            body.removeFeetBlock(block)
            if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)

            val entity = feetFixture.getEntity()
            if (entity is Megaman) {
                entity.aButtonTask =
                    if (entity.body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH

                if (!body.hasAnyFeetBlock()) entity.canMakeLandSound = true
            } else body.setBodySense(BodySense.FEET_ON_GROUND, false)
        }

        // feet, ice
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
            printDebugLog(contact, "End Contact: Feet-Ice, contact = $contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_ICE, false)
        }

        // feet, sand
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SAND)) {
            printDebugLog(contact, "endContact(): Feet-Sand, contact = $contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SAND, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SAND, false)
        }

        // feet, snow
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.SNOW)) {
            printDebugLog(contact, "endContact(): Feet-Snow, contact = $contact")
            val (feetFixture, snowFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.SNOW, out)!!

            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_ON_SNOW, false)

            if (snowFixture.hasHitByFeetReceiver(ProcessState.END))
                snowFixture.getHitByFeet(ProcessState.END, feetFixture, delta)
        }

        // head, block
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
            printDebugLog(contact, "End Contact: Head-Block, contact = $contact")
            val (headFixture, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK, out)!!
            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
        }

        // block, body
        else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.BODY)) {
            printDebugLog(contact, "endContact(): Block-Body, contact = $contact")
            val (blockFixture, bodyFixture) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.BODY, out)!!

            val block = blockFixture.getEntity() as Block
            if (bodyFixture.hasHitByBlockReceiver(ProcessState.END))
                bodyFixture.getHitByBlock(ProcessState.END, block, delta)

            val body = bodyFixture.getBody()
            body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, false)
        }

        // body, feet
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FEET)) {
            printDebugLog(contact, "endContact(): Body-Feet, contact = $contact")
            val (bodyFixture, feetFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FEET, out)!!
            if (bodyFixture.hasHitByFeetReceiver(ProcessState.END)) bodyFixture.getHitByFeet(
                ProcessState.END,
                feetFixture,
                delta
            )
        }

        // feet, ladder
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
            printDebugLog(contact, "End Contact: Feet-Ladder, contact = $contact")
            val (feetFixture, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER, out)!!
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)
            if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // head, ladder
        else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
            printDebugLog(contact, "End Contact: Head-Ladder, contact = $contact")
            val (headFixture, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER, out)!!
            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)
            if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
        }

        // body, force
        else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
            printDebugLog(contact, "End Contact: Body-Force, contact = $contact")
            val (bodyFixture, forceFixture) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE, out)!!
            val forceAlteration = forceFixture.getVelocityAlteration(bodyFixture, delta)
            bodyFixture.applyForceAlteration(ProcessState.END, forceAlteration)
            bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, false)
            forceFixture.getRunnable()?.invoke()
        }

        // water listener, water
        else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
            printDebugLog(contact, "End contact: Water-Listener, Water. Contact = $contact")
            val (listenerFixture, waterFixture) = contact.getFixturesInOrder(
                FixtureType.WATER_LISTENER, FixtureType.WATER, out
            )!!

            val water = waterFixture.getEntity() as IWater

            val body = listenerFixture.getBody()
            body.removeContactWater(water)
            if (!body.hasAnyContactWater()) body.setBodySense(BodySense.IN_WATER, false)

            val listener = listenerFixture.getEntity()

            if (!body.isSensing(BodySense.IN_WATER) && listener is Megaman) {
                Splash.splashOnWaterSurface(listenerFixture.getBody().getBounds(), waterFixture.getBody().getBounds())
                listener.aButtonTask = AButtonTask.AIR_DASH
                listener.gravityScalar = 1f
                game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
            }
        }

        // player feet, cart
        else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.CART)) {
            printDebugLog(contact, "endContact(): Feet-Cart, contact = $contact")
            val (feetFixture, cartFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.CART, out)!!

            val feetEntity = feetFixture.getEntity()
            val cartEntity = cartFixture.getEntity() as IOwnable
            cartEntity.owner = null

            if (feetEntity is Megaman && cartEntity is Cart) {
                feetEntity.body.setBodySense(BodySense.TOUCHING_CART, false)
                feetEntity.body.removeProperty(ConstKeys.CART)
            }
        }

        // teleporter listener, teleporter
        else if (contact.fixturesMatch(FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER)) {
            printDebugLog(contact, "endContact(): TeleporterListener-Teleporter, contact = $contact")
            val (teleporterListenerFixture, _) = contact.getFixturesInOrder(
                FixtureType.TELEPORTER_LISTENER, FixtureType.TELEPORTER, out
            )!!
            teleporterListenerFixture.getBody().setBodySense(BodySense.TELEPORTING, false)
        }
    }

    private fun printDebugLog(contact: Contact, log: String) {
        if (contactDebugFilter.invoke(contact)) {
            GameLogger.debug(TAG, log)
        }
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
