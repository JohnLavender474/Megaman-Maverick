package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class SmokePuff(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IDamager, IOwnable<IGameEntity>,
    IBodyEntity, ISpritesEntity, IDirectional {

    companion object {
        const val TAG = "SmokePuff"
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var owner: IGameEntity? = null

    private lateinit var animation: IAnimation

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        try {
            owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        } catch (e: Exception) {
            throw Exception("Owner: ${spawnProps.get(ConstKeys.OWNER)}", e)
        }
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        body.positionOnPoint(spawn, position)

        animation.reset()
    }

    override fun canDamage(damageable: IDamageable) =
        (damageable == megaman && owner?.isAny(AbstractEnemy::class, IHazard::class) == true) ||
                (damageable.isAny(AbstractEnemy::class, IHazard::class) && owner == megaman)

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (animation.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        animation = Animation(region!!, 1, 7, 0.025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }
        return component
    }

    override fun getType() = EntityType.EXPLOSION
}
