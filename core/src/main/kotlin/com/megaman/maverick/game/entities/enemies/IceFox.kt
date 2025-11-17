package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.IceBomb
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class IceFox(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "IceFox"

        private const val STAND_DUR = 1.5f

        private const val SHOOT_DUR = 2f
        private const val SHOOT_DUR_HARD = 3f
        private const val SHOTS = 3
        private const val SHOTS_HARD = 6
        private const val SHOOT_DELAY = 0.5f
        private const val SHOOT_OFFSET_X = 1f
        private const val SHOOT_OFFSET_Y = 0.75f
        private const val SHOOT_X_VEL = 8f
        private const val SHOOT_Y_VEL = 5f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.5f), false),
            "shoot" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class IceFoxState { STAND, SHOOT }

    override lateinit var facing: Facing

    private val loop = Loop(IceFoxState.entries.toGdxArray())
    private val currentState: IceFoxState
        get() = loop.getCurrent()

    private lateinit var timers: ObjectMap<IceFoxState, Timer>

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        FacingUtils.setFacingOf(this)

        loop.reset()

        timers = orderedMapOf(
            IceFoxState.STAND pairTo Timer(STAND_DUR),
            IceFoxState.SHOOT pairTo Timer(if (game.state.hardMode) SHOOT_DUR_HARD else SHOOT_DUR).also { timer ->
                val shots = if (game.state.getDifficultyMode() == DifficultyMode.HARD) SHOTS_HARD else SHOTS
                for (i in 1..shots) {
                    val time = i * SHOOT_DELAY
                    val runnable = TimeMarkedRunnable(time) { shoot() }
                    timer.addRunnable(runnable)
                }
            }
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when (damager) {
        is IFireEntity -> ConstVals.MAX_HEALTH
        is IFreezerEntity -> 1
        else -> baseDamage
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (game.isCameraRotating()) return@add

            val timer = timers[currentState]
            timer.update(delta)
            if (timer.isFinished()) {
                loop.next()
                timer.reset()
            }

            if (currentState == IceFoxState.STAND) FacingUtils.setFacingOf(this)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")

        val spawn = body.getCenter()
            .add(SHOOT_OFFSET_X * facing.value * ConstVals.PPM, SHOOT_OFFSET_Y * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(SHOOT_X_VEL * facing.value, SHOOT_Y_VEL)
            .scl(ConstVals.PPM.toFloat())

        val bomb = MegaEntityFactory.fetch(IceBomb::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }
}
