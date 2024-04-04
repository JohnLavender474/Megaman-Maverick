package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import kotlin.reflect.KClass

class TestEnemy(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        private const val WEB_SHOOT_DUR = 3f
        private const val WEB_SPEED = 10f
        private const val ANGLE_X = 25f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    private val webShootTimer = Timer(WEB_SHOOT_DUR)

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        webShootTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            webShootTimer.update(it)
            if (webShootTimer.isFinished()) {
                shootWebs()
                webShootTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM)
        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body })
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        return SpritesComponent(this, TAG to sprite)
    }

    private fun shootWebs() {
        val centerTrajectory = megaman.body.getCenter().sub(body.getCenter()).nor()
        val leftTrajectory = centerTrajectory.cpy().rotateDeg(-ANGLE_X)
        val rightTrajectory = centerTrajectory.cpy().rotateDeg(ANGLE_X)
        shootWeb(centerTrajectory)
        shootWeb(leftTrajectory)
        shootWeb(rightTrajectory)
    }

    private fun shootWeb(trajectory: Vector2) {
        val web = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPIDER_WEB)!!
        val scaledTrajectory = trajectory.scl(WEB_SPEED * ConstVals.PPM)
        val props = props(
            ConstKeys.POSITION to body.getBottomCenterPoint(),
            ConstKeys.TRAJECTORY to scaledTrajectory,
            ConstKeys.OWNER to this
        )
        game.engine.spawn(web, props)
    }
}