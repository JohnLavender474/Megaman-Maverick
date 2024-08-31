package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.isAny
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SmokePuff(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IDamager, IOwnable, IBodyEntity,
    ISpritesEntity, IDirectionRotatable {

    companion object {
        private var smokePuffRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private lateinit var animation: IAnimation

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (smokePuffRegion == null)
            smokePuffRegion =
                game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "SmokePuff")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val position = when (directionRotation!!) {
            Direction.UP -> Position.BOTTOM_CENTER
            Direction.DOWN -> Position.TOP_CENTER
            Direction.LEFT -> Position.CENTER_RIGHT
            Direction.RIGHT -> Position.CENTER_LEFT
        }
        body.positionOnPoint(spawn, position)
        animation.reset()
    }

    override fun canDamage(damageable: IDamageable) =
        (damageable is Megaman && owner is AbstractEnemy) ||
                (damageable.isAny(AbstractEnemy::class, IHazard::class) && owner is Megaman)


    private fun defineUpdatablesComponent() = UpdatablesComponent({ if (animation.isFinished()) kill() })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        animation = Animation(smokePuffRegion!!, 1, 7, 0.025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        sprite.setSize(0.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
            val position = when(directionRotation!!) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
        }
        return spritesComponent
    }
}
