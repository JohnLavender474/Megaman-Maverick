package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
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
import com.mega.game.engine.common.extensions.equalsAny
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
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.bosses.RodentMan
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class RatRobot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFreezableEntity,
    IFaceable {

    companion object {
        const val TAG = "RatRobot"

        private const val NORMAL_SPEED = 4f
        private const val HARD_SPEED = 6f

        private const val GRAVITY = 0.375f

        private const val FROZEN_DUR = 0.5f

        private val animDefs = orderedMapOf(
            "run" pairTo AnimationDef(3, 1, 0.1f, true),
            "still" pairTo AnimationDef(),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing
    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }

    private val frozenTimer = Timer(FROZEN_DUR)

    private val triggers = Array<GameRectangle>()
    private var triggered = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.POSITION) -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else -> spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        }
        body.setCenter(spawn)

        FacingUtils.setFacingOf(this)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.TRIGGER)) {
                val trigger = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                triggers.add(trigger)
            }
        }
        triggered = triggers.isEmpty

        frozen = false

        GameLogger.debug(
            TAG,
            "onSpawn(): triggered=$triggered, triggers=${triggers.size}, facing=$facing, spawnProps=$spawnProps"
        )
    }

    override fun canBeDamagedBy(damager: IDamager) = damager !is RodentMan && super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged && !frozen && damager is IFreezerEntity) frozen = true
        return damaged
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        triggers.forEach { GameObjectPools.free(it) }
        triggers.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (frozen) {
                frozenTimer.update(delta)
                if (frozenTimer.isJustFinished()) IceShard.spawn5(body.getCenter())
                return@add
            }

            if (!triggered &&
                !megaman.isBehaviorActive(BehaviorType.CLIMBING) &&
                triggers.any { it.overlaps(megaman.body.getBounds()) }
            ) {
                triggered = true
                facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        val leftFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        leftFootFixture.offsetFromBodyAttachment.set(
            (-0.5f * ConstVals.PPM) - (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        leftFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        leftFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> leftFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> leftFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(leftFootFixture)
        debugShapes.add { leftFootFixture }

        val rightFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        rightFootFixture.offsetFromBodyAttachment.set(
            (0.5f * ConstVals.PPM) + (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        rightFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        rightFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> rightFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> rightFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(rightFootFixture)
        debugShapes.add { rightFootFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            leftFootFixture.putProperty(ConstKeys.BLOCK, false)
            leftFootFixture.putProperty(ConstKeys.DEATH, false)
            rightFootFixture.putProperty(ConstKeys.BLOCK, false)
            rightFootFixture.putProperty(ConstKeys.DEATH, false)

            body.physics.gravity.y = if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else -GRAVITY * ConstVals.PPM
            body.physics.gravityOn = triggered

            body.physics.velocity.x = when {
                frozen || !body.isSensing(BodySense.FEET_ON_GROUND) -> 0f
                else -> when (game.state.getDifficultyMode()) {
                    DifficultyMode.NORMAL -> NORMAL_SPEED
                    DifficultyMode.HARD -> HARD_SPEED
                } * ConstVals.PPM * facing.value
            }

            body.forEachFixture { it.setActive(triggered) }
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.FEET_ON_GROUND) && !frozen) {
                if (isFacing(Facing.LEFT)) {
                    if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                        leftFootFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                        leftFootFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                    ) facing = Facing.RIGHT
                } else if (isFacing(Facing.RIGHT)) {
                    if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ||
                        rightFootFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                        rightFootFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                    ) facing = Facing.LEFT
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(isFacing(Facing.LEFT), false)

            sprite.hidden = !triggered || damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                frozen -> "frozen"
                body.isSensing(BodySense.FEET_ON_GROUND) -> "run"
                else -> "still"
            }
        }
        val animations = ObjectMap<String, IAnimation>().also { animations ->
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
