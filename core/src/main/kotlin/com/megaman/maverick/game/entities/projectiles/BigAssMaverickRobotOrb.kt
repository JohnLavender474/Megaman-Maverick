package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class BigAssMaverickRobotOrb(game: MegamanMaverickGame) : AbstractProjectile(game, size = Size.SMALL), IAnimatedEntity {

    companion object {
        const val TAG = "BigAssMaverickRobotOrb"
        private const val BODY_SIZE = 1.5f
        private var region: TextureRegion? = null
    }

    private val moveDelay = Timer()
    private val trajectory = Vector2()

    override fun init() {
        // if (region == null) region = ...
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)

        val delay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        moveDelay.resetDuration(delay)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        moveDelay.update(delta)
        if (moveDelay.isFinished()) body.physics.velocity.set(trajectory)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    // TODO
    override fun defineSpritesComponent() = SpritesComponentBuilder().build()

    // TODO
    private fun defineAnimationsComponent() = AnimationsComponentBuilder().build()
}
