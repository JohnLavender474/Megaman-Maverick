package com.megaman.maverick.game.world.contacts

import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.addRuleComponents
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.rootPlayer
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.state.SessionRules
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.contacts.rules.ProjectileRule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class TestPlayer(services: MegaServices) : MegaGameEntity(services), IDamagerEntity, IPlayer {
    override fun getType() = EntityType.SPECIAL
}

private class TestExplosion(services: MegaServices, override var owner: IGameEntity?) : MegaGameEntity(services),
    IOwnable<IGameEntity>, IDamagerEntity {

    init {
        init()
    }

    override fun getType() = EntityType.EXPLOSION
}

private class TestShot(services: MegaServices, override var owner: IGameEntity?) : MegaGameEntity(services),
    IProjectileEntity {

    var hitBodies = 0

    init {
        init()
    }
    var hitShields = 0

    override var size = Size.MEDIUM

    override fun getType() = EntityType.PROJECTILE

    override fun defineBodyComponent(): BodyComponent = error("not used")

    override fun defineSpritesComponent(): SpritesComponent = error("not used")

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        hitBodies++
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        hitShields++
    }
}

class FriendlyFireTest : DescribeSpec({

    lateinit var services: MegaServices
    lateinit var players: MegaPlayers
    lateinit var player0: TestPlayer
    lateinit var player1: TestPlayer
    lateinit var enemy: TestPlayer

    fun fixture(entity: MegaGameEntity, type: FixtureType): Fixture {
        val body = Body(BodyType.DYNAMIC, 0f, 0f, 32f, 32f)
        val fixture = Fixture(body, type, GameRectangle(0f, 0f, 32f, 32f))
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    fun hit(rules: SessionRules, shot: TestShot, target: MegaGameEntity, type: FixtureType) =
        ProjectileRule(rules).onBegin(fixture(shot, FixtureType.PROJECTILE), fixture(target, type), 0f)

    beforeEach {
        players = MegaPlayers(2)
        services = testServices(players = players)
        player0 = TestPlayer(services)
        player1 = TestPlayer(services)
        enemy = TestPlayer(services)
        players.register(PlayerSlot(0), player0)
        players.register(PlayerSlot(1), player1)
    }

    describe("rootPlayer") {
        it("finds the player from the player, a shot and the explosion of a shot") {
            val shot = TestShot(services, player1)
            player1.rootPlayer shouldBe player1
            shot.rootPlayer shouldBe player1
            TestExplosion(services, shot).rootPlayer shouldBe player1
        }

        it("gives null for an enemy's shot") {
            TestShot(services, enemy).rootPlayer shouldBe null
        }
    }

    describe("isFriendlyFire") {
        val off = GameMode.COOP.rules
        val on = GameMode.PVP.rules

        it("is true for a teammate's shot and its explosion when friendly fire is off") {
            val shot = TestShot(services, player1)
            off.isFriendlyFire(shot, player0) shouldBe true
            off.isFriendlyFire(TestExplosion(services, shot), player0) shouldBe true
            off.isFriendlyFire(player1, player0) shouldBe true
        }

        it("is false when friendly fire is on") {
            on.isFriendlyFire(TestShot(services, player1), player0) shouldBe false
        }

        it("is false for the player's own shot, an enemy's shot and a target that is not a player") {
            off.isFriendlyFire(TestShot(services, player0), player0) shouldBe false
            off.isFriendlyFire(TestShot(services, enemy), player0) shouldBe false
            off.isFriendlyFire(TestShot(services, player1), enemy) shouldBe false
        }

        it("is true for a target that a teammate owns, and false for one that the player owns") {
            val gem = TestExplosion(services, player0)
            off.isFriendlyFire(TestShot(services, player1), gem) shouldBe true
            off.isFriendlyFire(TestShot(services, player0), gem) shouldBe false
            on.isFriendlyFire(TestShot(services, player1), gem) shouldBe false
        }
    }

    describe("the damage checks") {
        lateinit var teammate: TestPlayer

        fun megaman(rules: SessionRules): Megaman {
            val slots = MegaPlayers(2)
            val session = testServices(players = slots, rules = rules)
            val megaman = Megaman(session)
            teammate = TestPlayer(session)
            slots.register(PlayerSlot(0), megaman)
            slots.register(PlayerSlot(1), teammate)
            megaman.existenceState = EntityExistenceState.LIVE
            megaman.addRuleComponents()
            return megaman
        }

        fun gem(megaman: Megaman): PreciousGem {
            val gem = PreciousGem(megaman.services)
            gem.owner = megaman
            gem.existenceState = EntityExistenceState.LIVE
            val damageTimer = AbstractHealthEntity::class.java.getDeclaredMethod("getDamageTimer")
            damageTimer.isAccessible = true
            (damageTimer.invoke(gem) as Timer).setToEnd()
            return gem
        }

        it("Megaman refuses a teammate's shot and its explosion with friendly fire off") {
            val megaman = megaman(GameMode.COOP.rules)
            val shot = TestShot(megaman.services, teammate)
            megaman.canBeDamagedBy(shot) shouldBe false
            megaman.canBeDamagedBy(TestExplosion(megaman.services, shot)) shouldBe false
        }

        it("Megaman takes a teammate's shot and its explosion with friendly fire on") {
            val megaman = megaman(GameMode.PVP.rules)
            val shot = TestShot(megaman.services, teammate)
            megaman.canBeDamagedBy(shot) shouldBe true
            megaman.canBeDamagedBy(TestExplosion(megaman.services, shot)) shouldBe true
        }

        it("Megaman refuses its own shot in each mode, and takes an enemy's shot") {
            GameMode.entries.forEach { mode ->
                val megaman = megaman(mode.rules)
                val enemyShot = TestShot(megaman.services, TestPlayer(megaman.services))
                megaman.canBeDamagedBy(TestShot(megaman.services, megaman)) shouldBe false
                megaman.canBeDamagedBy(enemyShot) shouldBe true
            }
        }

        it("a gem that a player owns refuses a teammate's shot with friendly fire off") {
            val megaman = megaman(GameMode.COOP.rules)
            gem(megaman).canBeDamagedBy(TestShot(megaman.services, teammate)) shouldBe false
        }

        it("a gem that a player owns takes a teammate's shot with friendly fire on") {
            val megaman = megaman(GameMode.PVP.rules)
            gem(megaman).canBeDamagedBy(TestShot(megaman.services, teammate)) shouldBe true
        }
    }

    describe("ProjectileRule") {
        it("ignores the body and the shield of a teammate when friendly fire is off") {
            val shot = TestShot(services, player1)
            hit(GameMode.COOP.rules, shot, player0, FixtureType.BODY)
            hit(GameMode.COOP.rules, shot, player0, FixtureType.SHIELD)
            shot.hitBodies shouldBe 0
            shot.hitShields shouldBe 0
        }

        it("ignores a shield that a teammate owns when friendly fire is off") {
            val shot = TestShot(services, player1)
            hit(GameMode.COOP.rules, shot, TestExplosion(services, player0), FixtureType.SHIELD)
            shot.hitShields shouldBe 0
        }

        it("hits the body and the shield of the other player when friendly fire is on") {
            val shot = TestShot(services, player1)
            hit(GameMode.PVP.rules, shot, player0, FixtureType.BODY)
            hit(GameMode.PVP.rules, shot, player0, FixtureType.SHIELD)
            shot.hitBodies shouldBe 1
            shot.hitShields shouldBe 1
        }

        it("hits an enemy when friendly fire is off") {
            val shot = TestShot(services, player1)
            hit(GameMode.COOP.rules, shot, enemy, FixtureType.BODY)
            shot.hitBodies shouldBe 1
        }
    }
})
