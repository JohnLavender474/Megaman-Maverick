package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.hasFixtureLabel
import com.megaman.maverick.game.world.contacts.rules.ProjectileRule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director

private const val WAIT = 15f

class D1000ShieldTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun fixtures(entity: IGameEntity, type: FixtureType): List<IFixture> {
        val out = Array<IFixture>()
        entity.getComponent(BodyComponent::class)!!.body.getFixtures(out, type)
        return out.toList()
    }

    fun movingBullet(): Bullet {
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo rig.x,
                ConstKeys.POSITION pairTo rig.bossCenter().sub(2f * RIG_PPM, 0f),
                ConstKeys.TRAJECTORY pairTo Vector2(10f * RIG_PPM, 0f)
            )
        )
        return bullet
    }

    fun dead() = rig.boss.getCurrentHealth() <= ConstVals.MIN_HEALTH

    fun stunAndWaitDown() {
        val sabre = rig.sabre()
        rig.ticksUntil(2f) { rig.core().canBeDamagedBy(sabre) } shouldBe true
        rig.core().takeDamageFrom(sabre) shouldBe true
        rig.ticksUntil(3f) { !rig.boss.shielded } shouldBe true
    }

    fun hit(damager: IDamagerEntity, damage: Int) {
        rig.ticksUntil(2f) { rig.boss.canBeDamagedBy(damager) } shouldBe true
        val before = rig.boss.getCurrentHealth()
        rig.boss.takeDamageFrom(damager) shouldBe true
        rig.boss.getCurrentHealth() shouldBe before - damage
    }

    fun hitsToWin(damager: () -> IDamagerEntity, most: Int): Int {
        var hits = 0
        for (hit in 0 until most) {
            val shot = damager()
            rig.ticksUntil(2f) { rig.boss.canBeDamagedBy(shot) } shouldBe true
            rig.boss.takeDamageFrom(shot) shouldBe true
            hits++
            if (dead()) break
        }
        dead() shouldBe true
        return hits
    }

    describe("the shielded body") {
        it("takes 1 from a bullet, 2 from a half charge, 2 from a full charge and 1 from the sabre") {
            rig.startFight()
            rig.boss.shielded shouldBe true
            hit(rig.bullet(), D1000.BULLET_DAMAGE)
            hit(rig.halfCharge(), D1000.SHIELDED_CHARGE_DAMAGE)
            hit(rig.fullCharge(), D1000.SHIELDED_CHARGE_DAMAGE)
            hit(rig.sabre(), D1000.SHIELDED_SABRE_DAMAGE)
        }

        it("blinks after a shot, as any damaged boss") {
            rig.startFight()
            rig.boss.damageBlink shouldBe false
            rig.boss.takeDamageFrom(rig.bullet()) shouldBe true
            rig.ticksUntil(0.3f) { rig.boss.damageBlink } shouldBe true
        }

        it("has no shield box, and a shot passes the body box to the damageable box in place of a bounce") {
            rig.startFight()
            rig.tick()
            fixtures(rig.boss, FixtureType.SHIELD).isEmpty() shouldBe true
            val bullet = movingBullet()
            val shot = fixtures(bullet, FixtureType.PROJECTILE).single()
            val bodyBox = fixtures(rig.boss, FixtureType.BODY).first { it.isActive() }
            bodyBox.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION) shouldBe false
            rig.boss.canBeDamagedBy(bullet) shouldBe true
            ProjectileRule(rig.services.rules).onBegin(shot, bodyBox, RIG_DELTA)
            rig.destroyed.contains(bullet, true) shouldBe false
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH
        }
    }

    describe("the downed body") {
        it("takes the full damage: full charge 4, then bullet 1") {
            rig.startFight()
            stunAndWaitDown()
            rig.tick()
            val before = rig.boss.getCurrentHealth()
            rig.boss.takeDamageFrom(rig.fullCharge()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe before - D1000.FULL_CHARGE_DAMAGE
            rig.ticks(0.55f)
            rig.boss.takeDamageFrom(rig.bullet()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe before - D1000.FULL_CHARGE_DAMAGE - D1000.BULLET_DAMAGE
        }

        it("goes back to the shielded table when the body rises") {
            rig.startFight()
            stunAndWaitDown()
            rig.ticksUntil(WAIT) { rig.boss.fighting } shouldBe true
            rig.tick()
            rig.boss.shielded shouldBe true
            hit(rig.fullCharge(), D1000.SHIELDED_CHARGE_DAMAGE)
        }
    }

    describe("hits to win on Normal") {
        it("sabre the eye, then one full charge on the downed body: 6 hits") {
            rig.startFight()
            val sabre = rig.sabre()
            var hits = 0
            for (cycle in 0 until 6) {
                rig.ticksUntil(WAIT) { rig.core().canBeDamagedBy(sabre) } shouldBe true
                rig.core().takeDamageFrom(sabre) shouldBe true
                hits++
                if (dead()) break
                rig.ticksUntil(WAIT) { !rig.boss.shielded } shouldBe true
                rig.boss.takeDamageFrom(rig.fullCharge()) shouldBe true
                hits++
                rig.ticksUntil(WAIT) { rig.boss.fighting || dead() } shouldBe true
                if (dead()) break
            }
            dead() shouldBe true
            hits shouldBe 6
        }

        it("full charges at the eye only: 8 hits") {
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, d1000Context(distance = D1000Distance.MID, cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false)))
            val shot = rig.fullCharge()
            var hits = 0
            for (hit in 0 until 10) {
                rig.ticksUntil(2f) { rig.core().canBeDamagedBy(shot) } shouldBe true
                rig.core().takeDamageFrom(shot) shouldBe true
                hits++
                if (dead()) break
            }
            dead() shouldBe true
            hits shouldBe 8
        }

        it("full charges at the shielded body only: 15 hits") {
            rig.startFight()
            hitsToWin({ rig.fullCharge() }, 20) shouldBe 15
        }

        it("bullets at the shielded body only: 30 hits") {
            rig.startFight()
            hitsToWin({ rig.bullet() }, 40) shouldBe 30
        }
    }
})
