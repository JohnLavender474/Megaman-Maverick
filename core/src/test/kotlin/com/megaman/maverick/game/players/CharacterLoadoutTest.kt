package com.megaman.maverick.game.players

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.protoman.Protoman
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponLook
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private val SHARED_SHOT = WeaponDef<MegaPlayer>(id = "shared_shot", cooldown = 0.1f, fire = { _, _, _ -> })

private fun look(atlas: TextureAsset) = WeaponLook<MegaPlayer>(
    atlas = atlas, pauseRegion = "weapons/shared_shot", getWeaponRegion = "test/shared_shot"
)

class CharacterLoadoutTest : DescribeSpec({

    describe("a shared weapon") {
        val megamanLook = look(TextureAsset.MEGAMAN_BUSTER)
        val protomanLook = look(TextureAsset.MEGAMAN_ICE_CUBE)
        val megamanLoadout = CharacterLoadout<Megaman>(
            character = PlayerCharacter.MEGAMAN,
            looksInSelectOrder = orderedMapOf(
                MegamanWeapons.MEGA_BUSTER pairTo MEGAMAN_LOADOUT.lookOf(MegamanWeapons.MEGA_BUSTER),
                SHARED_SHOT pairTo megamanLook
            ),
            rewards = ObjectMap()
        )
        val protomanLoadout = CharacterLoadout<Protoman>(
            character = PlayerCharacter.PROTOMAN,
            looksInSelectOrder = orderedMapOf(SHARED_SHOT pairTo protomanLook),
            rewards = ObjectMap()
        )

        it("fits the loadouts of two characters, with the look of each loadout") {
            megamanLoadout.find(SHARED_SHOT) shouldBeSameInstanceAs SHARED_SHOT
            protomanLoadout.find(SHARED_SHOT) shouldBeSameInstanceAs SHARED_SHOT
            megamanLoadout.lookOf(SHARED_SHOT) shouldBeSameInstanceAs megamanLook
            protomanLoadout.lookOf(SHARED_SHOT) shouldBeSameInstanceAs protomanLook
        }

        it("is not in a loadout that does not list it") {
            protomanLoadout.contains(MegamanWeapons.MEGA_BUSTER) shouldBe false
            shouldThrow<IllegalArgumentException> { protomanLoadout.lookOf(MegamanWeapons.MEGA_BUSTER) }
        }
    }

    describe("a loadout reward") {
        it("must be a weapon of the loadout") {
            val notInLoadout = WeaponDef<MegaPlayer>(id = "not_in_loadout", cooldown = 0.1f, fire = { _, _, _ -> })
            shouldThrow<IllegalArgumentException> {
                CharacterLoadout<Protoman>(
                    character = PlayerCharacter.PROTOMAN,
                    looksInSelectOrder = orderedMapOf(SHARED_SHOT pairTo look(TextureAsset.MEGAMAN_BUSTER)),
                    rewards = objectMapOf(LevelDefinition.DESERT_MAN pairTo gdxArrayOf<WeaponDef<Protoman>>(notInLoadout))
                )
            }
        }
    }

    describe("a weapon definition") {
        it("cannot be changed from outside through the set of blocked moves") {
            val moves = objectSetOf(BehaviorType.JUMPING)
            val weapon = WeaponDef<MegaPlayer>(
                id = "test_weapon", cooldown = 0.1f, fire = { _, _, _ -> }, movesBlockedWhileFiring = moves
            )

            moves.add(BehaviorType.CLIMBING)

            weapon.preventsWhileFiring(BehaviorType.JUMPING) shouldBe true
            weapon.preventsWhileFiring(BehaviorType.CLIMBING) shouldBe false
        }
    }
})
