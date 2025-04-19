package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

private class ToxicWaterWaveAnimator(private val game: MegamanMaverickGame) : IAnimator, Initializable, Updatable {

    companion object {
        const val TAG = "ToxicWaterWaveAnimator"
    }

    private lateinit var animation: IAnimation
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
        val region = atlas.findRegion("${ToxicWater.TAG}/waves")
        animation = Animation(region, 2, 2, 0.125f, true)
    }

    override fun update(delta: Float) = animation.update(delta)

    override fun shouldAnimate(delta: Float) = true

    override fun animate(sprite: GameSprite, delta: Float) {
        val region = animation.getCurrentRegion()
        if (region != null) sprite.setRegion(region)
    }

    override fun reset() = animation.reset()
}

class ToxicWater(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, ICullableEntity {

    companion object {
        const val TAG = "ToxicWater"
        private const val WATER_ALPHA = 0.8f
        private var waterRegion: TextureRegion? = null
        private var waveAnimator: ToxicWaterWaveAnimator? = null
    }

    private var water: Water? = null
    private val bounds = GameRectangle()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (waterRegion == null)
            waterRegion = game.assMan.getTextureRegion(TextureAsset.SPECIALS_1.source, "$TAG/water")
        if (waveAnimator == null) {
            waveAnimator = ToxicWaterWaveAnimator(game)
            waveAnimator!!.init()
        }
        super.init()
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        defineDrawables(bounds)

        val water = MegaEntityFactory.fetch(Water::class)!!
        water.spawn(
            props(
                ConstKeys.HIDDEN pairTo true,
                ConstKeys.BOUNDS pairTo bounds,
                "${ConstKeys.SPLASH}_${ConstKeys.TYPE}" pairTo SplashType.TOXIC
            )
        )
        this.water = water

        if (!game.updatables.containsKey(ToxicWaterWaveAnimator.TAG))
            game.updatables.put(ToxicWaterWaveAnimator.TAG, waveAnimator!!::update)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        water?.destroy()
        water = null

        if (MegaGameEntities.getOfTag(TAG).isEmpty) game.updatables.remove(ToxicWaterWaveAnimator.TAG)
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val rows = (bounds.getHeight() / (0.5f * ConstVals.PPM)).toInt()
        val columns = (bounds.getWidth() / (0.5f * ConstVals.PPM)).toInt()

        for (x in 0 until columns) for (y in 0 until rows) {
            val posX = bounds.getX() + (0.5f * x * ConstVals.PPM)
            val posY = bounds.getY() + (0.5f * y * ConstVals.PPM)

            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
            sprite.setBounds(posX, posY, 0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM)
            sprite.setAlpha(WATER_ALPHA)
            sprites.put("${x}_${y}", sprite)

            when (y) {
                rows - 1 -> animators.add({ sprite } pairTo waveAnimator!!)
                else -> sprite.setRegion(waterRegion!!)
            }
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
