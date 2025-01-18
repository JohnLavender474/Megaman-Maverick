package com.megaman.maverick.game.entities.enemies

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.motion.ArcMotion
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class TestEnemy(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "TestEnemy"
        const val DEBUG_DELAY = 0.25f
    }

    lateinit var arcMotion: ArcMotion
    val debugTimer = Timer(DEBUG_DELAY)

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        arcMotion = ArcMotion(
            startPosition = spawn,
            targetPosition = megaman.body.getCenter(),
            speed = 8f * ConstVals.PPM,
            arcFactor = -0.5f
        )

        debugTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            arcMotion.update(delta)
            val target = arcMotion.getMotionValue()!!

            debugTimer.update(delta)
            if (debugTimer.isFinished()) {
                debugTimer.reset()
                GameLogger.debug(TAG, "delta=$delta, current=${body.getCenter()}, target=$target")
            }

            body.setCenter(target)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM)

        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        return SpritesComponent(TAG pairTo sprite)
    }
}
