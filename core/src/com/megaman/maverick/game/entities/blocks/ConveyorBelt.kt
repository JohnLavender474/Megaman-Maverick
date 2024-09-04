package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setEntity
import com.megaman.maverick.game.world.setVelocityAlteration

class ConveyorBelt(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "ConveyorBelt"
        const val DEFAULT_TYPE = "default"
        const val GREEN_TYPE = "green"
        private const val FORCE_AMOUNT = 45f
        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = ObjectMap<String, AnimationDef>()
    }

    private var forceFixture: Fixture? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)

            regions.put("lLeft", atlas.findRegion("$TAG/LeftPart-MoveLeft"))
            regions.put("lRight", atlas.findRegion("$TAG/LeftPart-MoveRight"))
            regions.put("rLeft", atlas.findRegion("$TAG/RightPart-MoveLeft"))
            regions.put("rRight", atlas.findRegion("$TAG/RightPart-MoveRight"))
            regions.put("middle", atlas.findRegion("$TAG/MiddlePart"))

            regions.put("lLeft_green", atlas.findRegion("$TAG/LeftPart-MoveLeft-Green"))
            regions.put("lRight_green", atlas.findRegion("$TAG/LeftPart-MoveRight-Green"))
            regions.put("rLeft_green", atlas.findRegion("$TAG/RightPart-MoveLeft-Green"))
            regions.put("rRight_green", atlas.findRegion("$TAG/RightPart-MoveRight-Green"))
            regions.put("mLeft_green", atlas.findRegion("$TAG/MiddlePart-MoveLeft-Green"))
            regions.put("mRight_green", atlas.findRegion("$TAG/MiddlePart-MoveRight-Green"))
        }
        if (animDefs.isEmpty) {
            animDefs.put(DEFAULT_TYPE, AnimationDef(1, 2, 0.15f))
            animDefs.put(GREEN_TYPE, AnimationDef(2, 2, 0.15f))
        }

        super.init()

        forceFixture = Fixture(body, FixtureType.FORCE, GameRectangle())
        forceFixture!!.offsetFromBodyCenter.y = ConstVals.PPM / 8f
        forceFixture!!.setEntity(this)
        body.addFixture(forceFixture!!)
        addDebugShapeSupplier { forceFixture!!.getShape() }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS) as Rectangle
        (forceFixture!!.rawShape as GameRectangle).setSize(bounds.width - ConstVals.PPM / 4f, bounds.height)

        val left = spawnProps.get(ConstKeys.LEFT) as Boolean

        var forceX = FORCE_AMOUNT * ConstVals.PPM
        if (left) forceX = -forceX
        val velocityAlteration = VelocityAlteration(forceX = forceX, actionX = VelocityAlterationType.ADD)
        forceFixture!!.setVelocityAlteration { _, _ -> velocityAlteration }

        val type = spawnProps.getOrDefault(ConstKeys.TYPE, DEFAULT_TYPE, String::class)

        val sprites = OrderedMap<String, GameSprite>()
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()
        val numParts = (bounds.width / ConstVals.PPM).toInt()
        for (i in 0 until numParts) {
            val part = if (i == 0) "left" else if (i == numParts - 1) "right" else "middle $i"
            var regionKey =
                when (part) {
                    "left" -> if (left) "lLeft" else "lRight"
                    "right" -> if (left) "rLeft" else "rRight"
                    else -> when (type) {
                        DEFAULT_TYPE -> "middle"
                        GREEN_TYPE -> if (left) "mLeft" else "mRight"
                        else -> throw IllegalArgumentException("Illegal type: $type")
                    }
                }

            if (type != DEFAULT_TYPE) regionKey = "${regionKey}_${type}"

            val region = regions[regionKey]
            val animDef = animDefs[type]
            val animation = Animation(region, animDef.rows, animDef.cols, animDef.durations, true)

            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
            sprite.setBounds(bounds.x + i * ConstVals.PPM, bounds.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())

            sprites.put(part, sprite)
            animators.add({ sprite } to Animator(animation))
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }
}