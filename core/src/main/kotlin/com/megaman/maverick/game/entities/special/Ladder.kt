package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.LadderTop
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class Ladder(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "Ladder"
        private const val LADDER_TOP = "ladder_top"
    }

    private val ladderRectangle = GameRectangle()
    private var ladderTop: LadderTop? = null

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        ladderRectangle.set(bounds)

        if (spawnProps.containsKey(LADDER_TOP)) {
            val ladderTopBounds = spawnProps.get(LADDER_TOP, RectangleMapObject::class)!!.rectangle.toGameRectangle()
            val ladderTop = MegaEntityFactory.fetch(LadderTop::class)!!
            ladderTop.spawn(props(ConstKeys.BOUNDS pairTo ladderTopBounds, ConstKeys.CULL_OUT_OF_BOUNDS pairTo false))
            this.ladderTop = ladderTop
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ladderTop?.destroy()
        ladderTop = null
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val ladderFixture = Fixture(body, FixtureType.LADDER, ladderRectangle)
        body.addFixture(ladderFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ ladderRectangle }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
