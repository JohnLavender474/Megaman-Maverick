package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
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
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBody

class SigmaRatElectricBall(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "SigmaRatElectricBall"
        private const val HIT_DUR = 0.1f
        private var ballRegion: TextureRegion? = null
        private var hitRegion: TextureRegion? = null
    }

    override var owner: GameEntity? = null

    private val hitTimer = Timer(HIT_DUR)

    private var hit = false
    private var explosionDirection: Direction? = null

    override fun init() {
        if (ballRegion == null || hitRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            ballRegion = atlas.findRegion("SigmaRat/ElectricBall")
            hitRegion = atlas.findRegion("SigmaRat/ElectricBallDissipate")
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity = trajectory
        hit = false
        hitTimer.reset()
    }

    override fun hitBlock(blockFixture: IFixture) {
        super.hitBlock(blockFixture)
        body.physics.velocity.setZero()
        hit = true
        val blockBounds = blockFixture.getBody()
        explosionDirection = getOverlapPushDirection(body, blockBounds)
        requestToPlaySound(SoundAsset.BASSY_BLAST_SOUND, false)
    }

    internal fun launch(trajectory: Vector2) {
        body.physics.velocity = trajectory
        requestToPlaySound(SoundAsset.BLAST_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (hit) {
            hitTimer.update(delta)
            if (hitTimer.isFinished()) {
                spawnExplosion()
                destroy()
            }
        }
    })

    private fun spawnExplosion() {
        val explosion = EntityFactories.fetch(
            EntityType.EXPLOSION, ExplosionsFactory.SIGMA_RAT_ELECTRIC_BALL_EXPLOSION
        )
        explosion!!.spawn(
            props(
                ConstKeys.POSITION pairTo body.getBottomCenterPoint(),
                ConstKeys.DIRECTION pairTo (explosionDirection ?: Direction.UP)
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        body.color = Color.YELLOW
        debugShapes.add { body }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.BLUE
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (hit) "hit" else "ball" }
        val animations = objectMapOf<String, IAnimation>(
            "ball" pairTo Animation(ballRegion!!, 1, 2, 0.1f, true), "hit" pairTo Animation(hitRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}
