package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.JoeBall
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SwinginJoe(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFreezableEntity, IFaceable {

    companion object {
        const val TAG = "SwinginJoe"
        private const val BALL_SPEED = 9f
        private const val SETTING_DUR = .8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SwinginJoeState { SWING_EYES_CLOSED, SWING_EYES_OPEN, THROWING }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(
        this,
        onFrozen = {
            loop.reset()
            stateTimer.reset()
        }
    )

    private val loop = Loop(SwinginJoeState.entries.toGdxArray())
    private val currentState: SwinginJoeState
        get() = loop.getCurrent()
    private val stateTimer = Timer(SETTING_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("swing1", "swing2", "throw", "frozen").forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        loop.reset()
        stateTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun canBeDamagedBy(damager: IDamager) =
        super.canBeDamagedBy(damager) && (damager is Axe || currentState != SwinginJoeState.SWING_EYES_CLOSED)

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
        body.physics.gravityOn = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()

            shieldFixture.setActive(currentState == SwinginJoeState.SWING_EYES_CLOSED)

            when (currentState) {
                SwinginJoeState.SWING_EYES_CLOSED -> {
                    damageableFixture.offsetFromBodyAttachment.x = 0.05f * ConstVals.PPM * -facing.value
                    shieldFixture.offsetFromBodyAttachment.x = 0.1f * ConstVals.PPM * facing.value
                }
                else -> damageableFixture.offsetFromBodyAttachment.x = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)
            if (frozen) return@add

            facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

            stateTimer.update(delta)
            if (stateTimer.isJustFinished()) {
                val next = loop.next()
                if (next == SwinginJoeState.THROWING) throwBall()
                stateTimer.reset()
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(-0.5f * ConstVals.PPM * facing.value)
            sprite.setFlip(facing == Facing.LEFT, false)
            sprite.hidden = damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (frozen) "frozen" else when (currentState) {
                SwinginJoeState.SWING_EYES_CLOSED -> "swing1"
                SwinginJoeState.SWING_EYES_OPEN -> "swing2"
                SwinginJoeState.THROWING -> "throw"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "swing1" pairTo Animation(regions["swing1"], 2, 2, 0.1f, true),
            "swing2" pairTo Animation(regions["swing2"], 2, 2, 0.1f, true),
            "throw" pairTo Animation(regions["throw"]),
            "frozen" pairTo Animation(regions["frozen"]),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwBall() {
        val spawn = body.getCenter().add(0.25f * facing.value * ConstVals.PPM, 0f)

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(BALL_SPEED * ConstVals.PPM * facing.value, 0f)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.MASK pairTo objectSetOf<KClass<out IDamageable>>(Megaman::class)
        )

        val ball = MegaEntityFactory.fetch(JoeBall::class)!!
        ball.spawn(props)
    }
}
