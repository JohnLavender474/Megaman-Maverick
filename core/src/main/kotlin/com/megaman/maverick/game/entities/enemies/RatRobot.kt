package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.equalsAny
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.players.any
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.bosses.RodentMan
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.players
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class RatRobot(game: MegaServices) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFreezableEntity,
    IFaceable {

    companion object {
        const val TAG = "RatRobot"

        private const val NORMAL_SPEED = 4f
        private const val HARD_SPEED = 6f

        private const val GRAVITY = 56.25f

        private val animDefs = orderedMapOf(
            "run" pairTo AnimationDef(3, 1, 0.1f, true),
            "still" pairTo AnimationDef(),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private val triggers = Array<GameRectangle>()
    private var triggered = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
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

    override fun canBeDamagedBy(damager: IDamagerEntity) = damager !is RodentMan && super.canBeDamagedBy(damager)

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        triggers.forEach { GameObjectPools.free(it) }
        triggers.clear()

        frozen = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) return@add

            if (!triggered &&
                players.any { player ->
                    !player.isBehaviorActive(BehaviorType.CLIMBING) &&
                        triggers.any { it.overlaps(player.body.getBounds()) }
                }
            ) {
                triggered = true
                facing = if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
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
        leftFootFixture.setReceiver(Receivers.FILTER) { fixture ->
            fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH)
        }
        leftFootFixture.setReceiver(Receivers.CONSUMER) { _, fixture ->
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
        rightFootFixture.setReceiver(Receivers.FILTER) { fixture ->
            fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH)
        }
        rightFootFixture.setReceiver(Receivers.CONSUMER) { _, fixture ->
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
