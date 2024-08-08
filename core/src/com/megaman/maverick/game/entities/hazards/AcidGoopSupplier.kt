package com.megaman.maverick.game.entities.hazards

import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandom
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator

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

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        val spawnDelayDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnDelayTimer.resetDuration(spawnDelayDuration)
        dropDelayTimer.reset()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        acidGoop = null
    }

    private fun createAcidGoop() {
        acidGoop = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ACID_GOOP) as AcidGoop?
        game.engine.spawn(acidGoop!!, props(ConstKeys.POSITION to body.getCenter()))
    }

    private fun dropAcidGoop() = acidGoop!!.setToFall()

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
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
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        this, objectMapOf(
            ConstKeys.CULL_OUT_OF_BOUNDS to getGameCameraCullingLogic(this)
        )
    )
}