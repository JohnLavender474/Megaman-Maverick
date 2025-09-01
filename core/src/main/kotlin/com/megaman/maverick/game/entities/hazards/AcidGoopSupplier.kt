package com.megaman.maverick.game.entities.hazards

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class AcidGoopSupplier(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "AcidGoopSupplier"
        private const val MIN_SPAWN_DELAY = 1f
        private const val MAX_SPAWN_DELAY = 2.5f
        private const val MIN_SPAWN_DELAY_HARD = 0.75f
        private const val MAX_SPAWN_DELAY_HARD = 1.25f
        private const val DROP_DELAY = 0.5f
    }

    private val spawnDelayTimer = Timer()
    private val dropDelayTimer = Timer(DROP_DELAY)
    private var acidGoop: AcidGoop? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        resetSpawnDelay()

        dropDelayTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        acidGoop?.destroy()
        acidGoop = null
    }

    private fun resetSpawnDelay() {
        val minDelay: Float
        val maxDelay: Float
        if (game.state.getDifficultyMode() == DifficultyMode.HARD) {
            minDelay = MIN_SPAWN_DELAY_HARD
            maxDelay = MAX_SPAWN_DELAY_HARD
        } else {
            minDelay = MIN_SPAWN_DELAY
            maxDelay = MAX_SPAWN_DELAY
        }
        val delay = UtilMethods.getRandom(minDelay, maxDelay)
        spawnDelayTimer.resetDuration(delay)
    }

    private fun createAcidGoop() {
        val acidGoop = MegaEntityFactory.fetch(AcidGoop::class)!!
        acidGoop.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
        this.acidGoop = acidGoop
    }

    private fun dropAcidGoop() = acidGoop!!.setToFall()

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (acidGoop) {
            null -> {
                spawnDelayTimer.update(delta)
                if (spawnDelayTimer.isFinished()) {
                    createAcidGoop()
                    resetSpawnDelay()
                }
            }
            else -> {
                dropDelayTimer.update(delta)
                if (dropDelayTimer.isFinished()) {
                    dropAcidGoop()
                    acidGoop = null
                    dropDelayTimer.reset()
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(this))
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
