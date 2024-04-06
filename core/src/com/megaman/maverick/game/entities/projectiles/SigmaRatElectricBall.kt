package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getOverlapPushDirection
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SigmaRatElectricBall(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

    companion object {
        const val TAG = "SigmaRatElectricBall"
        private const val HIT_DUR = 0.1f
        private var ballRegion: TextureRegion? = null
        private var hitRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    private val hitTimer = Timer(HIT_DUR)

    private var hit = false
    private var explosionDirection: Direction? = null

    override fun init() {
        if (ballRegion == null || hitRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            ballRegion = atlas.findRegion("SigmaRat/ElectricBall")
            hitRegion = atlas.findRegion("SigmaRat/ElectricBallDissipate")
        }
        super<GameEntity>.init()
        addComponents(defineProjectileComponents())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
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

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (hit) {
            hitTimer.update(delta)
            if (hitTimer.isFinished()) {
                spawnExplosion()
                kill()
            }
        }
    })

    private fun spawnExplosion() {
        val explosion = EntityFactories.fetch(
            EntityType.EXPLOSION, ExplosionsFactory.SIGMA_RAT_ELECTRIC_BALL_EXPLOSION
        )
        game.engine.spawn(
            explosion!! to props(
                ConstKeys.POSITION to body.getBottomCenterPoint(),
                ConstKeys.DIRECTION to (explosionDirection ?: Direction.UP)
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

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

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (hit) "hit" else "ball" }
        val animations = objectMapOf<String, IAnimation>(
            "ball" to Animation(ballRegion!!, 1, 2, 0.1f, true), "hit" to Animation(hitRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}