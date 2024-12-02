package com.megaman.maverick.game.entities.hazards

import com.mega.game.engine.common.UtilMethods.getRandom
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic

import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class AcidGoopSupplier(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "AcidGoopSupplier"
        private const val MIN_SPAWN_DELAY = 1f
        private const val MAX_SPAWN_DELAY = 2.5f
        private const val DROP_DELAY = 0.5f
    }

    private val spawnDelayTimer = Timer(MIN_SPAWN_DELAY)
    private val dropDelayTimer = Timer(DROP_DELAY)
    private var acidGoop: AcidGoop? = null

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        val spawnDelayDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnDelayTimer.resetDuration(spawnDelayDuration)
        dropDelayTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        acidGoop?.destroy()
        acidGoop = null
    }

    private fun createAcidGoop() {
        acidGoop = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ACID_GOOP) as AcidGoop
        acidGoop!!.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun dropAcidGoop() = acidGoop!!.setToFall()

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (acidGoop == null) {
            spawnDelayTimer.update(delta)
            if (spawnDelayTimer.isFinished()) {
                createAcidGoop()
                val spawnDelayDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
                spawnDelayTimer.resetDuration(spawnDelayDuration)
            }
        } else {
            dropDelayTimer.update(delta)
            if (dropDelayTimer.isFinished()) {
                dropAcidGoop()
                acidGoop = null
                dropDelayTimer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(this)
        )
    )
}
