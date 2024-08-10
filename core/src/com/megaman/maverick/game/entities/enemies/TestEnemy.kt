package com.megaman.maverick.game.entities.enemies

import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.motion.ArcMotion
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import kotlin.reflect.KClass

class TestEnemy(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "TestEnemy"
        const val DEBUG_DELAY = 0.25f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    lateinit var arcMotion: ArcMotion
    val debugTimer = Timer(DEBUG_DELAY)

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        arcMotion = ArcMotion(
            startPosition = spawn,
            targetPosition = getMegaman().body.getCenter(),
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
        return SpritesComponent(TAG to sprite)
    }
}