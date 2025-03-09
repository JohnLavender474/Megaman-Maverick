package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

// implements `IBossListener` to ensure is destroyed after 2nd Reactor Man fight
class WanaanLauncher(game: MegamanMaverickGame) : AbstractHealthEntity(game), IBodyEntity, IAudioEntity,
    ICullableEntity, IDrawableShapesEntity, IBossListener, IDirectional {

    companion object {
        const val TAG = "WanaanLauncher"
        private const val NEW_WANAAN_DELAY = 0.25f
        private const val LAUNCH_DELAY = 1f
        private const val IMPULSE = 16.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var direction: Direction

    // wanaan launcher is only damaged by spikes
    override val damageNegotiator = null

    private val newWanaanDelay = Timer(NEW_WANAAN_DELAY)
    private val launchDelay = Timer(LAUNCH_DELAY)
    private val sensor = GameRectangle()
    private var wanaan: Wanaan? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            gdxArrayOf("launcher", "bust").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        sensor.set(spawnProps.get(ConstKeys.SENSOR, RectangleMapObject::class)!!.rectangle)

        this.direction = when {
            spawnProps.containsKey(ConstKeys.DIRECTION) -> {
                var direction = spawnProps.get(ConstKeys.DIRECTION)!!
                if (direction is String) direction = Direction.valueOf(direction.uppercase())
                direction as Direction
            }

            else -> Direction.UP
        }

        launchDelay.reset()
        newWanaanDelay.reset()
    }

    override fun onHealthDepleted() {
        wanaan?.destroy()
        wanaan = null

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

        requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun onBossDefeated(boss: AbstractBoss) {
        depleteHealth()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        wanaan?.destroy()
        wanaan = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (isHealthDepleted()) return@add

            if (wanaan != null && wanaan!!.comingDown && body.getBounds().contains(wanaan!!.cullPoint)) {
                wanaan!!.destroy()
                wanaan = null
                newWanaanDelay.reset()
            }

            if (wanaan?.dead == true) {
                wanaan = null
                newWanaanDelay.reset()
            }

            newWanaanDelay.update(delta)

            if (wanaan == null && newWanaanDelay.isFinished() && sensor.overlaps(megaman.body.getBounds())) {
                spawnWanaan()
                launchDelay.reset()
            } else if (wanaan != null) {
                launchDelay.update(delta)
                if (launchDelay.isJustFinished()) launchWanaan()
            }
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val bust = this.isHealthDepleted()
            val region = if (bust) regions["bust"] else regions["launcher"]
            sprite.setRegion(region)
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    private fun spawnWanaan() {
        if (wanaan != null) throw IllegalStateException("Wanaan ref must null when spawning a new one")
        GameLogger.debug(TAG, "spawnWanaan()")

        val spawn = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.5f * ConstVals.PPM)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER).add(0f, 0.5f * ConstVals.PPM)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT).add(0.5f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT).sub(0.5f * ConstVals.PPM, 0f)
        }

        wanaan = MegaEntityFactory.fetch(Wanaan::class)!!
        wanaan!!.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DIRECTION pairTo direction,
            )
        )
    }

    private fun launchWanaan() {
        if (wanaan == null) throw IllegalStateException("Wanaan ref cannot be null when launching the Wanaan")
        GameLogger.debug(TAG, "launchWanaan()")

        val impulse = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> impulse.set(0f, IMPULSE)
            Direction.DOWN -> impulse.set(0f, -IMPULSE)
            Direction.LEFT -> impulse.set(-IMPULSE, 0f)
            Direction.RIGHT -> impulse.set(IMPULSE, 0f)
        }.scl(ConstVals.PPM.toFloat())

        wanaan!!.body.physics.let {
            it.gravityOn = true
            it.velocity.set(impulse)
        }

        requestToPlaySound(SoundAsset.CHOMP_SOUND, false)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
