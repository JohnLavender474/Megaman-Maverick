package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class WhiteArrowPool(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, IDirectionRotatable {

    companion object {
        const val TAG = "WhiteArrowPool"
        private const val SPAWN_DELAY_DUR = 1f
    }

    override var directionRotation: Direction? = null

    private val spawns = Array<Vector2>()
    private val spawnDelayTimer = Timer(SPAWN_DELAY_DUR)
    private lateinit var bounds: GameRectangle
    private var outline = true
    private var even = false
    private var maxOffset = 0

    override fun init() {
        super.init()
        addComponent(defineDrawableShapesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        bounds.color = Color.WHITE
        outline = spawnProps.getOrDefault(ConstKeys.OUTLINE, true, Boolean::class)

        directionRotation = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())

        val cells = bounds.splitByCellSize(ConstVals.PPM.toFloat())
        when (directionRotation!!) {
            Direction.UP -> {
                for (i in 0 until cells.columns) spawns.add(cells[i, 0]!!.getBottomCenterPoint())
                maxOffset = cells.rows
            }

            Direction.DOWN -> {
                for (i in 0 until cells.columns) spawns.add(cells[i, cells.rows - 1]!!.getTopCenterPoint())
                maxOffset = cells.rows
            }

            Direction.LEFT -> {
                for (i in 0 until cells.rows) spawns.add(cells[cells.columns - 1, i]!!.getCenterRightPoint())
                maxOffset = cells.columns
            }

            Direction.RIGHT -> {
                for (i in 0 until cells.rows) spawns.add(cells[0, i]!!.getCenterLeftPoint())
                maxOffset = cells.columns
            }
        }

        even = false
        spawnDelayTimer.setToEnd()
    }

    override fun onDestroy() {
        super.onDestroy()
        spawns.clear()
    }

    private fun spawnArrows() {
        var i = if (even) 0 else 1
        while (i < spawns.size) {
            val spawn = spawns[i].cpy()
            val arrow = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.WHITE_ARROW)!!
            arrow.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.DIRECTION pairTo directionRotation,
                    ConstKeys.MAX pairTo maxOffset
                )
            )
            i += 2
        }
    }

    private fun defineDrawableShapesComponent() =
        DrawableShapesComponent(prodShapeSuppliers = gdxArrayOf({ if (outline) bounds else null }))

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        spawnDelayTimer.update(delta)
        if (spawnDelayTimer.isFinished()) {
            spawnArrows()
            even = !even
            spawnDelayTimer.reset()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cull = getGameCameraCullingLogic(getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cull))
    }

    override fun getEntityType() = EntityType.DECORATION

    override fun getTag() = TAG
}
