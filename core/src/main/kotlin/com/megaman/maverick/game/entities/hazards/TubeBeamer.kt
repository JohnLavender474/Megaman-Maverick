package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.TubeBeam
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class TubeBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IAudioEntity, IBodyEntity, ICullableEntity,
    IDirectional {

    companion object {
        const val TAG = "TubeBeamer"
        private const val VELOCITY = 10f
        private const val SPAWN_DELAY = 1.25f
    }

    override lateinit var direction: Direction

    private val spawnTimer = Timer(SPAWN_DELAY)
    private lateinit var spawnRoom: String

    override fun init() {
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        direction = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        spawnTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun beamTube() {
        val trajectory = (when (direction) {
            Direction.RIGHT -> Vector2(VELOCITY, 0f)
            Direction.LEFT -> Vector2(-VELOCITY, 0f)
            Direction.UP -> Vector2(0f, VELOCITY)
            Direction.DOWN -> Vector2(0f, -VELOCITY)
        }).scl(ConstVals.PPM.toFloat())

        val beam = MegaEntityFactory.fetch(TubeBeam::class)!!
        beam.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!megaman.ready) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            beamTube()
            spawnTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS), predicate@{ event ->
                    val eventRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val shouldCull = eventRoom != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): predicate: " +
                            "eventRoom=$eventRoom, spawnRoom=$spawnRoom, shouldCull=$shouldCull, event=$event"
                    )
                    return@predicate shouldCull
                }
            )
        )
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
