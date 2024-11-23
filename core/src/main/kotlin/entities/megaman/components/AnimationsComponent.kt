package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.enums.Facing
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

lateinit var megamanAnimator: Animator

internal fun Megaman.defineAnimationsComponent(): AnimationsComponent {
    val megamanAnimationKeySupplier = {
        val priorKey = getOrDefaultProperty("${ConstKeys.ANIMATION}_${ConstKeys.KEY}", ConstKeys.DEFAULT, String::class)

        val rawKey = getAnimationKey(priorKey)
        if (rawKey != ConstKeys.INVALID) putProperty("${ConstKeys.ANIMATION}_${ConstKeys.KEY}", rawKey)

        var amendedKey = rawKey
        if (maverick && isFacing(Facing.LEFT)) amendedKey += "_Left"
        amendedKey += if (maverick) "_MegamanMaverick" else "_Megaman"
        amendedKey += "_${currentWeapon.name}"

        amendedKey
    }
    val animations = MegamanAnimations(game).get()
    megamanAnimator = Animator(megamanAnimationKeySupplier, animations)

    val jetpackFlameRegion = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "JetpackFlame")
    val jetpackFlameAnimation = Animation(jetpackFlameRegion, 1, 3, 0.1f, true)
    val jetpackFlameAnimator = Animator(jetpackFlameAnimation)

    val animatorSpritePairs = gdxArrayOf<GamePair<() -> GameSprite, IAnimator>>(
        { sprites.get("megaman") } pairTo megamanAnimator,
        { sprites.get("jetpackFlame") } pairTo jetpackFlameAnimator
    )
    return AnimationsComponent(animatorSpritePairs)
}
