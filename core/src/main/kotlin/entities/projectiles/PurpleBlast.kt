package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class PurpleBlast(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PurpleBlast"
        private const val CHARGE_DELAY = 0.175f
        private var chargeRegion: TextureRegion? = null
        private var blastRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private val chargeDelayTimer = Timer(CHARGE_DELAY)

    override fun init() {
        if (chargeRegion == null || blastRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            chargeRegion = atlas.findRegion("PurpleBlast/Charge")
            blastRegion = atlas.findRegion("PurpleBlast/Blast")
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.velocity.setZero()
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity = trajectory
        chargeDelayTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        chargeDelayTimer.update(delta)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 15))
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            val angle = body.physics.velocity.angleDeg()
            _sprite.setOriginCenter()
            _sprite.setRotation(angle)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!chargeDelayTimer.isFinished()) "charge" else "blast" }
        val animations = objectMapOf<String, IAnimation>(
            "charge" pairTo Animation(chargeRegion!!, 1, 7, 0.025f, false),
            "blast" pairTo Animation(blastRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
