package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.toGameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class SmokePuff(game: MegamanMaverickGame) : GameEntity(game), ISpritesEntity {

    companion object {
        private var smokePuffRegion: TextureRegion? = null
    }

    private lateinit var animation: IAnimation

    override fun init() {
        if (smokePuffRegion == null)
            smokePuffRegion =
                game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "SmokePuff")
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        (firstSprite as GameSprite).setPosition(spawn, Position.BOTTOM_CENTER)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        animation = Animation(smokePuffRegion!!, 1, 7, 0.025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
        sprite.setSize(ConstVals.PPM.toFloat())
        addComponent(
            DrawableShapesComponent(
                this,
                debugShapeSuppliers = gdxArrayOf({ sprite.boundingRectangle.toGameRectangle() }),
                debug = true
            )
        )
        return SpritesComponent(this, sprite)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(this, {
            if (animation.isFinished()) kill()
        })
}
