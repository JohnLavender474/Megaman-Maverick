package com.megaman.maverick.game.entities.hazards

import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
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

class LavaDropSupplier(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "LavaDropSupplier"
        private const val DROP_DELAY = 1f
    }

    private val dropDelayTimer = Timer(DROP_DELAY)
    private var lavaDrop: LavaDrop? = null

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        dropDelayTimer.reset()
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        lavaDrop = null
    }

    private fun createLavaDrop() {
        lavaDrop = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.LAVA_DROP) as LavaDrop?
        game.engine.spawn(lavaDrop!!, props(ConstKeys.POSITION to body.getCenter()))
    }

    private fun dropLavaDrop() = lavaDrop!!.setToFall()

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (lavaDrop == null) createLavaDrop()
        else dropDelayTimer.update(delta)

        if (dropDelayTimer.isFinished()) {
            dropLavaDrop()
            lavaDrop = null
            dropDelayTimer.reset()
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
            ConstKeys.CULL_OUT_OF_BOUNDS to getGameCameraCullingLogic(this)
        )
    )
}