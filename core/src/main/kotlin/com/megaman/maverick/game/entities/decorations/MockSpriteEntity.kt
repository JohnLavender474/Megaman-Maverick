package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getPositionPoint

class MockSpriteEntity(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IDrawableShapesEntity,
    IFaceable {

    companion object {
        const val TAG = "MockSpriteEntity"
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private val position = Vector2()
    private val bounds = GameRectangle().also {
        it.setSize(5f * ConstVals.PPM, 4.75f * ConstVals.PPM)
        it.drawingColor = Color.YELLOW
    }
    private val swapFacingTimer = Timer(1f)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(
            TextureAsset.BOSSES_3.source, "${ElecDevil.TAG}/${ConstKeys.GRID}"
        )
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ bounds }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        this.position.set(position)

        bounds.setBottomCenterToPoint(position)

        facing = Facing.LEFT
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        swapFacingTimer.update(delta)
        if (swapFacingTimer.isFinished()) {
            swapFacing()
            swapFacingTimer.reset()
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(8f * ConstVals.PPM, 6f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.translateX(0.15f * facing.value * ConstVals.PPM)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!)))
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
