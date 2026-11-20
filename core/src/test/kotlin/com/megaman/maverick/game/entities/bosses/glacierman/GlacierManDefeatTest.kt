package com.megaman.maverick.game.entities.bosses.glacierman

import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.blocks.BreakableIce
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.levels.LevelDefinition
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val TOLERANCE = 2f * STEP
private const val FREEZE_DUR = 0.75f
private const val SHATTER_START = 1.35f
private const val SEQUENCE_DUR = 1.65f
private const val DEFEAT_DUR = 2.65f

class GlacierManDefeatTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun defeat(level: LevelDefinition? = null, playerX: Float = 6f): Float {
        rig = GlacierManRig()
        if (level != null) rig.services.levelState.currentLevel = level
        rig.startFight(phase2 = true)
        rig.place(playerX)
        rig.tick()
        repeat(15) { rig.boss.takeDamageFrom(rig.bullet()) }
        rig.checkPoints()
        rig.boss.defeated shouldBe true
        return rig.time
    }

    fun since(begin: Float) = rig.time - begin

    fun bossDestroyed() = rig.destroyed.contains(rig.boss, true)

    fun stripIndex(key: String) = (rig.animator().animations[key] as Animation).getIndex()

    describe("the freeze-and-shatter sequence") {
        it("the strips: freeze (5 x 0.15 s), frozen_crack (0.25, 0.25, 0.1 s), frozen_shatter (3 x 0.1 s), " +
            "no loop") {
            defeat()
            val animations = rig.animator().animations
            listOf(Triple("freeze", 5, 0.75f), Triple("frozen_crack", 3, 0.6f), Triple("frozen_shatter", 3, 0.3f))
                .forEach { (key, frames, duration) ->
                    withClue(key) {
                        val strip = animations[key] as Animation
                        strip.size() shouldBe frames
                        strip.getDuration() shouldBe (duration plusOrMinus 0.0001f)
                        strip.isLooping() shouldBe false
                    }
                }
        }

        it("velocity 0 and gravity off for the whole defeat; the keys and frames in time; no damage blink") {
            val begin = defeat()
            var k = 0
            while (since(begin) < SEQUENCE_DUR + 0.5f && !bossDestroyed()) {
                k++
                if (k % 10 == 0) {
                    rig.bossBody.physics.velocity.set(5f * T, 5f * T)
                    rig.bossBody.physics.gravityOn = true
                }
                rig.tick()
                val t = since(begin)
                withClue("t = $t") {
                    rig.bossBody.physics.velocity.x shouldBe 0f
                    rig.bossBody.physics.velocity.y shouldBe 0f
                    rig.bossBody.physics.gravityOn shouldBe false
                    when {
                        t < FREEZE_DUR - TOLERANCE -> {
                            rig.animKey shouldBe "freeze"
                            val local = t
                            if (local % 0.15f in TOLERANCE..0.15f - TOLERANCE)
                                stripIndex("freeze") shouldBe (local / 0.15f).toInt()
                        }

                        t in FREEZE_DUR + TOLERANCE..SHATTER_START - TOLERANCE -> {
                            rig.animKey shouldBe "frozen_crack"
                            val local = t - FREEZE_DUR
                            val expected = when {
                                local < 0.25f - TOLERANCE -> 0
                                local in 0.25f + TOLERANCE..0.5f - TOLERANCE -> 1
                                local > 0.5f + TOLERANCE -> 2
                                else -> null
                            }
                            if (expected != null) stripIndex("frozen_crack") shouldBe expected
                        }

                        t in SHATTER_START + TOLERANCE..SEQUENCE_DUR - TOLERANCE -> {
                            rig.animKey shouldBe "frozen_shatter"
                            val local = t - SHATTER_START
                            if (local % 0.1f in TOLERANCE..0.1f - TOLERANCE)
                                stripIndex("frozen_shatter") shouldBe (local / 0.1f).toInt()
                        }
                    }
                    if (t < SEQUENCE_DUR - TOLERANCE) rig.sprite().hidden shouldBe false
                    if (t > SEQUENCE_DUR + TOLERANCE) rig.sprite().hidden shouldBe true
                }
            }
        }
    }

    describe("the shards") {
        listOf(6f to Facing.LEFT, 24f to Facing.RIGHT).forEach { (playerX, facing) ->
            it("facing ${facing.name.lowercase()}: IceShard.spawn5 once at the first frozen_shatter frame " +
                "(1.35 s), from cell point (32, 22), with the BreakableIce shards") {
                val begin = defeat(playerX = playerX)
                rig.boss.facing shouldBe facing
                var keyAtShards: String? = null
                withClue("the shards") {
                    rig.ticksUntil(3f) {
                        val done = rig.spawnsOf<IceShard>().isNotEmpty()
                        if (done && keyAtShards == null) keyAtShards = rig.animKey
                        done
                    } shouldBe true
                }
                keyAtShards shouldBe "frozen_shatter"
                rig.ticks(DEFEAT_DUR - since(begin) + 0.2f)
                bossDestroyed() shouldBe true
                val shards = rig.spawnsOf<IceShard>()
                shards.size shouldBe 5
                shards.map { it.time }.toSet().size shouldBe 1
                (shards.first().time + STEP - begin) shouldBe (SHATTER_START plusOrMinus TOLERANCE)
                val body = rig.bossBody
                val x = if (facing == Facing.LEFT) 32f else 64f - 32f
                val spriteLeft = body.getX() + body.getWidth() / 2f - 2f * T
                shards.forEach { shard ->
                    shard.position.x shouldBe (spriteLeft + x * 4f * T / 64f plusOrMinus 0.01f)
                    shard.position.y shouldBe (body.getY() + 22f * 3f * T / 48f plusOrMinus 0.01f)
                    shard.props.get(ConstKeys.TAG) shouldBe BreakableIce.TAG
                }
            }
        }
    }

    describe("the orbs and the end") {
        it("no defeat explosion at any time; the sprite is hidden after the shatter (1.65 s)") {
            val begin = defeat()
            rig.ticksUntil(4f) { bossDestroyed() } shouldBe true
            rig.tick()
            rig.spawnsOf<Explosion>().size shouldBe 0
            rig.sounds(SoundAsset.EXPLOSION_2_SOUND) shouldBe 0
            rig.sprite().hidden shouldBe true
            (since(begin) > SEQUENCE_DUR) shouldBe true
        }

        it("the orb ring and DEFEAT_SOUND start with the shards, once, and none at destroy") {
            defeat()
            withClue("the orbs") { rig.ticksUntil(3f) { rig.spawnsOf<ExplosionOrb>().isNotEmpty() } shouldBe true }
            val orbs = rig.spawnsOf<ExplosionOrb>()
            orbs.size shouldBe 8
            orbs.map { it.time }.toSet() shouldBe setOf(rig.spawnsOf<IceShard>().first().time)
            rig.audioSounds.count { it == SoundAsset.DEFEAT_SOUND } shouldBe 1
            rig.ticksUntil(3f) { bossDestroyed() } shouldBe true
            rig.tick()
            rig.spawnsOf<ExplosionOrb>().size shouldBe 8
            rig.audioSounds.count { it == SoundAsset.DEFEAT_SOUND } shouldBe 1
        }

        it("during the defeat the sprite draws above the orbs (FOREGROUND, orb priority + 1); PLAYGROUND 1 on " +
            "the next spawn") {
            defeat()
            rig.tick()
            rig.sprite().priority.section shouldBe DrawingSection.FOREGROUND
            rig.sprite().priority.value shouldBe ExplosionOrb.SPRITE_PRIORITY_VALUE + 1
            rig.ticksUntil(4f) { bossDestroyed() } shouldBe true
            rig.tick()
            rig.boss.spawn(rig.spawnProps()) shouldBe true
            rig.processSpawns()
            rig.boss.getComponent(SpritesComponent::class)!!.preProcess(STEP)
            rig.sprite().priority.section shouldBe DrawingSection.PLAYGROUND
            rig.sprite().priority.value shouldBe 1
        }

        it("the boss is destroyed only after the shatter: 1.65 s + a 1 s hold (2.65 s)") {
            val begin = defeat()
            rig.ticksUntil(4f) { bossDestroyed() } shouldBe true
            since(begin) shouldBe (DEFEAT_DUR plusOrMinus TOLERANCE)
        }
    }

    describe("the level") {
        it("the death is the same in GLACIER_MAN and in the WILY_STAGE_2 refight") {
            fun trace(level: LevelDefinition): List<String> {
                val begin = defeat(level)
                val out = mutableListOf<String>()
                var orbs = 0
                var shards = 0
                var sounds = 0
                while (!bossDestroyed() && since(begin) < 4f) {
                    rig.tick()
                    val t = ((since(begin)) / STEP).toInt()
                    out.add("$t ${rig.animKey} ${rig.sprite().hidden}")
                    if (rig.spawnsOf<ExplosionOrb>().size != orbs) out.add("$t orbs ${rig.spawnsOf<ExplosionOrb>().size}")
                    if (rig.spawnsOf<IceShard>().size != shards) out.add("$t shards ${rig.spawnsOf<IceShard>().size}")
                    val defeatSounds = rig.audioSounds.count { it == SoundAsset.DEFEAT_SOUND }
                    if (defeatSounds != sounds) out.add("$t defeat sound $defeatSounds")
                    orbs = rig.spawnsOf<ExplosionOrb>().size
                    shards = rig.spawnsOf<IceShard>().size
                    sounds = defeatSounds
                }
                rig.tick()
                out.add("end orbs ${rig.spawnsOf<ExplosionOrb>().size} explosions ${rig.spawnsOf<Explosion>().size}")
                rig.dispose()
                return out
            }

            val glacier = trace(LevelDefinition.GLACIER_MAN)
            val wily = trace(LevelDefinition.WILY_STAGE_2)
            rig = GlacierManRig()
            wily shouldBe glacier
            glacier.any { it.contains("orbs 8") } shouldBe true
        }
    }
})
