package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.*
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.pairTo
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.megaman.sprites.getAnimationKey

var Megaman.currentAnimKey: String?
    get() = getProperty(ConstKeys.ANIMATION_KEY, String::class)
    private set(value) {
        putProperty(ConstKeys.ANIMATION_KEY, value)
    }

internal fun Megaman.defineAnimationsComponent(animations: OrderedMap<String, IAnimation>): AnimationsComponent {
    val megamanAnimKeySupplier: (String?) -> String? = supplier@{
        val key = getAnimationKey(currentAnimKey)
        return@supplier when {
            key != null -> {
                currentAnimKey = key
                MegamanAnimations.buildFullKey(key, currentWeapon)
            }
            else -> null
        }
    }
    val megamanAnimator = Animator(
        keySupplier = megamanAnimKeySupplier,
        animations = animations,
        onChangeKey = { _, currentKey, nextKey -> onChangeAnimationKey(currentKey, nextKey, animations) },
        postProcessKey = { _, currentKey, nextKey -> postProcessAnimationKey(currentKey, nextKey) },
        shouldEqualKeysTriggerChange = this::shouldEqualAnimKeysTriggerChange
    )

    val decorationsAtlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)

    val jetpackFlameRegion = decorationsAtlas.findRegion(JETPACK_FLAME_SPRITE_KEY)
    val jetpackFlameAnimation = Animation(jetpackFlameRegion, 1, 3, 0.1f)
    val jetpackFlameAnimator = Animator(jetpackFlameAnimation)

    val desertTornadoRegion = decorationsAtlas.findRegion(DESERT_TORNADO_SPRITE_KEY)
    val desertTornadoAnimation = Animation(desertTornadoRegion, 2, 1, 0.1f)
    val desertTornadoAnimator = Animator(desertTornadoAnimation)

    val animators = orderedMapOf<Any, IAnimator>(
        MEGAMAN_SPRITE_KEY pairTo megamanAnimator,
        JETPACK_FLAME_SPRITE_KEY pairTo jetpackFlameAnimator,
        DESERT_TORNADO_SPRITE_KEY pairTo desertTornadoAnimator
    )

    return AnimationsComponent(animators, sprites)
}

internal fun Megaman.onChangeAnimationKey(
    currentKey: String?, nextKey: String?, animations: OrderedMap<String, IAnimation>
) {
    // Special case for Axe Throw weapon: need to ensure that if Megaman is "shooting" (throwing),
    // then if the animation key changes, the new animation starts at the same time stamp as what
    // the current animation is at.
    if (currentWeapon == MegamanWeapon.AXE_SWINGER &&
        currentKey != null && nextKey != null &&
        currentKey.contains("axe_throw")
    ) {
        val time = animations[currentKey]?.getCurrentTime()
        time?.let { t -> animations[nextKey]?.setCurrentTime(t) }
    }
}

internal fun Megaman.shouldEqualAnimKeysTriggerChange(key: String?): Boolean {
    // When the player is equipped with the Rodent Claws, then the animation should be
    // reset each time the player presses the SHOOT button EXCEPT when the player is
    // standing. This is because every slash sequence besides the STAND animation uses
    // the same animation repeatedly. Without this check, then Mega Man will appear to
    // be frozen still if the player rapid fires the SHOOT button.
    return key != null && currentWeapon == MegamanWeapon.RODENT_CLAWS &&
        weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.NOT_CHARGED) &&
        game.controllerPoller.isPressed(MegaControllerButton.B) &&
        key.contains("slash") && !key.contains("stand")
}

internal fun Megaman.postProcessAnimationKey(currentKey: String?, nextKey: String?): String? {
    if (currentKey?.contains("_shoot") == true && nextKey?.contains("needle_spin") == true) {
        shootAnimTimer.setToEnd()
        return currentKey.replace("_shoot", "")
    }

    return nextKey
}
