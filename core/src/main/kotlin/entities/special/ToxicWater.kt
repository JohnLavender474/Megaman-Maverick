package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.SpecialsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class ToxicWater(game: MegamanMaverickGame): MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, ICullableEntity {

    companion object {
        const val TAG = "ToxicWater"
        private const val WATER_ALPHA = 0.8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private var water: Water? = null
    private lateinit var bounds: GameRectangle

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            regions.put("waves", atlas.findRegion("$TAG/waves"))
            regions.put("water", atlas.findRegion("$TAG/water"))
        }
        super.init()
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        defineDrawables(bounds)
        water = EntityFactories.fetch(EntityType.SPECIAL, SpecialsFactory.WATER)!! as Water
        water!!.spawn(props(ConstKeys.BOUNDS pairTo bounds, ConstKeys.HIDDEN pairTo true))
    }

    override fun onDestroy() {
        super.onDestroy()
        water?.destroy()
        water = null
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val rows = (bounds.getHeight() / (0.5f * ConstVals.PPM)).toInt()
        val columns = (bounds.getWidth() / (0.5f * ConstVals.PPM)).toInt()

        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val pos = Vector2(
                    bounds.getX() + (0.5f * x * ConstVals.PPM),
                    bounds.getY() + (0.5f * y * ConstVals.PPM)
                )
                val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
                sprite.setBounds(pos.x, pos.y, 0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM)
                sprite.setAlpha(WATER_ALPHA)
                sprites.put("${x}_${y}", sprite)
                if (y == rows - 1) {
                    val region = regions["waves"]!!
                    val animation = Animation(region, 2, 2, 0.15f, true)
                    val animator = Animator(animation)
                    animators.add({ sprite } pairTo animator)
                } else {
                    val region = regions["water"]!!
                    sprite.setRegion(region)
                }
            }
        }
        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getEntityType() = EntityType.SPECIAL
}
