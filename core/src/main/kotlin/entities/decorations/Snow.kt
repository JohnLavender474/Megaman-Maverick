package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.swapped
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.*

class Snow(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "Snow"
        private const val SWITCH_DELAY = 0.5f
        private var region: TextureRegion? = null
    }

    private lateinit var sine: SineWave

    private val timer = Timer(SWITCH_DELAY)

    private var minAmplitude = 0f
    private var maxAmplitude = 0f

    private var minFrequency = 0f
    private var maxFrequency = 0f
    private var drift = 0f

    private var minY = 0f

    private val out = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        minFrequency = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.FREQUENCY}", Float::class)!!
        maxFrequency = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.FREQUENCY}", Float::class)!!

        minAmplitude = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.AMPLITUDE}", Float::class)!!
        maxAmplitude = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.AMPLITUDE}", Float::class)!!

        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        val amplitude = UtilMethods.getRandom(minAmplitude, maxAmplitude)
        val frequency = UtilMethods.getRandom(minFrequency, maxFrequency)
        sine = SineWave(spawn.cpy().swapped(), speed, amplitude, frequency)

        drift = spawnProps.getOrDefault(ConstKeys.DRIFT, 0f, Float::class)
        minY = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.Y}", Float::class)!!

        timer.reset()

        val size = MegaGameEntities.getEntitiesOfTag(getTag()).size
        GameLogger.debug(
            TAG,
            "onSpawn(): snowAmount=$size spawnProps=$spawnProps, megamanPos=${megaman().body.getCenter()}"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        val size = MegaGameEntities.getEntitiesOfTag(getTag()).size
        GameLogger.debug(
            TAG,
            "onDestroy(): snowAmount=$size"
        )

    }

    private fun adjust() {
        sine.amplitude = UtilMethods.getRandom(minAmplitude, maxAmplitude)
        GameLogger.debug(
            TAG,
            "adjust(): hashcode=${hashCode()}, sine.amplitude=${sine.amplitude}, sine.frequency=${sine.frequency}, " +
                "drift=$drift, position=${body.getCenter()}"
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) {
            adjust()
            timer.reset()
        }

        sine.update(delta)
        val position = sine.getMotionValue(out).swapped()
        body.setPosition(position).translate(drift * delta, 0f)

        if (body.getY() < minY) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM)

        val bodyRect = GameRectangle()
        val bodyFixture = Fixture(body, FixtureType.BODY, bodyRect)
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            GameLogger.debug(TAG, "hitByBlock(): mapObjId=${block.mapObjectId}")
            destroy()
        }
        bodyFixture.setHitByBodyReceiver {
            if ((it as MegaGameEntity).getTag() == getTag()) return@setHitByBodyReceiver
            GameLogger.debug(TAG, "hitByBody(): body=$it")
        }
        bodyFixture.setHitByProjectileReceiver {
            GameLogger.debug(TAG, "hitByProjectile(): projectile=$it")
            destroy()
        }
        bodyFixture.setHitByWaterReceiver {
            GameLogger.debug(TAG, "hitByWater(): water=$it")
            destroy()
        }
        body.addFixture(bodyFixture)

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle(body))
        waterListenerFixture.setHitByWaterReceiver { destroy() }
        waterListenerFixture.putProperty(ConstKeys.SPLASH, false)
        body.addFixture(waterListenerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) { bodyRect.set(body) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!))
        .updatable { _, sprite -> sprite.setBounds(body.getBounds()) }
        .build()

    override fun getEntityType() = EntityType.DECORATION

    override fun getTag() = TAG
}
