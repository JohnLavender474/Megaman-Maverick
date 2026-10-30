package com.megaman.maverick.game.entities.enemies

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.motion.ArcMotion
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class TestEnemy(services: MegaServices) : AbstractEnemy(services) {

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
            targetPosition = targetPlayer.body.getCenter(),
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
