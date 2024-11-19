package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class ShieldAttacker(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "ShieldAttacker"
        private const val TURN_AROUND_DUR = 0.5f
        private const val X_VEL = 6f
        private const val CULL_TIME = 5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 5
        }
    )
    override lateinit var facing: Facing

    private val turnAroundTimer = Timer(TURN_AROUND_DUR)
    private val canMove: Boolean
        get() = !game.isCameraRotating()
    private val turningAround: Boolean
        get() = !turnAroundTimer.isFinished()

    private lateinit var animations: ObjectMap<String, IAnimation>

    private var min = 0f
    private var max = 0f

    private var vertical = false
    private var switch = false
    private var flipY = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("turn", atlas.findRegion("$TAG/TurnAround"))
            regions.put("attack", atlas.findRegion("$TAG/Attack"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        vertical = spawnProps.getOrDefault(ConstKeys.VERTICAL, false, Boolean::class)
        if (vertical) {
            body.setSize(1.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
            val targetY = spawn.y + spawnProps.get(ConstKeys.VALUE, Float::class)!! * ConstVals.PPM
            when {
                spawn.y < targetY -> {
                    min = spawn.y
                    max = targetY
                    switch = false
                }

                else -> {
                    min = targetY
                    max = spawn.y
                    switch = true
                }
            }
            body.setCenter(spawn)
            facing = if (megaman().body.y < body.y) Facing.LEFT else Facing.RIGHT
        } else {
            body.setSize(0.75f * ConstVals.PPM, 1.5f * ConstVals.PPM)
            val targetX = spawn.x + spawnProps.get(ConstKeys.VALUE, Float::class)!! * ConstVals.PPM
            when {
                spawn.x < targetX -> {
                    min = spawn.x
                    max = targetX
                    switch = false
                }

                else -> {
                    min = targetX
                    max = spawn.x
                    switch = true
                }
            }
            body.setCenter(spawn)
            facing = if (megaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        }

        val frameDuration = spawnProps.getOrDefault(ConstKeys.FRAME, 0.1f, Float::class)
        animations.forEach { it.value.setFrameDuration(frameDuration) }

        turnAroundTimer.reset()

        flipY = spawnProps.getOrDefault("${ConstKeys.FLIP}_${ConstKeys.Y}", false, Boolean::class)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY)
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture.rawShape }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            (bodyFixture.rawShape as GameRectangle).set(body)
            (damagerFixture.rawShape as GameRectangle).set(body)

            when {
                vertical -> {
                    body.setSize(1.75f * ConstVals.PPM, 0.85f * ConstVals.PPM)
                    (damageableFixture.rawShape as GameRectangle).width = body.width
                    (shieldFixture.rawShape as GameRectangle).setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)
                }

                else -> {
                    body.setSize(0.85f * ConstVals.PPM, 1.75f * ConstVals.PPM)
                    (damageableFixture.rawShape as GameRectangle).height = body.height
                    (shieldFixture.rawShape as GameRectangle).setSize(0.75f * ConstVals.PPM, 1.25f * ConstVals.PPM)
                }
            }

            val damageableShape = damageableFixture.rawShape as GameRectangle
            when {
                turningAround -> {
                    shieldFixture.active = false
                    damageableFixture.offsetFromBodyCenter.x = 0f
                    when {
                        vertical -> damageableShape.height = 0.5f * ConstVals.PPM
                        else -> damageableShape.width = 0.5f * ConstVals.PPM
                    }
                }

                else -> {
                    shieldFixture.active = true
                    when {
                        vertical -> {
                            damageableFixture.offsetFromBodyCenter.y = (if (switch) 0.5f else -0.5f) * ConstVals.PPM
                            damageableShape.height = 0.15f * ConstVals.PPM
                        }

                        else -> {
                            damageableFixture.offsetFromBodyCenter.x = (if (switch) 0.5f else -0.5f) * ConstVals.PPM
                            damageableShape.width = 0.15f * ConstVals.PPM
                        }
                    }
                }
            }
        })

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (!canMove) {
                body.physics.velocity.setZero()
                return@add
            }

            when {
                vertical -> {
                    val centerY = body.getCenter().y
                    if (centerY < min || centerY > max) {
                        turnAroundTimer.reset()
                        body.setCenterY(if (centerY < min) min else max)
                        body.physics.velocity.setZero()
                        switch = centerY >= max
                    }

                    turnAroundTimer.update(it)
                    if (turnAroundTimer.isJustFinished()) {
                        val y = X_VEL * ConstVals.PPM * (if (switch) -1 else 1) * movementScalar
                        body.physics.velocity.y = y
                        GameLogger.debug(TAG, "Turning around. New y vel: $y")
                    }
                }

                else -> {
                    val centerX = body.getCenter().x
                    if (centerX < min || centerX > max) {
                        turnAroundTimer.reset()
                        body.setCenterX(if (centerX < min) min else max)
                        body.physics.velocity.setZero()
                        switch = centerX >= max
                    }

                    turnAroundTimer.update(it)
                    if (turnAroundTimer.isJustFinished()) {
                        val x = X_VEL * ConstVals.PPM * (if (switch) -1 else 1) * movementScalar
                        body.physics.velocity.x = x
                        GameLogger.debug(TAG, "Turning around. New x vel: $x")
                    }
                }
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(turningAround != switch, flipY)
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = if (vertical) 90f else 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (turningAround) "turn" else "attack" }
        animations = objectMapOf(
            "turn" pairTo Animation(regions["turn"], 1, 5, 0.1f, false),
            "attack" pairTo Animation(regions["attack"], 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
