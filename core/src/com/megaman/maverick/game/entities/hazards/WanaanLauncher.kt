package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.utils.getObjectProps

import com.megaman.maverick.game.world.BodyComponentCreator

class WanaanLauncher(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IAudioEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "WanaanLauncher"
        private const val TIMER_DURATION = 1f
        private const val WANAAN_IMPULSE = 15f
    }

    private val timer = Timer(TIMER_DURATION)
    private val sensors = Array<GameRectangle>()
    private lateinit var direction: Direction
    private var wanaan: Wanaan? = null

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(AudioComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        val children = getObjectProps(spawnProps)
        children.forEach { sensors.add(it.rectangle.toGameRectangle()) }
        if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            var direction = spawnProps.get(ConstKeys.DIRECTION)!!
            if (direction is String) direction = Direction.valueOf(direction.uppercase())
            this.direction = direction as Direction
        } else this.direction = Direction.UP
        timer.setToEnd()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        wanaan?.kill()
        wanaan = null
        sensors.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val megaman = game.megaman
        if (wanaan != null && wanaan!!.comingDown && body.contains(wanaan!!.cullPoint)) {
            wanaan!!.kill()
            wanaan = null
        }
        if (wanaan?.dead == true) wanaan = null
        if (wanaan == null && sensors.any { it.overlaps(getMegaman().body as Rectangle) }) {
            timer.update(delta)
            if (timer.isFinished()) {
                launchWanaan()
                timer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun launchWanaan() {
        GameLogger.debug(TAG, "Launching Wanaan")
        wanaan = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.WANAAN) as Wanaan?
        game.engine.spawn(
            wanaan!!, props(
                ConstKeys.POSITION to body.getTopCenterPoint(),
                ConstKeys.DIRECTION to direction,
                ConstKeys.IMPULSE to WANAAN_IMPULSE * ConstVals.PPM
            )
        )
        requestToPlaySound(SoundAsset.CHOMP_SOUND, false)
    }

}