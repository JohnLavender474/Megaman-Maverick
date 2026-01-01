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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.SineWave
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
import com.megaman.maverick.game.entities.projectiles.BouncingPebble
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CarriCarry(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFreezableEntity, IMotionEntity, IAnimatedEntity,
    IFaceable {

    companion object {
        const val TAG = "CarriCarry"

        private const val SINE_SPEED = 3f
        private const val SINE_AMPLITUDE = 3f
        private const val SINE_FREQUENCY = 3f

        private const val SHAKE_DUR = 1f

        private const val CULL_TIME = 3f

        private const val THROW_PEBBLE_IMPULSE_X = 6f
        private const val THROW_PEBBLE_IMPULSE_Y = 4f

        private const val CRUMBLE_MAX_BOUNCES = 1
        private val CRUMBLE_PEBBLE_IMPULSES = gdxArrayOf(
            Vector2(-4f, 4f),
            Vector2(-2f, 6f),
            Vector2(2f, 6f),
            Vector2(4f, 4f)
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(
        this,
        onFrozen = { shakeTimer.setToEnd() }
    )

    private val shakeTimer = Timer(SHAKE_DUR)
    private var centerX = 0f

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("ride", "shake", "frozen").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(MotionComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

        shakeTimer.setToEnd()

        centerX = body.getCenter().x

        val sine = SineWave(Vector2(0f, 1f), SINE_SPEED, SINE_AMPLITUDE, SINE_FREQUENCY)
        putMotionDefinition(
            ConstKeys.MOVE, MotionDefinition(
                motion = sine,
                function = { value, _ ->
                    if (frozen) return@MotionDefinition
                    body.setCenterX(centerX + value.y)
                    GameLogger.debug(TAG, "set to center x = ${body.getCenter().x}")
                },
                doUpdate = { !frozen && shakeTimer.isFinished() }
            )
        )

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        if (isHealthDepleted()) crumble()
        frozen = false
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damageTaken = super.takeDamageFrom(damager)

        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager, damageTaken=$damageTaken")

        if (damageTaken && !isHealthDepleted()) {
            shakeTimer.reset()
            throwPebble()
        }

        return damageTaken
    }

    private fun throwPebble() {
        GameLogger.debug(TAG, "throwPebble()")

        val spawn = body.getPositionPoint(Position.TOP_CENTER)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(THROW_PEBBLE_IMPULSE_X * facing.value, THROW_PEBBLE_IMPULSE_Y)
            .scl(ConstVals.PPM.toFloat())

        val pebble = MegaEntityFactory.fetch(BouncingPebble::class)!!
        pebble.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.IMPULSE pairTo impulse))
    }

    private fun crumble() {
        GameLogger.debug(TAG, "crumble()")

        CRUMBLE_PEBBLE_IMPULSES.forEach {
            val spawn = body.getPositionPoint(Position.TOP_CENTER)

            val impulse = GameObjectPools.fetch(Vector2::class).set(it).scl(ConstVals.PPM.toFloat())

            val pebble = MegaEntityFactory.fetch(BouncingPebble::class)!!
            pebble.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse,
                    "${ConstKeys.MAX}_${ConstKeys.BOUNCE}" pairTo CRUMBLE_MAX_BOUNCES
                )
            )
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)
            if (frozen) return@add

            facing = if (megaman.body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

            shakeTimer.update(delta)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                1.75f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        damageableFixture.offsetFromBodyAttachment.y = 0.75f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.SHIELD, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (frozen) "frozen"
            else if (!shakeTimer.isFinished()) "shake"
            else "ride"
        }
        val animations = objectMapOf<String, IAnimation>(
            "frozen" pairTo Animation(regions.get("frozen")),
            "shake" pairTo Animation(regions.get("shake"), 2, 1, 0.1f, true),
            "ride" pairTo Animation(regions.get("ride"), 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
