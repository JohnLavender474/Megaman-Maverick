package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getPosition
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import kotlin.math.floor

class PreciousSpike(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, IAnimatedEntity,
    ISpritesEntity, IHazard, IDirectional {

    companion object {
        const val TAG = "PreciousSpike"
        private var region: TextureRegion? = null
    }

    override lateinit var direction: Direction

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val rows = floor(bounds.getHeight() / ConstVals.PPM).toInt()
        val cols = floor(bounds.getWidth() / ConstVals.PPM).toInt()
        defineDrawables(rows, cols)

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase()
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        sprites.clear()
        animators.clear()
    }

    private fun defineDrawables(rows: Int, cols: Int) {
        for (row in 0 until rows) for (col in 0 until cols) {
            val key = "${row}_${col}"

            val sprite = GameSprite()
            sprite.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)

            sprites.put(key, sprite)

            putSpriteUpdateFunction(key) { _, _ ->
                val position = body.getBounds()
                    .translate(
                        col * ConstVals.PPM.toFloat(),
                        row * ConstVals.PPM.toFloat()
                    )
                    .getPosition()
                sprite.setPosition(position)

                val offsetX = when (direction) {
                    Direction.LEFT -> -0.5f
                    Direction.RIGHT -> 0.5f
                    else -> 0f
                } * ConstVals.PPM
                sprite.translateX(offsetX)

                val offsetY = when (direction) {
                    Direction.LEFT, Direction.RIGHT -> -0.5f
                    Direction.DOWN -> -1f
                    else -> 0f
                } * ConstVals.PPM
                sprite.translateY(offsetY)

                sprite.setOriginCenter()
                sprite.rotation = direction.rotation
            }

            val animation = Animation(region!!, 2, 2, 0.1f, true)
            val animator = Animator(animation)
            putAnimator(key, sprite, animator)
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture ->
                val shape = (fixture as Fixture).rawShape
                (shape as GameRectangle).set(body)
            }
        }
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DEATH))
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
