package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.world.body.*

class Force(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "Force"
        const val ACTION_X = "action_x"
        const val ACTION_Y = "action_y"
        const val FORCE_X = "force_x"
        const val FORCE_Y = "force_y"
    }

    private lateinit var filter: (IFixture, Float) -> Boolean
    private lateinit var actionX: VelocityAlterationType
    private lateinit var actionY: VelocityAlterationType
    private var forceX = 0f
    private var forceY = 0f

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag(): String = TAG

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val filterByTagSet = ObjectSet<String>()
        spawnProps.get("${ConstKeys.TAG}_${ConstKeys.FILTER}", String::class)?.split(",")!!.forEach {
            GameLogger.debug(TAG, "Adding tag to force filter: ${it.uppercase()}")
            filterByTagSet.add(it.uppercase())
        }
        val filterByNotBodySenseSet = ObjectSet<BodySense>()
        spawnProps.get("${ConstKeys.NOT}_${ConstKeys.BODY}_${ConstKeys.SENSE}_${ConstKeys.FILTER}", String::class)
            ?.split(",")!!.forEach {
                val bodySense = BodySense.valueOf(it.uppercase())
                GameLogger.debug(TAG, "Adding body sense to force filter: $bodySense")
                filterByNotBodySenseSet.add(bodySense)
            }
        filter = { fixture, _ ->
            val tagFiltered = filterByTagSet.contains(fixture.getEntity().getTag().uppercase())
            val bodySenseFiltered = !fixture.getBody().isSensingAny(filterByNotBodySenseSet)
            if (tagFiltered && bodySenseFiltered) GameLogger.debug(
                TAG,
                "Force applied to ${fixture.getEntity().getTag()}"
            )
            tagFiltered && bodySenseFiltered
        }

        if (spawnProps.containsKey(ACTION_X)) {
            var actionX = spawnProps.get(ACTION_X)!!
            if (actionX is String) actionX = VelocityAlterationType.valueOf(actionX.uppercase())
            this.actionX = actionX as VelocityAlterationType
        } else this.actionX = VelocityAlterationType.ADD

        if (spawnProps.containsKey(ACTION_Y)) {
            var actionY = spawnProps.get(ACTION_Y)!!
            if (actionY is String) actionY = VelocityAlterationType.valueOf(actionY.uppercase())
            this.actionY = actionY as VelocityAlterationType
        } else this.actionY = VelocityAlterationType.ADD

        forceX = spawnProps.getOrDefault(FORCE_X, 0f, Float::class) * ConstVals.PPM
        forceY = spawnProps.getOrDefault(FORCE_Y, 0f, Float::class) * ConstVals.PPM

        val cull = spawnProps.getOrDefault(ConstKeys.CULL, true, Boolean::class)
        if (cull) addComponent(createCullablesComponent()) else removeComponent(CullablesComponent::class)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val forceFixture = Fixture(body, FixtureType.FORCE, GameRectangle())
        forceFixture.setVelocityAlteration { fixture, delta ->
            if (filter(fixture, delta)) VelocityAlteration(
                forceX, forceY, actionX, actionY
            ) else VelocityAlteration.addNone()
        }
        body.addFixture(forceFixture)
        body.preProcess.put(ConstKeys.DEFAULT) { (forceFixture.rawShape as GameRectangle).set(body) }
        return BodyComponentCreator.create(this, body)
    }

    private fun createCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullOnOutOfBounds))
    }
}