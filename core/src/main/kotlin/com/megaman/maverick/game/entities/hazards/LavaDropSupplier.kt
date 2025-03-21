package com.megaman.maverick.game.entities.hazards

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

class LavaDropSupplier(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "LavaDropSupplier"
        private const val DROP_DELAY = 1f
    }

    private val dropDelayTimer = Timer(DROP_DELAY)
    private var lavaDrop: LavaDrop? = null

    override fun getType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        dropDelayTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        lavaDrop = null
    }

    private fun createLavaDrop() {
        lavaDrop = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.LAVA_DROP) as LavaDrop?
        lavaDrop!!.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
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
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(this)
        )
    )
}
