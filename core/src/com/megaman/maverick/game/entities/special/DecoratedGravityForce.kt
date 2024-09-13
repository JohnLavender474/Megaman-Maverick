package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.ForceDecoration
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.SpecialsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

// convenience implementation that joins `GravityChange`, `Force`, and `ForceDecoration` into one entity
class DecoratedGravityForce(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "GravityForce"
        private const val DEFAULT_NOT_BODY_SENSE_FILTER_SET = "feet_on_ground"
        private const val DEFAULT_TAG_FILTER_SET = "megaman,sniperjoe,met"
    }

    private val decorations = Array<ForceDecoration>()
    private var gravityChange: GravityChange? = null
    private var force: Force? = null
    private lateinit var bounds: GameRectangle

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag() = TAG

    override fun init() {
        super.init()
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        if (!spawnProps.containsKey("${ConstKeys.TAG}_${ConstKeys.FILTER}"))
            spawnProps.put("${ConstKeys.TAG}_${ConstKeys.FILTER}", DEFAULT_TAG_FILTER_SET)
        if (!spawnProps.containsKey("${ConstKeys.NOT}_${ConstKeys.BODY}_${ConstKeys.SENSE}_${ConstKeys.FILTER}"))
            spawnProps.put(
                "${ConstKeys.NOT}_${ConstKeys.BODY}_${ConstKeys.SENSE}_${ConstKeys.FILTER}",
                DEFAULT_NOT_BODY_SENSE_FILTER_SET
            )

        val spawnPropsCopy = spawnProps.copy()
        spawnPropsCopy.put(ConstKeys.CULL, false)

        gravityChange = EntityFactories.fetch(EntityType.SPECIAL, SpecialsFactory.GRAVITY_CHANGE)!! as GravityChange
        gravityChange!!.spawn(spawnPropsCopy)
        force = EntityFactories.fetch(EntityType.SPECIAL, SpecialsFactory.FORCE)!! as Force
        force!!.spawn(spawnPropsCopy)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        val direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val splitSize = if (direction.isVertical()) Vector2(
            2f * ConstVals.PPM,
            ConstVals.PPM.toFloat()
        ) else Vector2(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
        val matrix = bounds.splitByCellSize(splitSize.x, splitSize.y)
        val decorationBounds = Array<GameRectangle>()
        when (direction) {
            Direction.UP -> for (i in 0 until matrix.columns) decorationBounds.add(matrix[i, 0])
            Direction.DOWN -> for (i in 0 until matrix.columns) decorationBounds.add(matrix[i, matrix.rows - 1])
            Direction.LEFT -> for (i in 0 until matrix.rows) decorationBounds.add(matrix[matrix.columns - 1, i])
            Direction.RIGHT -> for (i in 0 until matrix.rows) decorationBounds.add(matrix[0, i])
        }
        decorationBounds.forEach {
            val decoration =
                EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.FORCE_DECORATION)!! as ForceDecoration
            decoration.spawn(
                props(
                    ConstKeys.BOUNDS to it, ConstKeys.ROTATION to direction.rotation, ConstKeys.CULL to false
                )
            )
            decorations.add(decoration)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gravityChange?.destroy()
        gravityChange = null
        force?.destroy()
        force = null
        decorations.forEach { it.destroy() }
        decorations.clear()
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullOnOutOfBounds))
    }
}