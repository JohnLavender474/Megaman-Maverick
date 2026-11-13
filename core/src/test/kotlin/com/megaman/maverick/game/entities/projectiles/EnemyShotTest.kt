package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestBodyPlayer
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val VIEW_SIZE = 64f
private const val PLAYER_X = 10f
private const val TEXTURE_SIZE = 1024

private class TestShieldWall(services: MegaServices) : MegaGameEntity(services), IBodyEntity {

    override fun init(vararg params: Any) {
        super.init()
        val body = Body(BodyType.STATIC, width = PPM, height = PPM)
        addComponent(BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.SHIELD)))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
    }

    override fun getType() = EntityType.SPECIAL
}

class EnemyShotTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var audio: MegaAudioManager
    lateinit var player: TestBodyPlayer

    fun step(cycles: Int = 1) = repeat(cycles) { engine.update(STEP) }

    beforeEach {
        val players = MegaPlayers()
        val gravity = WorldGravity(players)
        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { worldContainer },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity, GameMode.SINGLE.rules) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply {
            add(worldSystem)
            add(AnimationsSystem())
        })
        audio = mockk(relaxed = true)
        services = testServices(
            engine = engine,
            audio = audio,
            players = players,
            gravity = gravity,
            gameCamera = CameraView(
                OrthographicCamera().apply {
                    viewportWidth = VIEW_SIZE * PPM
                    viewportHeight = VIEW_SIZE * PPM
                }
            )
        )
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns TEXTURE_SIZE
        every { texture.height } returns TEXTURE_SIZE
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)

        player = TestBodyPlayer(services)
        player.body.setCenter(PLAYER_X * PPM, 0f)
        val damageable = Fixture(player.body, FixtureType.DAMAGEABLE, GameRectangle(player.body))
        damageable.setEntity(player)
        player.body.addFixture(damageable)
        players.register(PlayerSlot(0), player)
        engine.spawn(player, props()) shouldBe true
        step()
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
    }

    fun spawn(shot: EnemyShot, kind: EnemyShot.Kind, x: Float, trajectory: Vector2 = Vector2()): EnemyShot {
        shot.spawn(
            props(
                ConstKeys.TYPE pairTo kind,
                ConstKeys.POSITION pairTo Vector2(x * PPM, 0f),
                ConstKeys.TRAJECTORY pairTo trajectory.scl(PPM)
            )
        ) shouldBe true
        return shot
    }

    fun shot(kind: EnemyShot.Kind, x: Float, trajectory: Vector2 = Vector2()) =
        spawn(MegaEntityFactory.fetch(EnemyShot::class)!!, kind, x, trajectory)

    fun playerBullet(x: Float): Bullet {
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.POSITION pairTo Vector2(x * PPM, 0f),
                ConstKeys.TRAJECTORY pairTo Vector2()
            )
        ) shouldBe true
        return bullet
    }

    fun live(entity: IGameEntity) = entity.existenceState.liveAfterUpdate

    fun active(shot: EnemyShot, type: FixtureType): Boolean {
        val out = Array<IFixture>()
        shot.body.getFixtures(out, type)
        return out.any { it.isActive() }
    }

    fun tagCount(tag: String) = MegaGameEntities.getOfTag(tag).size

    val damages = listOf(
        EnemyShot.Kind.CORE to 2,
        EnemyShot.Kind.JUNKROID to 1,
        EnemyShot.Kind.JUNKROID_HARD to 1,
        EnemyShot.Kind.HOVER_GUNNER to 3,
        EnemyShot.Kind.ROLLING_BOT to 3,
        EnemyShot.Kind.FIRE_PELLET to 3
    )

    val sizes = listOf(
        EnemyShot.Kind.CORE to Vector2(0.5f, 0.5f),
        EnemyShot.Kind.JUNKROID to Vector2(0.3125f, 0.3125f),
        EnemyShot.Kind.JUNKROID_HARD to Vector2(0.3125f, 0.3125f),
        EnemyShot.Kind.HOVER_GUNNER to Vector2(0.3125f, 0.3125f),
        EnemyShot.Kind.ROLLING_BOT to Vector2(0.85f, 0.45f),
        EnemyShot.Kind.FIRE_PELLET to Vector2(0.5f, 0.5f)
    )

    describe("damage to a player") {
        damages.forEach { (kind, damage) ->
            it("$kind takes $damage health") {
                val before = player.getCurrentHealth()
                shot(kind, PLAYER_X)

                step()

                player.getCurrentHealth() shouldBe before - damage
            }
        }
    }

    describe("a hit on a player") {
        damages.forEach { (kind, _) ->
            val dies = kind != EnemyShot.Kind.FIRE_PELLET
            it("$kind ${if (dies) "dies" else "lives on"}") {
                val shot = shot(kind, PLAYER_X)

                step()

                live(shot) shouldBe !dies
            }
        }
    }

    describe("body size") {
        sizes.forEach { (kind, size) ->
            it("$kind is ${size.x} x ${size.y} tiles, with its fixtures") {
                val shot = shot(kind, 0f)

                step()

                shot.body.getWidth() shouldBe (size.x * PPM plusOrMinus 0.001f)
                shot.body.getHeight() shouldBe (size.y * PPM plusOrMinus 0.001f)
                shot.body.forEachFixture {
                    val shape = (it as Fixture).rawShape as GameRectangle
                    shape.getWidth() shouldBe (size.x * PPM plusOrMinus 0.001f)
                    shape.getHeight() shouldBe (size.y * PPM plusOrMinus 0.001f)
                }
            }
        }
    }

    describe("a player's Bullet") {
        listOf(EnemyShot.Kind.CORE, EnemyShot.Kind.JUNKROID, EnemyShot.Kind.HOVER_GUNNER).forEach { kind ->
            it("destroys $kind and is used up") {
                val shot = shot(kind, 0f)
                val bullet = playerBullet(0f)

                step()

                live(shot) shouldBe false
                live(bullet) shouldBe false
            }
        }

        listOf(EnemyShot.Kind.JUNKROID_HARD, EnemyShot.Kind.ROLLING_BOT, EnemyShot.Kind.FIRE_PELLET).forEach { kind ->
            it("passes through $kind") {
                val shot = shot(kind, 0f)
                val bullet = playerBullet(0f)

                step()

                live(shot) shouldBe true
                live(bullet) shouldBe true
            }
        }
    }

    describe("a shield") {
        damages.forEach { (kind, _) ->
            val dies = kind == EnemyShot.Kind.ROLLING_BOT || kind == EnemyShot.Kind.FIRE_PELLET
            it("$kind ${if (dies) "dies on it" else "flies through it"}") {
                TestShieldWall(services).spawn(props(ConstKeys.POSITION pairTo Vector2(0f, 0f))) shouldBe true
                step()
                val shot = shot(kind, 0f)

                step()

                live(shot) shouldBe !dies
            }
        }
    }

    describe("a block") {
        damages.forEach { (kind, _) ->
            it("$kind dies on it") {
                Block(services).spawn(
                    props(ConstKeys.BOUNDS pairTo GameRectangle(-PPM / 2f, -PPM / 2f, PPM, PPM))
                ) shouldBe true
                step()
                val shot = shot(kind, 0f)

                step()

                live(shot) shouldBe false
            }
        }
    }

    describe("death") {
        it("a junk kind leaves a Disintegration and plays no sound") {
            val before = tagCount(Disintegration.TAG)
            shot(EnemyShot.Kind.HOVER_GUNNER, PLAYER_X)

            step(2)

            tagCount(Disintegration.TAG) shouldBe before + 1
            verify(exactly = 0) { audio.playSound(any(), any()) }
        }

        it("the Rolling Bot kind leaves a ChargedShotExplosion with the hit sound") {
            val before = tagCount(ChargedShotExplosion.TAG)
            shot(EnemyShot.Kind.ROLLING_BOT, PLAYER_X)

            step(2)

            tagCount(ChargedShotExplosion.TAG) shouldBe before + 1
            verify(exactly = 1) { audio.playSound(SoundAsset.ENEMY_DAMAGE_SOUND, false) }
        }

        it("the pellet kind leaves a Disintegration with the hit sound on a shield") {
            TestShieldWall(services).spawn(props(ConstKeys.POSITION pairTo Vector2(0f, 0f))) shouldBe true
            step()
            val before = tagCount(Disintegration.TAG)
            shot(EnemyShot.Kind.FIRE_PELLET, 0f)

            step(2)

            tagCount(Disintegration.TAG) shouldBe before + 1
            verify(exactly = 1) { audio.playSound(SoundAsset.ENEMY_DAMAGE_SOUND, false) }
        }
    }

    describe("look") {
        fun sprite(shot: EnemyShot) = shot.getComponent(SpritesComponent::class)!!.let {
            it.preProcess(0f)
            shot.defaultSprite
        }

        it("the pellet kind turns with its path") {
            val shot = shot(EnemyShot.Kind.FIRE_PELLET, 0f, Vector2(1f, 1f))
            step()

            sprite(shot).rotation shouldBe (-45f plusOrMinus 0.01f)
        }

        it("the Rolling Bot kind is flipped when it flies left, on PLAYGROUND 1") {
            val shot = shot(EnemyShot.Kind.ROLLING_BOT, 0f, Vector2(-8f, 0f))
            step()

            val sprite = sprite(shot)
            sprite.isFlipX shouldBe true
            sprite.priority shouldBe DrawingPriority(DrawingSection.PLAYGROUND, 1)
        }

        it("a junk kind is flipped when it flies right, on FOREGROUND 0") {
            val shot = shot(EnemyShot.Kind.CORE, 0f, Vector2(8f, 0f))
            step()

            val sprite = sprite(shot)
            sprite.isFlipX shouldBe true
            sprite.priority shouldBe DrawingPriority(DrawingSection.FOREGROUND, 0)
        }
    }

    describe("a pooled shot spawned again as another kind") {
        it("takes only the new kind's fixtures, size, gravity, draw priority and turn") {
            val shot = shot(EnemyShot.Kind.FIRE_PELLET, 0f, Vector2(1f, 1f))
            step()
            shot.getComponent(SpritesComponent::class)!!.preProcess(0f)
            active(shot, FixtureType.DAMAGEABLE) shouldBe false
            shot.destroy()
            step()

            spawn(shot, EnemyShot.Kind.JUNKROID, 0f, Vector2(1f, 0f))
            step()
            shot.getComponent(SpritesComponent::class)!!.preProcess(0f)

            active(shot, FixtureType.DAMAGEABLE) shouldBe true
            shot.body.getWidth() shouldBe (0.3125f * PPM plusOrMinus 0.001f)
            shot.body.physics.gravity.y shouldBe (EnemyShot.Kind.JUNKROID.gravity * PPM plusOrMinus 0.001f)
            shot.defaultSprite.rotation shouldBe 0f
            shot.defaultSprite.priority shouldBe DrawingPriority(DrawingSection.FOREGROUND, 0)
        }

        it("drops the junk kind's damageable fixture and gravity as the Rolling Bot kind") {
            val shot = shot(EnemyShot.Kind.JUNKROID, 0f, Vector2(1f, 0f))
            step()
            shot.destroy()
            step()

            spawn(shot, EnemyShot.Kind.ROLLING_BOT, 0f, Vector2(-8f, 0f))
            step()

            active(shot, FixtureType.DAMAGEABLE) shouldBe false
            shot.body.getHeight() shouldBe (0.45f * PPM plusOrMinus 0.001f)
            shot.body.physics.gravity.y shouldBe 0f
            shot.defaultSprite.priority shouldBe DrawingPriority(DrawingSection.PLAYGROUND, 1)
        }
    }
})
