package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.utils.getObjectProps
import com.megaman.maverick.game.world.body.BodyComponentCreator

class WanaanLauncher(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IAudioEntity,
    IDrawableShapesEntity, IDirectionRotatable {

    companion object {
        const val TAG = "WanaanLauncher"
        private const val LAUNCH_DELAY = 0.75f
        private const val IMPULSE = 12f
    }

    override lateinit var directionRotation: Direction

    private val launchDelay = Timer(LAUNCH_DELAY)
    private val sensors = Array<GameRectangle>()
    private var wanaan: Wanaan? = null

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val children = getObjectProps(spawnProps)
        children.forEach { sensors.add(it.rectangle.toGameRectangle()) }

        this.directionRotation = if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            var direction = spawnProps.get(ConstKeys.DIRECTION)!!
            if (direction is String) direction = Direction.valueOf(direction.uppercase())
            direction as Direction
        } else Direction.UP

        launchDelay.reset()
    }

    override fun onDestroy() {
        super.onDestroy()

        wanaan?.destroy()
        wanaan = null

        sensors.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (wanaan != null && wanaan!!.comingDown && body.contains(wanaan!!.cullPoint)) {
            wanaan!!.destroy()
            wanaan = null
        }

        if (wanaan?.dead == true) wanaan = null

        if (wanaan == null && sensors.any { it.overlaps(megaman().body as Rectangle) }) {
            spawnWanaan()
            launchDelay.reset()
        } else if (wanaan != null) {
            launchDelay.update(delta)
            if (launchDelay.isJustFinished()) launchWanaan()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun spawnWanaan() {
        GameLogger.debug(TAG, "launchWanaan()")
        wanaan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.WANAAN) as Wanaan
        val spawn = when (directionRotation) {
            Direction.UP -> body.getTopCenterPoint().sub(0f, 0.25f * ConstVals.PPM)
            Direction.DOWN -> body.getBottomCenterPoint().add(0f, 0.25f * ConstVals.PPM)
            Direction.LEFT -> body.getCenterLeftPoint().add(0.25f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getCenterRightPoint().sub(0.25f * ConstVals.PPM, 0f)
        }
        wanaan!!.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DIRECTION pairTo directionRotation,
            )
        )
    }

    private fun launchWanaan() {
        if (wanaan == null) throw IllegalStateException("Wanaan cannot be null when launching")
        val impulse = when (directionRotation) {
            Direction.UP -> Vector2(0f, IMPULSE)
            Direction.DOWN -> Vector2(0f, -IMPULSE)
            Direction.LEFT -> Vector2(-IMPULSE, 0f)
            Direction.RIGHT -> Vector2(IMPULSE, 0f)
        }.scl(ConstVals.PPM.toFloat())
        wanaan!!.body.physics.velocity.set(impulse)
        wanaan!!.body.physics.gravityOn = true

        requestToPlaySound(SoundAsset.CHOMP_SOUND, false)
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
