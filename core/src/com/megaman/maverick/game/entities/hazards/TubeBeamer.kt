package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.engine.audio.AudioComponent
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator

class TubeBeamer(game: MegamanMaverickGame) : GameEntity(game), IAudioEntity, IBodyEntity, ICullableEntity,
    IDirectionRotatable {

    companion object {
        const val TAG = "TubeBeamer"
        private const val VELOCITY = 10f
        private const val SPAWN_DELAY = 1.25f
        private const val DEFAULT_INITIAL_DELAY = 0f
    }

    override lateinit var directionRotation: Direction

    private val spawnTimer = Timer(SPAWN_DELAY)
    private lateinit var initialDelayTimer: Timer

    override fun init() {
        addComponent(AudioComponent(this))
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        directionRotation = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())
        spawnTimer.reset()
        val initialDelay = spawnProps.getOrDefault(ConstKeys.DELAY, DEFAULT_INITIAL_DELAY, Float::class)
        initialDelayTimer = Timer(initialDelay)
    }

    private fun beamTube() {
        val tubeBeam = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.TUBE_BEAM)!!
        val trajectory = (when (directionRotation) {
            Direction.RIGHT -> Vector2(VELOCITY, 0f)
            Direction.LEFT -> Vector2(-VELOCITY, 0f)
            Direction.UP -> Vector2(0f, VELOCITY)
            Direction.DOWN -> Vector2(0f, -VELOCITY)
        }).scl(ConstVals.PPM.toFloat())
        game.engine.spawn(
            tubeBeam, props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.DIRECTION to directionRotation,
                ConstKeys.TRAJECTORY to trajectory
            )
        )
        requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        initialDelayTimer.update(delta)
        if (!initialDelayTimer.isFinished()) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            beamTube()
            spawnTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(this, objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullable))
    }
}