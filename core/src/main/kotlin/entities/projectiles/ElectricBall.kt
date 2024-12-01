package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class ElectricBall(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "ElectricBall"
        private var smallRegion: TextureRegion? = null
        private var largeRegion: TextureRegion? = null
    }

    val trajectory = Vector2()

    private var large = false

    override fun init() {
        if (smallRegion == null) smallRegion = game.assMan.getTextureRegion(
            TextureAsset.PROJECTILES_1.source, "Electric/SmallElectric"
        )
        if (largeRegion == null) largeRegion =
            game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Electric/BigElectric")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        if (spawnProps.containsKey(ConstKeys.TRAJECTORY)) trajectory.set(
            spawnProps.get(
                ConstKeys.TRAJECTORY,
                Vector2::class
            )!!
        )
        else {
            trajectory.x = spawnProps.getOrDefault(ConstKeys.X, 0f, Float::class)
            trajectory.y = spawnProps.getOrDefault(ConstKeys.Y, 0f, Float::class)
        }
        large = spawnProps.getOrDefault(ConstKeys.LARGE, false, Boolean::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        // TODO: create zap explosion
        // requestToPlaySound(SoundAsset.MM3_ELECTRIC_PULSE_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val bounds = GameRectangle()

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, bounds)
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, bounds)
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = if (large) ConstVals.PPM.toFloat() else ConstVals.PPM / 4f
            body.setSize(size)
            bounds.setSize(size)
            body.physics.velocity.set(trajectory)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (large) "large" else "small" }
        val animations = objectMapOf<String, IAnimation>(
            "large" pairTo Animation(largeRegion!!, 1, 2, 0.15f, true),
            "small" pairTo Animation(smallRegion!!, 1, 2, 0.15f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
