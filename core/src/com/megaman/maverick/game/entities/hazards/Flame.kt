package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
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
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Flame(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IDamager, IHazard, IDirectionRotatable {

    companion object {
        const val TAG = "Flame"
        private var region: TextureRegion? = null
    }

    override var directionRotation: Direction? = null

    private lateinit var cullTimer: Timer

    private var perpetual = true

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Flame")
        super<MegaGameEntity>.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        when (directionRotation!!) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }
        if (spawnProps.containsKey(ConstKeys.CULL_TIME)) {
            perpetual = false
            cullTimer = Timer(spawnProps.get(ConstKeys.CULL_TIME, Float::class)!!)
        } else perpetual = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (!perpetual) {
            cullTimer.update(delta)
            if (cullTimer.isFinished()) kill()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            val position = when (directionRotation!!) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation?.rotation ?: 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}