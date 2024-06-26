package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setEntity
import com.megaman.maverick.game.world.setVelocityAlteration

class ConveyorBelt(game: MegamanMaverickGame) : ISpritesEntity, IDrawableShapesEntity, Block(game) {

    companion object {
        private const val FORCE_AMOUNT = 45f

        private var lLeft: TextureRegion? = null
        private var lRight: TextureRegion? = null
        private var rLeft: TextureRegion? = null
        private var rRight: TextureRegion? = null
        private var middle: TextureRegion? = null
    }

    private var forceFixture: Fixture? = null

    override fun init() {
        super<Block>.init()

        val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)

        if (lLeft == null) lLeft = atlas.findRegion("ConveyorBelt/LeftPart-MoveLeft")
        if (lRight == null) lRight = atlas.findRegion("ConveyorBelt/LeftPart-MoveRight")
        if (rLeft == null) rLeft = atlas.findRegion("ConveyorBelt/RightPart-MoveLeft")
        if (rRight == null) rRight = atlas.findRegion("ConveyorBelt/RightPart-MoveRight")
        if (middle == null) middle = atlas.findRegion("ConveyorBelt/MiddlePart")
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        if (forceFixture == null) {
            forceFixture = Fixture(body, FixtureType.FORCE, GameRectangle())
            forceFixture!!.offsetFromBodyCenter.y = ConstVals.PPM / 8f
            forceFixture!!.setEntity(this)
            body.addFixture(forceFixture!!)
            addDebugShapeSupplier { forceFixture!!.getShape() }
        }

        val bounds = spawnProps.get(ConstKeys.BOUNDS) as Rectangle
        (forceFixture!!.rawShape as GameRectangle).setSize(
            bounds.width - ConstVals.PPM / 4f, bounds.height
        )

        val left = spawnProps.get(ConstKeys.LEFT) as Boolean
        var forceX = FORCE_AMOUNT * ConstVals.PPM
        if (left) forceX = -forceX
        val velocityAlteration =
            VelocityAlteration(forceX = forceX, actionX = VelocityAlterationType.ADD)
        forceFixture!!.setVelocityAlteration { _, _ -> velocityAlteration }

        val sprites = OrderedMap<String, GameSprite>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()
        val numParts = (bounds.width / ConstVals.PPM).toInt()
        for (i in 0 until numParts) {
            val part = if (i == 0) "left" else if (i == numParts - 1) "right" else "middle $i"
            val region =
                when (part) {
                    "left" -> if (left) lLeft else lRight
                    "right" -> if (left) rLeft else rRight
                    else -> middle
                }
            val animation = Animation(region!!, 1, 2, 0.15f, true)
            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
            sprite.setBounds(
                bounds.x + i * ConstVals.PPM, bounds.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat()
            )

            sprites.put(part, sprite)
            animators.add({ sprite } to Animator(animation))
        }

        addComponent(SpritesComponent(this, sprites))
        addComponent(AnimationsComponent(this, animators))
    }
}