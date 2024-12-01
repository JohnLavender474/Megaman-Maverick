package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.megaman.sprites.getAnimationKey

var Megaman.currentAnimKey: String?
    get() = getProperty(ConstKeys.ANIMATION_KEY, String::class)
    private set(value) {
        putProperty(ConstKeys.ANIMATION_KEY, value)
    }

internal fun Megaman.defineAnimationsComponent(): AnimationsComponent {
    val megamanAnimKeySupplier = {
        val key = getAnimationKey(currentAnimKey)
        if (key != null) {
            currentAnimKey = key
            "${key}_${currentWeapon.name}"
        } else null
    }
    val animations = MegamanAnimations(game).get()
    val megamanAnimator = Animator(megamanAnimKeySupplier, animations)

    val jetpackFlameRegion = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "JetpackFlame")
    val jetpackFlameAnimation = Animation(jetpackFlameRegion, 1, 3, 0.1f, true)
    val jetpackFlameAnimator = Animator(jetpackFlameAnimation)

    val animatorSpritePairs = gdxArrayOf<GamePair<() -> GameSprite, IAnimator>>(
        { sprites.get("megaman") } pairTo megamanAnimator,
        { sprites.get("jetpackFlame") } pairTo jetpackFlameAnimator
    )
    return AnimationsComponent(animatorSpritePairs)
}
