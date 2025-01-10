package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class CacaFlame(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "CacaFlame"
        private const val GRAVITY = -0.15f
        private const val BURST_DUR = 0.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val burstTimer = Timer(BURST_DUR)
    private var burst = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            gdxArrayOf("burst", "fall").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(center)

        burstTimer.reset()
        burst = false
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        burst = true
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        val circle = GameCircle().setRadius(0.5f * ConstVals.PPM)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, circle.copy())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, circle.copy())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.GRAVITY) {
            when {
                burst -> {
                    body.physics.gravity.y = 0f
                    body.physics.velocity.setZero()
                }

                else -> body.physics.gravity.y = GRAVITY * ConstVals.PPM
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite()
                .also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (burst) "burst" else "fall" }
                .addAnimations(
                    "burst" pairTo Animation(regions["burst"], 2, 1, 0.1f, true),
                    "fall" pairTo Animation(regions["fall"], 3, 1, 0.1f, true),
                )
                .build()
        )
        .build()
}
