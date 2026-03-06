package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class GroundExplosion(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IDamager,
    IOwnable<IGameEntity> {

    companion object {
        const val TAG = "GroundExplosion"
        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f
        private const val SPRITE_WIDTH = 4f
        private const val SPRITE_HEIGHT = 4f
        private const val DURATION = 0.3f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

        private
    val timer = Timer(DURATION)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.positionOnPoint(position, Position.BOTTOM_CENTER)

        timer.reset()

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
    }

    override fun canDamage(damageable: IDamageable) = damageable != owner

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.EXPLOSION, FixtureType.DAMAGER))
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 3, 1, 0.1f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val bottomCenter = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(bottomCenter, Position.BOTTOM_CENTER)
        }
        return component
    }

    override fun getType() = EntityType.EXPLOSION
}
