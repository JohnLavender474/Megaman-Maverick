package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.X6FrameDef
import com.megaman.maverick.game.entities.utils.X6Frames
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MetaDridler(services: MegaServices) : AbstractEnemy(services, size = Size.LARGE), IAnimatedEntity {

    companion object {
        const val TAG = "MetaDridler"

        private const val WAIT_DUR = 1.5f
        private const val HARD_WAIT_DUR = 0.75f
        private const val DROP_WARNING_DUR = 0.5f
        private const val HARD_DROP_WARNING_DUR = 0.25f
        private const val DRILL_DUR = 1f
        private const val RISE_WARNING_DUR = 0.5f
        private const val RISE_SOUND_DELAY = 1f

        private const val DROP_START_SPEED = 0.12f
        private const val GRAVITY = -14f
        private const val MAX_DROP = 4f
        private const val RISE_SPEED = 0.94f

        private const val CONTACT_WIDTH = 0.875f
        private const val CONTACT_HEIGHT = 2.5f
        private const val TIP_BELOW_ORIGIN = 1.1875f

        val FRAMES = orderedMapOf(
            "wait" pairTo X6FrameDef(32, 63, 16, 32, 4, intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), 0),
            "drill" pairTo X6FrameDef(32, 63, 16, 32, 4, intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), 0),
            "rise" pairTo X6FrameDef(32, 63, 16, 32, 4, intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), 0),
            "flame" pairTo X6FrameDef(
                37, 15, 18, -7, 6, intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(1, 2, 1, 1, 1, 2), 0
            ),
            "flame_drop" pairTo X6FrameDef(
                37, 24, 18, 46, 6, intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(1, 2, 1, 1, 1, 2), 0
            ),
            "warn_drop" pairTo X6FrameDef(
                32, 4, 16, 3, 4, intArrayOf(0, 1, 2, 3, 2, 1), intArrayOf(4, 3, 2, 1, 2, 3), 0
            ),
            "warn_rise" pairTo X6FrameDef(
                32, 4, 16, 13, 4, intArrayOf(0, 1, 2, 3, 2, 1), intArrayOf(4, 3, 2, 1, 2, 3), 0
            )
        )
    }

    enum class MetaDridlerState { WAIT, DROP, DRILL, RISE }

    lateinit var state: MetaDridlerState
        private set

    private val animations = ObjectMap<String, IAnimation>()
    private val home = Vector2()
    private val waitTimer = Timer(WAIT_DUR)
    private val drillTimer = Timer(DRILL_DUR)
    private val riseSoundTimer = Timer(RISE_SOUND_DELAY)

    private val origin: Vector2
        get() = body.getCenter()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        X6Frames.animations(atlas, TAG, FRAMES, animations)
        super.init()
        addComponent(
            AnimationsComponentBuilder(this)
                .key(TAG).animator(Animator({ bodyKey() }, animations))
                .key("flame").animator(Animator({ flameKey() }, animations))
                .key("warning").animator(Animator({ warningKey() }, animations))
                .build()
        )
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        super.onSpawn(spawnProps)
        spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(home)
        body.setCenter(home)
        body.physics.velocity.setZero()
        body.physics.gravity.setZero()
        state = MetaDridlerState.WAIT
        waitTimer.resetDuration(if (services.gameState.hardMode) HARD_WAIT_DUR else WAIT_DUR)
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) = false

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                MetaDridlerState.WAIT -> {
                    waitTimer.update(delta)
                    if (waitTimer.isFinished()) {
                        state = MetaDridlerState.DROP
                        body.physics.velocity.set(0f, DROP_START_SPEED * ConstVals.PPM)
                        body.physics.gravity.set(0f, GRAVITY * ConstVals.PPM)
                        playSound(SoundAsset.WHOOSH_SOUND)
                    }
                }

                MetaDridlerState.DROP -> {
                    val maxDrop = home.y - MAX_DROP * ConstVals.PPM
                    if (body.isSensing(BodySense.FEET_ON_GROUND) || origin.y <= maxDrop) {
                        if (origin.y < maxDrop) body.setCenter(home.x, maxDrop)
                        body.physics.velocity.setZero()
                        body.physics.gravity.setZero()
                        state = MetaDridlerState.DRILL
                        drillTimer.reset()
                        playSound(SoundAsset.DIG_SOUND)
                    }
                }

                MetaDridlerState.DRILL -> {
                    drillTimer.update(delta)
                    if (drillTimer.isFinished()) {
                        state = MetaDridlerState.RISE
                        riseSoundTimer.setToEnd()
                    }
                }

                MetaDridlerState.RISE -> {
                    riseSoundTimer.update(delta)
                    if (riseSoundTimer.isFinished()) {
                        playSound(SoundAsset.JET_SOUND)
                        riseSoundTimer.reset()
                    }
                    body.physics.velocity.set(0f, RISE_SPEED * ConstVals.PPM)
                    if (origin.y >= home.y) {
                        body.setCenter(home)
                        body.physics.velocity.setZero()
                        state = MetaDridlerState.WAIT
                        waitTimer.reset()
                    }
                }
            }
        }
    }

    private fun playSound(sound: SoundAsset) {
        if (overlapsGameCamera()) requestToPlaySound(sound, false)
    }

    private fun dropWarningDur() = if (services.gameState.hardMode) HARD_DROP_WARNING_DUR else DROP_WARNING_DUR

    private fun bodyKey() = when (state) {
        MetaDridlerState.WAIT -> "wait"
        MetaDridlerState.DROP, MetaDridlerState.DRILL -> "drill"
        MetaDridlerState.RISE -> "rise"
    }

    private fun flameKey() = when (state) {
        MetaDridlerState.WAIT, MetaDridlerState.RISE -> "flame"
        MetaDridlerState.DROP -> "flame_drop"
        MetaDridlerState.DRILL -> null
    }

    private fun warningKey() = when {
        state == MetaDridlerState.WAIT && waitTimer.duration - waitTimer.time <= dropWarningDur() -> "warn_drop"
        state == MetaDridlerState.DROP -> "warn_drop"
        state == MetaDridlerState.DRILL && drillTimer.duration - drillTimer.time <= RISE_WARNING_DUR -> "warn_rise"
        state == MetaDridlerState.RISE -> "warn_rise"
        else -> null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(CONTACT_WIDTH * ConstVals.PPM, CONTACT_HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -TIP_BELOW_ORIGIN * ConstVals.PPM
        body.addFixture(feetFixture)

        addComponent(
            DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { feetFixture }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1)))
        .preProcess { _, sprite -> X6Frames.place(sprite, FRAMES[bodyKey()], origin.x, origin.y, false) }
        .sprite("flame", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0)))
        .preProcess { _, sprite ->
            val key = flameKey()
            sprite.hidden = key == null
            if (key != null) X6Frames.place(sprite, FRAMES[key], origin.x, origin.y, false)
        }
        .sprite("warning", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2)))
        .preProcess { _, sprite ->
            val key = warningKey()
            sprite.hidden = key == null
            if (key != null) X6Frames.place(sprite, FRAMES[key], origin.x, origin.y, false)
        }
        .build()
}
