package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools

class SmallGrassPatch(game: MegamanMaverickGame) : MegaGameEntity(game) {

    companion object {
        const val TAG = "SmallGrassPatch"

        private fun spawnGrass(bounds: GameRectangle, out: Array<SmallGrass>) {
            val startX = bounds.getX() + ConstVals.PPM / 2f
            val startY = bounds.getY()
            val columns = bounds.getWidth().div(ConstVals.PPM).toInt()

            for (column in 0 until columns) {
                val x = startX + column * ConstVals.PPM
                val y = startY

                val position = GameObjectPools.fetch(Vector2::class).set(x, y)

                val grass = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SMALL_GRASS)!! as SmallGrass
                grass.spawn(props(ConstKeys.POSITION pairTo position))

                out.add(grass)
            }
        }
    }

    private val bounds = GameRectangle()
    private val grass = Array<SmallGrass>()

    override fun init() {
        super.init()
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        this.bounds.set(bounds)

        spawnGrass(bounds, grass)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        grass.forEach { it.destroy() }
        grass.clear()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        )
    )

    override fun getType() = EntityType.DECORATION
}
