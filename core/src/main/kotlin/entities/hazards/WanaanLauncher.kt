package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
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
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getObjectProps

import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class WanaanLauncher(game: MegamanMaverickGame) : AbstractHealthEntity(game), IBodyEntity, IAudioEntity,
    ICullableEntity, IDrawableShapesEntity, IDirectional {

    companion object {
        const val TAG = "WanaanLauncher"
        private const val NEW_WANAAN_DELAY = 0.25f
        private const val LAUNCH_DELAY = 1f
        private const val IMPULSE = 16.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        // TODO
    )
    override lateinit var direction: Direction

    private val newWanaanDelay = Timer(NEW_WANAAN_DELAY)
    private val launchDelay = Timer(LAUNCH_DELAY)
    private val sensors = Array<GameRectangle>()
    private var wanaan: Wanaan? = null

    private val objs = Array<RectangleMapObject>()

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

        val children = getObjectProps(spawnProps, objs)
        children.forEach { sensors.add(it.rectangle.toGameRectangle(false)) }

        this.direction = if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            var direction = spawnProps.get(ConstKeys.DIRECTION)!!
            if (direction is String) direction = Direction.valueOf(direction.uppercase())
            direction as Direction
        } else Direction.UP

        launchDelay.reset()
    }

    override fun onHealthDepleted() {
        wanaan?.destroy()
        wanaan = null

        sensors.clear()

        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

        requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        wanaan?.destroy()
        wanaan = null

        sensors.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
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
            if (wanaan == null &&
                !this.hasDepletedHealth() &&
                newWanaanDelay.isFinished() &&
                sensors.any { it.overlaps(megaman().body.getBounds()) }
            ) {
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
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.SHIELD))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val bust = this.hasDepletedHealth()
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
        GameLogger.debug(TAG, "launchWanaan()")
        wanaan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.WANAAN) as Wanaan
        val spawn = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.5f * ConstVals.PPM)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER).add(0f, 0.5f * ConstVals.PPM)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT).add(0.5f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT).sub(0.5f * ConstVals.PPM, 0f)
        }
        wanaan!!.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DIRECTION pairTo direction,
            )
        )
    }

    private fun launchWanaan() {
        if (wanaan == null) throw IllegalStateException("Wanaan cannot be null when launching")
        val impulse = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> impulse.set(0f, IMPULSE)
            Direction.DOWN -> impulse.set(0f, -IMPULSE)
            Direction.LEFT -> impulse.set(-IMPULSE, 0f)
            Direction.RIGHT -> impulse.set(IMPULSE, 0f)
        }.scl(ConstVals.PPM.toFloat())
        wanaan!!.body.physics.velocity.set(impulse)
        wanaan!!.body.physics.gravityOn = true

        requestToPlaySound(SoundAsset.CHOMP_SOUND, false)
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
