package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class SigmaRatElectricBallExplosion(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IDamager, IHazard, IDirectionRotatable {

    companion object {
        const val TAG = "SigmaRatElectricBallExplosion"
        private const val SHOCK_DUR = 0.5f
        private const val DISSIPATE_DUR = 0.3f
        private var explosionRegion: TextureRegion? = null
        private var dissipateRegion: TextureRegion? = null
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val shockTimer = Timer(SHOCK_DUR)
    private val dissipateTimer = Timer(DISSIPATE_DUR)

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (explosionRegion == null || dissipateRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            explosionRegion = atlas.findRegion("SigmaRat/ElectricPulse")
            dissipateRegion = atlas.findRegion("SigmaRat/ElectricDissipate")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        shockTimer.reset()
        dissipateTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        shockTimer.update(delta)
        if (shockTimer.isFinished()) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM, 1.05f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                0.5f * ConstVals.PPM,
                ConstVals.PPM.toFloat()
            )
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation?.rotation ?: 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySuppplier: () -> String? = { if (shockTimer.isFinished()) "dissipate" else "pulse" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipate" pairTo Animation(dissipateRegion!!, 1, 3, 0.1f, false),
            "pulse" pairTo Animation(explosionRegion!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySuppplier, animations)
        return AnimationsComponent(this, animator)
    }
}