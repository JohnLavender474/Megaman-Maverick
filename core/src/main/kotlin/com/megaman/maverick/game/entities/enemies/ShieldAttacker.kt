package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class ShieldAttacker(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFreezableEntity, IFaceable {

    companion object {
        const val TAG = "ShieldAttacker"

        private const val X_VEL = 6f

        private const val CULL_TIME = 5f
        private const val TURN_DUR = 0.5f
        private const val FROZEN_DUR = 0.5f

        private val animDefs = orderedMapOf(
            "frozen" pairTo AnimationDef(),
            "turn" pairTo AnimationDef(1, 5, 0.1f, false),
            "attack" pairTo AnimationDef(1, 2, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }
    override lateinit var facing: Facing

    private val canMove: Boolean
        get() = !game.isCameraRotating()

    private val turnTimer = Timer(TURN_DUR)
    private val turning: Boolean
        get() = !turnTimer.isFinished()

    private val frozenTimer = Timer(FROZEN_DUR)

    private lateinit var animations: ObjectMap<String, IAnimation>

    private var min = 0f
    private var max = 0f

    private var vertical = false
    private var switch = false
    private var flipY = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        vertical = spawnProps.getOrDefault(ConstKeys.VERTICAL, false, Boolean::class)
        when {
            vertical -> {
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
                facing = if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            }

            else -> {
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
                facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            }
        }

        val frameDuration = spawnProps.getOrDefault(ConstKeys.FRAME, 0.1f, Float::class)
        animations.forEach { it.value.setFrameDuration(frameDuration) }

        flipY = spawnProps.getOrDefault("${ConstKeys.FLIP}_${ConstKeys.Y}", false, Boolean::class)
        turnTimer.reset()
        frozen = false
    }

    override fun canBeDamagedBy(damager: IDamager) = !frozen && super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is IFreezerEntity) frozen = true
        return damaged
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!canMove) {
                body.physics.velocity.setZero()
                return@add
            }

            if (frozen) {
                body.physics.velocity.setZero()

                frozenTimer.update(delta)
                if (frozenTimer.isJustFinished()) {
                    damageTimer.reset()
                    IceShard.spawn5(body.getCenter())
                }

                return@add
            }

            when {
                vertical -> {
                    val centerY = body.getCenter().y
                    if (centerY < min || centerY > max) {
                        turnTimer.reset()
                        body.setCenterY(if (centerY < min) min else max)
                        body.physics.velocity.setZero()
                        switch = centerY >= max
                    }

                    turnTimer.update(delta)
                    if (turnTimer.isFinished()) {
                        val y = X_VEL * ConstVals.PPM * (if (switch) -1 else 1) * movementScalar
                        body.physics.velocity.y = y
                        GameLogger.debug(TAG, "Turning around. New y vel: $y")
                    }
                }

                else -> {
                    val centerX = body.getCenter().x
                    if (centerX < min || centerX > max) {
                        turnTimer.reset()
                        body.setCenterX(if (centerX < min) min else max)
                        body.physics.velocity.setZero()
                        switch = centerX >= max
                    }

                    turnTimer.update(delta)
                    if (turnTimer.isFinished()) {
                        val x = X_VEL * ConstVals.PPM * (if (switch) -1 else 1) * movementScalar
                        body.physics.velocity.x = x
                        GameLogger.debug(TAG, "Turning around. New x vel: $x")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY)
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableRect = GameRectangle(body)
        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damageableRect)
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val shieldRect = GameRectangle(body)
        val shieldFixture = Fixture(body, FixtureType.SHIELD, shieldRect)
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.bodyAttachmentPosition = Position.BOTTOM_CENTER
        body.addFixture(feetFixture)

        val frozenFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(frozenFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            frozenFixture.setActive(frozen)

            bodyFixture.setShape(body.getBounds())
            damagerFixture.setShape(body.getBounds())

            when {
                vertical -> {
                    body.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
                    damageableRect.setWidth(body.getWidth())
                    shieldRect.setSize(1.75f * ConstVals.PPM, 0.85f * ConstVals.PPM)
                }

                else -> {
                    body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
                    damageableRect.setHeight(body.getHeight())
                    shieldRect.setSize(0.85f * ConstVals.PPM, 1.75f * ConstVals.PPM)
                }
            }

            when {
                turning -> {
                    shieldFixture.setActive(false)
                    damageableFixture.offsetFromBodyAttachment.x = 0f
                    when {
                        vertical -> damageableRect.setHeight(0.85f * ConstVals.PPM)
                        else -> damageableRect.setWidth(0.85f * ConstVals.PPM)
                    }
                }

                else -> {
                    shieldFixture.setActive(true)
                    when {
                        vertical -> {
                            damageableFixture.offsetFromBodyAttachment.y = (if (switch) 0.5f else -0.5f) * ConstVals.PPM
                            damageableRect.setHeight(0.15f * ConstVals.PPM)
                        }

                        else -> {
                            damageableFixture.offsetFromBodyAttachment.x = (if (switch) 0.5f else -0.5f) * ConstVals.PPM
                            damageableRect.setWidth(0.15f * ConstVals.PPM)
                        }
                    }
                }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(turning != switch, flipY)
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = if (vertical) 90f else 0f
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (frozen) "frozen" else if (turning) "turn" else "attack" }
        animations = ObjectMap<String, IAnimation>().also { animations ->
            animDefs.forEach { entry ->
                val key = entry.key
                val (rows, columns, durations, loop) = entry.value
                animations.put(key, Animation(regions[key], rows, columns, durations, loop))
            }
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
