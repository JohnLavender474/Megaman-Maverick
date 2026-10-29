package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sprites.*
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class MegamanTrailSpriteV2(game: MegaServices) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "MegamanTrailingSprite_v2"
        private const val FADE_DUR = 0.25f
    }

    private val fadeTimer = Timer(FADE_DUR)
    private lateinit var animKey: String
    private var flipX = false
    private var flipY = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val owner = spawnProps.get(ConstKeys.OWNER, Megaman::class)!!
        val rawAnimKey = when {
            spawnProps.containsKey(ConstKeys.KEY) -> spawnProps.get(ConstKeys.KEY, String::class)
            else -> owner.currentAnimKey
        }
        if (rawAnimKey == null) {
            GameLogger.error(TAG, "onSpawn(): destroying trail sprite because raw anim key is null")
            destroy()
            return
        }
        this.animKey = "${owner.currentWeapon.name.lowercase()}/$rawAnimKey"

        super.onSpawn(spawnProps)

        val position = DirectionPositionMapper.getInvertedPosition(owner.getSpriteDirection())
        val spawn = owner.body.getPositionPoint(position)

        defaultSprite.let { sprite ->
            sprite.setPosition(spawn, position)

            sprite.translateX(owner.getSpriteXTranslation() * ConstVals.PPM)
            sprite.translateY(owner.getSpriteYTranslation() * ConstVals.PPM)

            sprite.setOriginCenter()
            sprite.rotation = owner.getSpriteRotation()
        }
        flipX = owner.shouldFlipSpriteX()
        flipY = owner.shouldFlipSpriteY()

        fadeTimer.reset()

        GameLogger.debug(TAG, "onSpawn(): animKey=$animKey, rawAnimKey=$rawAnimKey, spawn=$spawn, position=$position")
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (game.camera.rotating) {
            destroy()
            return@UpdatablesComponent
        }

        fadeTimer.update(delta)
        if (fadeTimer.isFinished()) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val alpha = 1f - fadeTimer.getRatio()
            sprite.setAlpha(alpha)
            sprite.setFlip(flipX, flipY)
            sprite.hidden = game.camera.rotating
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { animKey }
        val animations = ObjectMap<String, IAnimation>()

        val atlas = game.assMan.getTextureAtlas(TextureAsset.MEGAMAN_TRAIL_SPRITE_V2.source)

        MegamanWeapon.entries.forEach { weapon ->
            MegamanAnimationDefs.getKeys().forEach { key ->
                val fullKey = "${weapon.name.lowercase()}/$key"
                if (!atlas.containsRegion(fullKey)) {
                    GameLogger.debug(TAG, "defineAnimationsComponent(): no region: fullKey=$fullKey")
                    return@forEach
                }

                val (rows, columns, durations, loop) = MegamanAnimationDefs.get(key)
                animations.put(fullKey, Animation(atlas.findRegion(fullKey), rows, columns, durations, loop))

                GameLogger.debug(TAG, "defineAnimationsComponent(): put animation: fullKey=$fullKey")
            }
        }

        GameLogger.debug(TAG, "defineAnimationsComponent(): animations.size=${animations.size}")

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
