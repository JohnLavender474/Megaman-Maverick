package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.SaucerRay
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class TellySaucer(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IMotionEntity,
    IDirectional {

    companion object {
        const val TAG = "TellySaucer"

        private const val SPEED = 4f
        private const val FREQUENCY = 3f
        private const val AMPLITUDE = 0.025f

        private const val RAY_DUR = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private lateinit var sine: SineWave

    private var ray: SaucerRay? = null

    private val rayTimer = Timer(RAY_DUR)
    private val raying: Boolean
        get() = !rayTimer.isFinished()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("spin", "flash").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(MotionComponent())
        addComponent(defineAnimationsComponent())
        damageOverrides.put(Asteroid::class, dmgNeg(ConstVals.MAX_HEALTH))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, megaman.body.getX() <= body.getX(), Boolean::class)
        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)

        val sinePos = body.getCenter(false)
        val speed = ConstVals.PPM * if (left) -SPEED else SPEED
        val amplitude = ConstVals.PPM * if (flip) -AMPLITUDE else AMPLITUDE
        sine = SineWave(sinePos, speed, amplitude, FREQUENCY)

        rayTimer.setToEnd()

        requestToPlaySound(SoundAsset.ALARM_SOUND, false)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ray?.destroy()
        ray = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            rayTimer.update(delta)
            if (!rayTimer.isFinished()) {
                body.physics.velocity.setZero()
                return@update
            }
            if (rayTimer.isJustFinished()) {
                ray?.destroy()
                ray = null
            }

            sine.update(delta)
            sine.getMotionValue()?.let { body.setCenter(it) }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val rayScanner =
            Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(ConstVals.PPM.toFloat(), 4f * ConstVals.PPM))
        rayScanner.offsetFromBodyAttachment.y = -body.getHeight()
        rayScanner.setFilter { fixture -> fixture.getEntity() == megaman && fixture.getType() == FixtureType.DAMAGEABLE }
        rayScanner.setConsumer { processState, _ -> if (processState == ProcessState.BEGIN && !raying) startRay() }
        body.addFixture(rayScanner)
        rayScanner.drawingColor = Color.GREEN
        debugShapes.add { rayScanner }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = megaman.direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (raying) "flash" else "spin" }
                .addAnimations(
                    "flash" pairTo Animation(regions["flash"], 2, 1, 0.1f, true),
                    "spin" pairTo Animation(regions["spin"], 3, 2, 0.1f, true)
                )
                .build()
        )
        .build()

    private fun startRay() {
        GameLogger.debug(TAG, "startRay()damageable")

        val spawn = body.getPositionPoint(DirectionPositionMapper.getInvertedPosition(direction))

        val ray = MegaEntityFactory.fetch(SaucerRay::class)!!
        ray.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.DIRECTION pairTo direction))
        this.ray = ray

        rayTimer.reset()
    }
}
