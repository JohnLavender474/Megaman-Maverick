package com.megaman.maverick.game.entities.megaman.weapons

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.weapons.BusterCharge
import com.megaman.maverick.game.weapons.TrailWhileFiring
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.reflect.KClass
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.players.PlayerCharacter

private val IDS = listOf(
    "mega_buster", "frigid_shot", "infernal_barrage", "moon_scythes", "precious_guard",
    "axe_swinger", "needle_spin", "rodent_claws", "reactor_shot", "rush_jet"
)

private val STAND_KEYS = listOf(
    "stand_megabuster", "stand_frigidshot", "stand_infernalbarrage", "stand_moonscythes", "stand_preciousguard",
    "stand_axeswinger", "stand_needlespin", "stand_rodentclaws", "stand_reactorshot", "stand_rushjet"
)

private val DISPLAY_NAMES = listOf(
    "MEGA BUSTER", "FRIGID SHOT", "INFERNAL BARRAGE", "MOON SCYTHES", "PRECIOUS GUARD",
    "AXE SWINGER", "NEEDLE SPIN", "RODENT CLAWS", "REACTOR SHOT", "RUSH JET"
)

private val NORMAL_COSTS = listOf(0, 1, 3, 3, 6, 1, 2, 2, 2, 0)

private val ATLASES = listOf(
    TextureAsset.MEGAMAN_BUSTER, TextureAsset.MEGAMAN_ICE_CUBE, TextureAsset.MEGAMAN_MAGMA_WAVE,
    TextureAsset.MEGAMAN_MOON_SCYTHE, TextureAsset.MEGAMAN_PRECIOUS_GUARD, TextureAsset.MEGAMAN_AXE_THROW,
    TextureAsset.MEGAMAN_NEEDLE_SPIN, TextureAsset.MEGAMAN_RATTY_CLAWS, TextureAsset.MEGAMAN_REACTOR_SHOT,
    TextureAsset.MEGAMAN_RUSH_JETPACK
)

private val HUD_BIT_REGIONS = listOf(
    null, "ice", "fire", "moon", "precious", "axe", "needle", "rat", "reactor", "rush_jet"
)

private val PAUSE_REGIONS = IDS.map { "weapons/$it" }

private val GET_WEAPON_REGIONS = IDS.map { "megaman/$it" }

private val AIR_DASH_KEYS = listOf(
    "airdash", "airdash", "airdash", "airdash", "airdash", "airdash", "airdash", "airdash", "airdash",
    "jetpack_airdash"
)

private val TRAILS = listOf(
    TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.SHOOT_KEY,
    TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.NONE, TrailWhileFiring.PLAIN_KEY,
    TrailWhileFiring.SHOOT_KEY, TrailWhileFiring.SHOOT_KEY
)

