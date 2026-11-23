package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

class TimberWalkerLog(services: MegaServices) : AbstractProjectile(services) {

    companion object {
        const val TAG = "TimberWalkerLog"
        private const val BODY_SIZE = 1.25f
        private const val SPRITE_SIZE = 2f
        private const val SPIN_FRAME_DUR = 0.08f
        private const val BREAK_DUR = 0.3f
        private val spinRegions = Array<TextureRegion>()
        private var breakRegion: TextureRegion? = null
    }

    private lateinit var spin: Animation
    private val breakTimer = Timer(BREAK_DUR)
    private var broken = false

    override fun init(vararg params: Any) {
        if (spinRegions.isEmpty) {
            for (i in 1..4) spinRegions.add(
                services.assets.getTextureRegion(TextureAsset.TIMBER_WALKER.source, "$TAG/spin_$i")
            )
            breakRegion = services.assets.getTextureRegion(TextureAsset.TIMBER_WALKER.source, "$TAG/break")
        }
        super.init()
        spin = Animation(spinRegions, SPIN_FRAME_DUR, loop = true)
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.gravity.set(0f, spawnProps.get(ConstKeys.GRAVITY, Float::class)!!)
        body.physics.gravityOn = true
        broken = false
        breakTimer.reset()
        spin.reset()
    }

    override fun canDamage(damageable: IDamageableEntity) = !broken && super.canDamage(damageable)

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = breakApart()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shieldFixture.getEntity() != owner) breakApart()
    }

    // TODO: the log breaks into one harmless frame, with no log pieces that hurt (branch 078g TODO.md)
    private fun breakApart() {
        if (broken) return
        broken = true
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (broken) {
            breakTimer.update(delta)
            if (breakTimer.isFinished()) destroy()
        } else spin.update(delta)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(BODY_SIZE * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(BODY_SIZE * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setRegion(if (broken) breakRegion!! else spin.getCurrentRegion())
            sprite.setCenter(body.getCenter())
        }
        return component
    }
}
