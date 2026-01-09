package com.megaman.maverick.game.entities.special

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds

class PipePortal(game: MegamanMaverickGame): MegaGameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "PipePortal"
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val drawableShapesComponent = DrawableShapesComponentBuilder()
        drawableShapesComponent.addDebug { body.getBounds() }
        addComponent(drawableShapesComponent.build())

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.SPECIAL
}