class MegamanWeaponsTest : DescribeSpec({

    lateinit var handler: MegamanWeaponsHandler

    fun newHandler(): MegamanWeaponsHandler {
        val state = GameState()
        MEGAMAN_LOADOUT.weapons.forEach { state.addWeapon(PlayerCharacter.MEGAMAN, it) }
        val megaman = mockk<Megaman>(relaxed = true)
        every { megaman.services } returns testServices(gameState = state)
        every { megaman.character } returns PlayerCharacter.MEGAMAN
        every { megaman.getComponent(any<KClass<IGameComponent>>()) } returns null
        return MegamanWeaponsHandler(megaman).also { it.reset() }
    }

    beforeEach { handler = newHandler() }

    afterEach { unmockkAll() }

    describe("the weapon list") {
        it("has the ids in the select order") {
            MEGAMAN_LOADOUT.weapons.map { it.id } shouldBe IDS
        }

        it("gives the same animation key for each weapon") {
            MEGAMAN_LOADOUT.weapons.map { MegamanAnimations.buildFullKey("stand", it) } shouldBe STAND_KEYS
        }

        it("gives the same display name for each weapon") {
            MEGAMAN_LOADOUT.weapons.map { it.displayName } shouldBe DISPLAY_NAMES
        }

        it("gives the same look values for each weapon") {
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).atlas } shouldBe ATLASES
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).hudBitRegion } shouldBe HUD_BIT_REGIONS
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).pauseRegion } shouldBe PAUSE_REGIONS
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).getWeaponRegion } shouldBe GET_WEAPON_REGIONS
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).airDashKey } shouldBe AIR_DASH_KEYS
            MEGAMAN_LOADOUT.weapons.map { MEGAMAN_LOADOUT.lookOf(it).trailWhileFiring } shouldBe TRAILS
            MEGAMAN_LOADOUT.weapons.filter { MEGAMAN_LOADOUT.lookOf(it).jumpDownChargeKeys } shouldBe
                listOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.REACTOR_SHOT)
            MEGAMAN_LOADOUT.weapons.filter { MEGAMAN_LOADOUT.lookOf(it).showsTornadoWhileFiring } shouldBe
                listOf(MegamanWeapons.NEEDLE_SPIN)
            MEGAMAN_LOADOUT.weapons.filter { MEGAMAN_LOADOUT.lookOf(it).keepsAnimTimeAcrossKeys } shouldBe
                listOf(MegamanWeapons.AXE_SWINGER)
            MEGAMAN_LOADOUT.weapons.filter { MEGAMAN_LOADOUT.lookOf(it).restartsSameKeyOnPress } shouldBe
                listOf(MegamanWeapons.RODENT_CLAWS)
            MEGAMAN_LOADOUT.weapons.filter { MEGAMAN_LOADOUT.lookOf(it).shootKey != null } shouldBe
                listOf(MegamanWeapons.AXE_SWINGER, MegamanWeapons.NEEDLE_SPIN, MegamanWeapons.RODENT_CLAWS)
        }
    }

    describe("the cost of a shot") {
        it("is the normal cost of each weapon at every charge level") {
            MEGAMAN_LOADOUT.weapons.forEachIndexed { i, weapon ->
                val cost = NORMAL_COSTS[i]
                handler.translateAmmo(weapon, cost - MegamanValues.MAX_WEAPON_AMMO)
                MegaChargeStatus.entries.forEach { stat ->
                    handler.canFireWeapon(weapon, stat) shouldBe true
                }
                if (cost > 0) {
                    handler.translateAmmo(weapon, -1)
                    MegaChargeStatus.entries.forEach { stat ->
                        handler.canFireWeapon(weapon, stat) shouldBe false
                    }
                }
            }
        }

        it("lets the buster fire with no ammo, and reads its ammo as full") {
            handler.translateAmmo(MegamanWeapons.MEGA_BUSTER, -MegamanValues.MAX_WEAPON_AMMO)
            handler.getAmmo(MegamanWeapons.MEGA_BUSTER) shouldBe MegamanValues.MAX_WEAPON_AMMO
            MegaChargeStatus.entries.forEach { handler.canFireWeapon(MegamanWeapons.MEGA_BUSTER, it) shouldBe true }
        }

        it("gives a buster charge only to the buster, an axe windup only to the axe, and a windup to no other weapon") {
            val services = testServices()
            services.levelState.currentLevel = LevelDefinition.DESERT_MAN
            val megaman = Megaman(services)
            MEGAMAN_LOADOUT.weapons.filter { megaman.windupOf(it) is BusterCharge } shouldBe
                listOf(MegamanWeapons.MEGA_BUSTER)
            MEGAMAN_LOADOUT.weapons.filter { megaman.windupOf(it) is AxeThrowWindup } shouldBe
                listOf(MegamanWeapons.AXE_SWINGER)
            MEGAMAN_LOADOUT.weapons.filter { megaman.windupOf(it) != null } shouldBe
                listOf(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.AXE_SWINGER)
        }
    }

    describe("the state of a weapon") {
        it("is a separate object for each player") {
            val other = newHandler()
            MEGAMAN_LOADOUT.weapons.forEach { handler.stateOf(it) shouldNotBeSameInstanceAs other.stateOf(it) }
        }

        it("keeps the claws combo of each player apart") {
            val other = newHandler()
            val mine = handler.stateOf(MegamanWeapons.RODENT_CLAWS) as RodentClawsState
            val theirs = other.stateOf(MegamanWeapons.RODENT_CLAWS) as RodentClawsState
            mine.nextSlash(comboBroken = false)
            mine.nextSlash(comboBroken = false)
            mine.slashIndex shouldBe 2
            theirs.slashIndex shouldBe 0
            theirs.keySlashIndex shouldBe 1
        }

        it("keeps the moon scythe wait of each player apart") {
            val other = newHandler()
            (handler.stateOf(MegamanWeapons.MOON_SCYTHES) as MoonScythesState).maxWaitTime.reset()
            (other.stateOf(MegamanWeapons.MOON_SCYTHES) as MoonScythesState).maxWaitTime.isFinished() shouldBe true
        }
    }

    describe("the moves that a weapon blocks while it fires") {
        val blocked = listOf(
            BehaviorType.AIR_DASHING, BehaviorType.CLIMBING, BehaviorType.CROUCHING, BehaviorType.GROUND_SLIDING,
            BehaviorType.JUMPING, BehaviorType.SWIMMING, BehaviorType.WALL_SLIDING
        )

        it("are the 7 moves for Needle Spin, and none for the other weapons") {
            MEGAMAN_LOADOUT.weapons.forEach { weapon ->
                blocked.forEach { move ->
                    weapon.preventsWhileFiring(move) shouldBe (weapon == MegamanWeapons.NEEDLE_SPIN)
                }
            }
        }

        it("block only while Megaman shoots") {
            val megaman = mockk<Megaman>(relaxed = true)
            every { megaman.currentWeapon } returns MegamanWeapons.NEEDLE_SPIN
            every { megaman.shooting } returns false
            megaman.currentWeaponPrevents(BehaviorType.JUMPING) shouldBe false
            every { megaman.shooting } returns true
            megaman.currentWeaponPrevents(BehaviorType.JUMPING) shouldBe true
            every { megaman.currentWeapon } returns MegamanWeapons.MEGA_BUSTER
            megaman.currentWeaponPrevents(BehaviorType.JUMPING) shouldBe false
        }
    }
})
